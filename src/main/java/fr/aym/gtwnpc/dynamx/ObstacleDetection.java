package fr.aym.gtwnpc.dynamx;

import com.google.common.base.Predicates;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import fr.aym.gtwnpc.utils.AIRaycast;
import fr.aym.gtwnpc.utils.OOBB;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ObstacleDetection {
    private final BaseVehicleEntity<?> entity;
    private final AutopilotModule autopilotModule;
    private OOBB myOOBB;

    private final List<CollisionSimplex> collisionSimplexs = new ArrayList<>();
    private final List<CollisionCluster> collisionClusters = new ArrayList<>();

    public ObstacleDetection(BaseVehicleEntity<?> entity, AutopilotModule autopilotModule) {
        this.entity = entity;
        this.autopilotModule = autopilotModule;
    }

    public OOBB getEntityOOBB(BaseVehicleEntity<?> entity) {
        Matrix3f rot = entity.physicsRotation.toRotationMatrix();
        org.joml.Matrix3f jomlRot = new org.joml.Matrix3f(rot.get(0, 0), rot.get(0, 1), rot.get(0, 2), rot.get(1, 0), rot.get(1, 1), rot.get(1, 2), rot.get(2, 0), rot.get(2, 1), rot.get(2, 2));
        List<MutableBoundingBox> unrotatedBoxes = entity.getCollisionBoxes();
        AxisAlignedBB entityBoxCache;
        if (unrotatedBoxes.isEmpty()) { //If there is no boxes, create a default one
            com.jme3.math.Vector3f min = Vector3fPool.get(entity.getPositionVector()).subtractLocal(2, 1, 2);
            com.jme3.math.Vector3f max = Vector3fPool.get(entity.getPositionVector()).addLocal(2, 2, 2);
            entityBoxCache = new AxisAlignedBB(min.x, min.y, min.z, max.x, max.y, max.z);
        } else {
            MutableBoundingBox container;
            if (unrotatedBoxes.size() == 1) { //If there is one, no more calculus to do !
                container = unrotatedBoxes.get(0);
            } else {
                container = new MutableBoundingBox(unrotatedBoxes.get(0));
                for (int i = 1; i < unrotatedBoxes.size(); i++) { //Else create a bigger box containing all the boxes
                    container.growTo(unrotatedBoxes.get(i));
                }
            }
            entityBoxCache = container.toBB();
        }
        return new OOBB(new Vector3f(entity.physicsPosition.x, entity.physicsPosition.y, entity.physicsPosition.z),
                new Vector3f((float) ((entityBoxCache.maxX - entityBoxCache.minX) / 2), (float) ((entityBoxCache.maxY - entityBoxCache.minY) / 2), (float) ((entityBoxCache.maxZ - entityBoxCache.minZ) / 2)),
                jomlRot.transpose());
    }

    public void updateOOBB() {
        myOOBB = getEntityOOBB(entity);
        collisionSimplexs.removeIf(c -> !c.isCollidingB());
        collisionSimplexs.forEach(c -> {
            c.setCollidingB(false);
        });
        collisionClusters.removeIf(c -> !c.isCollidingA() || !c.isCollidingB());
        collisionClusters.forEach(c -> {
            if (c.getObstacleDetectionA() == this)
                c.setCollidingA(false);
            if (c.getObstacleDetectionB() == this)
                c.setCollidingB(false);
        });
    }

    public AxisAlignedBB getDetectionAABB(int rayDistance) {
        Vec3d min = entity.getPositionVector().add(entity.getLookVec().scale(4));
        Vec3d max = entity.getPositionVector().add(entity.getLookVec().scale(rayDistance));
        AxisAlignedBB front = new AxisAlignedBB(min.x, min.y, min.z, max.x, max.y, max.z).grow(5);
        com.jme3.math.Vector3f right = entity.physicsRotation.mult(com.jme3.math.Vector3f.UNIT_X, Vector3fPool.get()).multLocal(-3.5f);
        front = front.expand(right.x, right.y, right.z);
        return front;
    }

    public void detectObstacles() {
        // System.out.println("======== Updating " + entity + " =========");
        updateOOBB();
        float mySpeed = entity.getPhysicsHandler() != null && entity.ticksExisted > 10 ? entity.getPhysicsHandler().getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : 0;
        int checkFar2 = mySpeed > 35 ? 16 : (int) (mySpeed > 12 ? 8 :  Math.ceil(mySpeed/2+0.01));
        checkFar2 = Math.max(2, checkFar2);
        AxisAlignedBB front = getDetectionAABB(checkFar2);
        List<Entity> entities = entity.world.getEntitiesInAABBexcluding(entity, front, Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && entity.getRidingEntity() == null));
        List<BaseVehicleEntity<?>> vehicles = entities.stream().filter(e -> e instanceof BaseVehicleEntity).map(e -> (BaseVehicleEntity<?>) e).collect(Collectors.toList());
        entities.removeIf(e -> e instanceof BaseVehicleEntity);
        ObstacleAction retainedAction = ObstacleAction.IGNORE;
        checkFar2 *= 2;
        List<AIRaycast> rayVectors = createRayVectors(checkFar2);
        if (!entities.isEmpty()) {
            ObstacleAction action = avoidCollisionWithEntities(entities, rayVectors, checkFar2, 2);
            if (retainedAction.ordinal() < action.ordinal()) {
                retainedAction = action;
            }
            //System.out.println("Retained action after entities: " + retainedAction);
        }
        if (!vehicles.isEmpty()) {
            ObstacleAction action = avoidCollisionWithVehicles(vehicles, rayVectors, checkFar2, 1);
            if (retainedAction.ordinal() < action.ordinal()) {
                retainedAction = action;
            }
            //System.out.println("Retained action after vehicles: " + retainedAction);
        }
        lastVectors = rayVectors;
        switch (retainedAction) {
            case IGNORE:
                break;
            case SLOW_DOWN:
                autopilotModule.setSpeedLimit(10);
                //  System.out.println("SLOW DOWN " + mySpeed + " " + autopilotModule.getSpeedLimit() + " " + retainedAction);
                if (mySpeed > 15) {
                    int controls = autopilotModule.getControls();
                    controls |= 4; // braking
                    controls &= ~2; // no acceleration
                    autopilotModule.setControls(controls);
                }
                break;
            case STEER_LEFT:
                float steering = autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : autopilotModule.getSteerForce();
                //  System.out.println("STEER LEFT " + mySpeed + " " + retainedAction);
                if (autopilotModule.getSteerForce() <= 0 || (autopilotModule.getSteerForce() - steering) < 0.4f) {
                    autopilotModule.setForcedSteering(Math.max(-1, steering - 0.1f));
                    autopilotModule.setForcedSteeringTime(20);
                }
                int controls = autopilotModule.getControls();
                if (mySpeed > 2f)
                    controls |= 4; // braking
                //else
                  //  controls |= 32; // handbrake
                controls &= ~2; // no acceleration
                autopilotModule.setControls(controls);
                break;
            case STEER_RIGHT:
                steering = autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : autopilotModule.getSteerForce();
                //  System.out.println("STEER RIGHT " + mySpeed + " " + retainedAction);
                if (autopilotModule.getSteerForce() >= 0 || (autopilotModule.getSteerForce() - steering) > -0.4f) {
                    autopilotModule.setForcedSteering(Math.min(1, steering + 0.1f));
                    autopilotModule.setForcedSteeringTime(20);
                }
                controls = autopilotModule.getControls();
                if (mySpeed > 2f)
                    controls |= 4; // braking
               // else
                 //   controls |= 32; // handbrake
                controls &= ~2; // no acceleration
                autopilotModule.setControls(controls);
                break;
            case STOP:
                //  System.out.println("STOP " + mySpeed + " " + retainedAction);
                controls = autopilotModule.getControls();
                if (mySpeed > 0.5f)
                    controls |= 4; // braking
                else
                    controls |= 32; // handbrake
                controls &= ~2; // no acceleration
                autopilotModule.setControls(controls);
                break;
        }
    }

    public List<AIRaycast> lastVectors;

    public List<AIRaycast> createRayVectors(float rayDistance) {
        List<AIRaycast> vectors = new ArrayList<>();
        float increment = 0.4f;
        increment = 0.3f;
        //System.out.println("Fils de merde");
        PartWheel wheel1 = entity.getPackInfo().getPartByTypeAndId(PartWheel.class, (byte) 0);
        PartWheel wheel2 = entity.getPackInfo().getPartsByType(PartWheel.class).stream().filter(w -> w != wheel1 && w.getPosition().x != wheel1.getPosition().x).findFirst().orElse(null);
        float width = Math.abs(wheel1.getPosition().x - wheel2.getPosition().x);
        float angle = (float) 0;
        float steering = autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : autopilotModule.getSteerForce();
        angle += steering * 0.6f;
        float xStart = -width / 2 - increment * 1.5f;
        float xEnd = width / 2 + increment * 1.5f;
        //System.out.println("lol");

        com.jme3.math.Vector3f rayVecBase = com.jme3.math.Vector3f.UNIT_Z;
        com.jme3.math.Vector3f rayVec = Vector3fPool.get(rayVecBase).multLocal(rayDistance);
        Quaternion q = QuaternionPool.get();
        q.fromAngles(0, angle, 0);
        rayVec = DynamXGeometry.rotateVectorByQuaternion(rayVec, q);
        rayVec = DynamXGeometry.rotateVectorByQuaternion(rayVec, entity.physicsRotation);
        float zoneSize = (xEnd - xStart) / 3;
        for (float x = xStart; x <= xEnd; x += increment) {
            com.jme3.math.Vector3f origin = Vector3fPool.get(x, wheel1.getPosition().y, wheel1.getPosition().z);
            origin = DynamXGeometry.rotateVectorByQuaternion(origin, entity.physicsRotation);
            origin.addLocal(entity.physicsPosition);
            com.jme3.math.Vector3f rayVec2 = Vector3fPool.get(rayVec);
            rayVec2.addLocal(origin);
            vectors.add(new AIRaycast(entity, new com.jme3.math.Vector3f(origin), new com.jme3.math.Vector3f(rayVec2), x < xStart + zoneSize ? AIRaycast.RayCastType.FRONT_LEFT : x > xEnd - zoneSize ? AIRaycast.RayCastType.FRONT_RIGHT : AIRaycast.RayCastType.FRONT_CENTER));
        }
        float angleIncrement = 0.1f;
        float angleStart = angle - steering * 1.2f;
        float angleEnd = (float) (angleStart + Math.PI / 3);
        com.jme3.math.Vector3f origin = Vector3fPool.get(xStart, wheel1.getPosition().y, wheel1.getPosition().z);
        origin = DynamXGeometry.rotateVectorByQuaternion(origin, entity.physicsRotation);
        origin.addLocal(entity.physicsPosition);
        for (float a = angleStart; a <= angleEnd; a += angleIncrement) {
            com.jme3.math.Vector3f rayVec2 = Vector3fPool.get(rayVecBase).multLocal(rayDistance / 1.2f);
            Quaternion q2 = QuaternionPool.get();
            q2.fromAngles(0, -a, 0);
            rayVec2 = DynamXGeometry.rotateVectorByQuaternion(rayVec2, q2);
            rayVec2 = DynamXGeometry.rotateVectorByQuaternion(rayVec2, entity.physicsRotation);
            rayVec2.addLocal(origin);
            vectors.add(new AIRaycast(entity, new com.jme3.math.Vector3f(origin), new com.jme3.math.Vector3f(rayVec2), AIRaycast.RayCastType.RIGHT));
        }
        if (steering > 0.05f) {
            angleStart = angleStart - (float) Math.PI / 8;
            angleEnd = (float) (angleStart + Math.PI / 8);
            origin = Vector3fPool.get(xEnd, wheel1.getPosition().y, wheel1.getPosition().z);
            origin = DynamXGeometry.rotateVectorByQuaternion(origin, entity.physicsRotation);
            origin.addLocal(entity.physicsPosition);
            for (float a = angleStart; a <= angleEnd; a += angleIncrement) {
                com.jme3.math.Vector3f rayVec2 = Vector3fPool.get(rayVecBase).multLocal(Math.min(rayDistance / 1.2f, 5));
                Quaternion q2 = QuaternionPool.get();
                q2.fromAngles(0, -a, 0);
                rayVec2 = DynamXGeometry.rotateVectorByQuaternion(rayVec2, q2);
                rayVec2 = DynamXGeometry.rotateVectorByQuaternion(rayVec2, entity.physicsRotation);
                rayVec2.addLocal(origin);
                vectors.add(new AIRaycast(entity, new com.jme3.math.Vector3f(origin), new com.jme3.math.Vector3f(rayVec2), AIRaycast.RayCastType.RIGHT));
            }
        }
        return vectors;
    }

    public ObstacleAction avoidCollisionWithVehicles(List<BaseVehicleEntity<?>> vehicles, List<AIRaycast> rayVectors, float rayDistance, int frontSize) {
        ObstacleAction retainedAction = ObstacleAction.IGNORE;
        for (int i = 0; i < rayVectors.size(); i++) {
            AIRaycast.HitInfo info = rayVectors.get(i).rayTraceLineOnVehicles(this, vehicles, rayDistance, frontSize);
            ObstacleAction action = info.getAction();
            if (retainedAction.ordinal() < action.ordinal()) {
                retainedAction = action;
                //System.out.println("Collision with " + i);
                //if (retainedAction == ObstacleAction.STOP)
                //  return retainedAction;
            }
            if (info.getHitEntity() != null && action.ordinal() >= ObstacleAction.STOP.ordinal()) {
                if (collisionSimplexs.stream().noneMatch(c -> c.getObstacleDetectionB() == this))
                    collisionSimplexs.add(new CollisionSimplex(this));
                else {
                    CollisionSimplex simplex = collisionSimplexs.stream().filter(c -> c.getObstacleDetectionB() == this).findFirst().get();
                    simplex.incrementCollision(this);
                    if (simplex.getCollisionTime() > 20 * 45) {
                        System.out.println("Despawning " + this);
                        if(entity.world.isRemote) {
                            Entity entity1 = ((SPPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).getOtherSideEntity();
                            if(entity1 != null)
                                entity1.setDead();
                            return ObstacleAction.STOP;
                        }
                        entity.setDead();
                        return ObstacleAction.STOP;
                    }
                }
            }
        }
        return retainedAction;
    }

    public ObstacleAction avoidCollisionWithEntities(List<Entity> entities, List<AIRaycast> rayVectors, float rayDistance, int frontSize) {
        Vec3d origin = new Vec3d(entity.posX, entity.posY, entity.posZ);
        ObstacleAction retainedAction = ObstacleAction.IGNORE;
        for (AIRaycast rayVec : rayVectors) {
            ObstacleAction action = rayVec.rayTraceLineOnEntities(this, entities, rayDistance, frontSize).getAction();
            if (retainedAction.ordinal() < action.ordinal()) {
                retainedAction = action;
                if (retainedAction == ObstacleAction.STOP)
                    return retainedAction;
            }
        }
        return retainedAction;
    }

    public enum ObstacleAction {
        IGNORE, SLOW_DOWN, STEER_LEFT, STEER_RIGHT, STOP
    }
}

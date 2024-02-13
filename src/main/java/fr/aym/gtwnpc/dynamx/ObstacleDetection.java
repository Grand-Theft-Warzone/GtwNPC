package fr.aym.gtwnpc.dynamx;

import com.google.common.base.Predicates;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import fr.aym.gtwnpc.utils.OOBB;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
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
                jomlRot);
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
        int checkFar2 = mySpeed > 35 ? 16 : mySpeed > 20 ? 9 : mySpeed > 5 ? 7 : mySpeed > 2 ? 4 : 3;
        AxisAlignedBB front = getDetectionAABB(checkFar2);
        List<Entity> entities = entity.world.getEntitiesInAABBexcluding(entity, front, Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && entity.getRidingEntity() == null));
        List<BaseVehicleEntity<?>> vehicles = entities.stream().filter(e -> e instanceof BaseVehicleEntity).map(e -> (BaseVehicleEntity<?>) e).collect(Collectors.toList());
        entities.removeIf(e -> e instanceof BaseVehicleEntity);
        ObstacleAction retainedAction = ObstacleAction.IGNORE;
        checkFar2 *= 2;
        List<com.jme3.math.Vector3f> rayVectors = createRayVectors(checkFar2);
        if (!entities.isEmpty()) {
            ObstacleAction action = avoidCollisionWithEntities(entities, rayVectors, checkFar2, 4);
            if (retainedAction.ordinal() < action.ordinal()) {
                retainedAction = action;
            }
            //System.out.println("Retained action after entities: " + retainedAction);
        }
        if (!vehicles.isEmpty()) {
            ObstacleAction action = avoidCollisionWithVehicles(vehicles, rayVectors, checkFar2, 4);
            if (retainedAction.ordinal() < action.ordinal()) {
                retainedAction = action;
            }
            //System.out.println("Retained action after vehicles: " + retainedAction);
        }
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
            case STOP:
              //  System.out.println("STOP " + mySpeed + " " + retainedAction);
                int controls = autopilotModule.getControls();
                if (mySpeed > 0.5f)
                    controls |= 4; // braking
                else
                    controls |= 32; // handbrake
                controls &= ~2; // no acceleration
                autopilotModule.setControls(controls);
                break;
        }
    }

    public List<com.jme3.math.Vector3f> createRayVectors(float rayDistance) {
        List<com.jme3.math.Vector3f> vectors = new ArrayList<>();
        float increment = (float) (Math.PI / 2 / 14);
        float angle = (float) (Math.PI / 4) - increment;
        float steering = autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : autopilotModule.getSteerForce();
        angle += steering * 0.35f;
        com.jme3.math.Vector3f rayVecBase = com.jme3.math.Vector3f.UNIT_Z;
        float[] rayDistanceFactors = new float[]{0.23f, 0.26f, 0.3f, 0.5f, 0.6f, 0.75f, 0.925f, 1f, 0.975f, 0.925f, 0.85f, 0.75f, 0.6f, 0.5f, 0.3f, 0.26f};
        for (float rayDistanceFactor : rayDistanceFactors) {
            com.jme3.math.Vector3f rayVec = Vector3fPool.get(rayVecBase).multLocal(rayDistance * rayDistanceFactor);
            Quaternion q = QuaternionPool.get();
            q.fromAngles(0, angle, 0);
            rayVec = DynamXGeometry.rotateVectorByQuaternion(rayVec, q);
            rayVec = DynamXGeometry.rotateVectorByQuaternion(rayVec, entity.physicsRotation);
            rayVec.addLocal(entity.physicsPosition);
            vectors.add(rayVec);
            angle -= increment;
        }
        return vectors;
    }

    public ObstacleAction avoidCollisionWithVehicles(List<BaseVehicleEntity<?>> vehicles, List<com.jme3.math.Vector3f> rayVectors, float rayDistance, int frontSize) {
        ObstacleAction retainedAction = ObstacleAction.IGNORE;
        for (int i = 0; i < rayVectors.size(); i++) {
            com.jme3.math.Vector3f rayVec = rayVectors.get(i);
            ObstacleAction action = rayTraceLineOnVehicles(vehicles, entity.physicsPosition, new com.jme3.math.Vector3f(rayVec.x, rayVec.y, rayVec.z), rayDistance, frontSize, i > 4d && i < 10);
            if (retainedAction.ordinal() < action.ordinal()) {
                retainedAction = action;
                //System.out.println("Collision with " + i);
                //if (retainedAction == ObstacleAction.STOP)
                //  return retainedAction;
            }
        }
        return retainedAction;
    }

    public float editRayVec(com.jme3.math.Vector3f rayOrigin, com.jme3.math.Vector3f rayVec, float hitDist, OOBB otherOOBB, float steerDelta) {
        com.jme3.math.Vector3f basic = rayVec;
        rayVec = rayVec.subtract(rayOrigin);
        rayVec.multLocal(1.2f);
        Quaternion q = QuaternionPool.get();
        q.fromAngles(0, steerDelta * 0.35f, 0);
        rayVec = DynamXGeometry.rotateVectorByQuaternion(rayVec, q);
        rayVec.addLocal(rayOrigin);
        Vector3f intersection = otherOOBB.calculateIntercept(new Vector3f(rayOrigin.x, rayOrigin.y, rayOrigin.z), new Vector3f(rayVec.x, rayVec.y, rayVec.z));
        if (intersection != null) {
            float d3 = rayOrigin.distance(new com.jme3.math.Vector3f(intersection.x, intersection.y, intersection.z));
            //System.out.println("Vec from " + basic + " to " + rayVec + " " + intersection + " " + hitDist + " " + steerDelta + " " + d3);
            if (d3 > hitDist) {
                return d3; // intersection but farther
            }
            return 0; // intersection and closer
        }
        return -1; // no intersection
    }

    public float searchBetterSteering(com.jme3.math.Vector3f rayOrigin, com.jme3.math.Vector3f rayVec, float hitDist, OOBB otherOOBB, float currentSteer, float forcedSteering) {
        float lessWorst = currentSteer;
        float lessWorstDist = hitDist;
        boolean forcedLeft = forcedSteering != -10 && forcedSteering < currentSteer;
        boolean forcedRight = forcedSteering != -10 && forcedSteering > currentSteer;
       // System.out.println("Forced " + forcedSteering + " " + currentSteer + " " + forcedLeft + " " + forcedRight);
        if (forcedRight || (!forcedLeft && entity.world.rand.nextBoolean())) {
            for (float steerDelta = 1 - currentSteer; currentSteer + steerDelta > -1; steerDelta -= 0.1f) {
              //  System.out.println("1-Test Better: " + currentSteer + " " + steerDelta + " " + lessWorst + " " + lessWorstDist + " " + forcedSteering);
                if (forcedSteering != -10 && ((forcedSteering > currentSteer && currentSteer + steerDelta < forcedSteering) || (forcedSteering < currentSteer && currentSteer + steerDelta > forcedSteering)))
                    continue;
                float result = editRayVec(rayOrigin, rayVec, lessWorstDist, otherOOBB, steerDelta);
                if (result == -1) {
                //    System.out.println("1-Better steering found " + currentSteer + " " + steerDelta + " " + lessWorst + " " + lessWorstDist + " " + result);
                    return currentSteer + steerDelta;
                } else if (result != 0) {
                    lessWorst = currentSteer + steerDelta;
                    lessWorstDist = result;
                }
            }
        } else {
            for (float steerDelta = -1 - currentSteer; currentSteer + steerDelta < 1; steerDelta += 0.1f) {
            //    System.out.println("2-Test Better: " + currentSteer + " " + steerDelta + " " + lessWorst + " " + lessWorstDist + " " + forcedSteering);
                if (forcedSteering != -10 && ((forcedSteering > currentSteer && currentSteer + steerDelta < forcedSteering) || (forcedSteering < currentSteer && currentSteer + steerDelta > forcedSteering)))
                    continue;
                float result = editRayVec(rayOrigin, rayVec, lessWorstDist, otherOOBB, steerDelta);
                if (result == -1) {
              //      System.out.println("2-Better steering found " + currentSteer + " " + steerDelta + " " + lessWorst + " " + lessWorstDist + " " + result);
                    return currentSteer + steerDelta;
                } else if (result != 0) {
                    lessWorst = currentSteer + steerDelta;
                    lessWorstDist = result;
                }
            }
        }
        return lessWorst;
    }

    public Vector3f lastVehicleHit = null;

    public boolean updateCollisionWith(BaseVehicleEntity<?> entity, com.jme3.math.Vector3f rayOrigin, com.jme3.math.Vector3f rayVec, float rayDistance, OOBB otherOOBB, boolean searchPathOut) {
        ObstacleDetection oth = entity.getModuleByType(AutopilotModule.class) != null ? entity.getModuleByType(AutopilotModule.class).getObstacleDetection() : null;
        if (oth == null)
            return true;
        float otherSpeed = entity.getPhysicsHandler() != null && entity.ticksExisted > 10 ? entity.getPhysicsHandler().getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : 0;
        CollisionCluster collisionCluster = collisionClusters.stream().filter(c -> c.isCollidingWith(entity)).findFirst().orElse(null);
        if (collisionCluster != null) {
            float stea = autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : autopilotModule.getSteerForce();
            if (collisionCluster.getObstacleDetectionA() == this && collisionCluster.isCollidingA()) {
                if (collisionCluster.isATurningAround()) {
                    float steer = searchBetterSteering(rayOrigin, rayVec, rayDistance, otherOOBB, this.autopilotModule.getSteerForce(), autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : -10);
                    if (steer != stea) {
                        this.autopilotModule.setForcedSteering(steer);
                        this.autopilotModule.setForcedSteeringTime(20);
                        //System.out.println("A-Steering changed to " + steer);
                    }
                }
                return false;
            }
            if (collisionCluster.getObstacleDetectionB() == this && collisionCluster.isCollidingB()) {
                if (collisionCluster.isBTurningAround()) {
                    float steer = searchBetterSteering(rayOrigin, rayVec, rayDistance, otherOOBB, this.autopilotModule.getSteerForce(), autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : -10);
                    if (steer != stea) {
                        this.autopilotModule.setForcedSteering(steer);
                        this.autopilotModule.setForcedSteeringTime(20);
                        //System.out.println("B-Steering changed to " + steer);
                    }
                }
                return false;
            }
            collisionCluster.incrementCollision(this);
            boolean imTurningAround = collisionCluster.getObstacleDetectionA() == this ? collisionCluster.isATurningAround() : collisionCluster.isBTurningAround();
            if (collisionCluster.getCollisionTime() > 20) {
                //System.out.println("Collision cluster too long " + collisionCluster);
                //Determine who is at the right
                float yawDiff = this.entity.rotationYaw - entity.rotationYaw;
                if (yawDiff < 0 || collisionCluster.getCollisionTime() > 200 || imTurningAround || otherSpeed < 0.1f) {
                    if (!searchPathOut || true) {
                        if (collisionCluster.getCollisionTime() > 200 * 3 && entity.world.rand.nextInt(100) == 0) {
                            System.out.println("Dead " + entity + " " + this.entity + " " + collisionCluster);
                            entity.setDead();
                        }
                        return true;
                    }
                  //  System.out.println("Avoiding " + this.entity + " " + entity + " " + collisionCluster + " " + autopilotModule.getForcedSteering()+ " " + autopilotModule.getForcedSteeringTime() +" "+autopilotModule.getSteerForce());
                    float steer = searchBetterSteering(rayOrigin, rayVec, rayDistance, otherOOBB, this.autopilotModule.getSteerForce(), autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : -10);
                    if (steer != this.autopilotModule.getSteerForce()) {
                        this.autopilotModule.setForcedSteering(steer);
                        this.autopilotModule.setForcedSteeringTime(20);
                        //System.out.println("Steering changed to " + steer);
                        if (collisionCluster.getObstacleDetectionA() == this) {
                            collisionCluster.setATurningAround(true);
                        } else {
                            collisionCluster.setBTurningAround(true);
                        }
                        //I'm at the right and can avoid
                        return false;
                    } else {
                        //System.out.println("Avoid failed");
                        if (collisionCluster.getObstacleDetectionA() == this) {
                            collisionCluster.setATurningAround(false);
                        } else {
                            collisionCluster.setBTurningAround(false);
                        }
                        if (collisionCluster.getCollisionTime() > 200 * 5 && entity.world.rand.nextInt(10) == 0) {
                            System.out.println("Dead " + entity + " " + this.entity + " " + collisionCluster);
                            entity.setDead();
                        }
                        return true;
                    }
                }
            }
            return true;
        }
        collisionCluster = new CollisionCluster(this, oth);
        collisionCluster.incrementCollision(this);
        collisionCluster.incrementCollision(oth);
        collisionClusters.add(collisionCluster);
        oth.collisionClusters.add(collisionCluster);

        CollisionSimplex collisionSimplex = collisionSimplexs.stream().filter(c -> c.isCollidingWith(entity)).findFirst().orElse(null);
        if (collisionSimplex != null) {
            if (collisionSimplex.isCollidingB()) {
                if (collisionSimplex.isATurningAround()) {
                    float steer = searchBetterSteering(rayOrigin, rayVec, rayDistance, otherOOBB, this.autopilotModule.getSteerForce(), autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : -10);
                    if (steer != this.autopilotModule.getSteerForce()) {
                        this.autopilotModule.setForcedSteering(steer);
                        this.autopilotModule.setForcedSteeringTime(20);
                        //System.out.println("SIMPLEX-Steering changed to " + steer);
                    }
                }
                return false;
            }
            collisionSimplex.incrementCollision(oth);
            if (collisionSimplex.getCollisionTime() > 200 || collisionSimplex.isATurningAround() || otherSpeed < 0.1f) {
                //System.out.println("SIMPLEX-Collision cluster too long " + collisionCluster);
                //Determine who is at the right
                float yawDiff = this.entity.rotationYaw - entity.rotationYaw;
                if (yawDiff < 0 || collisionCluster.getCollisionTime() > 200) {
                    if (!searchPathOut || true) {
                        if (collisionSimplex.getCollisionTime() > 200 * 5 && entity.world.rand.nextInt(100) == 0) {
                            System.out.println("SIMPLEX-Dead " + entity + " " + this.entity + " " + collisionSimplex);
                            entity.setDead();
                        }
                        return true;
                    }
                //    System.out.println("Avoiding " + this.entity + " " + entity + " " + collisionCluster + " " + autopilotModule.getForcedSteering()+ " " + autopilotModule.getForcedSteeringTime() +" "+autopilotModule.getSteerForce());
                    float steer = searchBetterSteering(rayOrigin, rayVec, rayDistance, otherOOBB, this.autopilotModule.getSteerForce(), autopilotModule.getForcedSteeringTime() > 0 ? autopilotModule.getForcedSteering() : -10);
                    if (steer != this.autopilotModule.getSteerForce()) {
                        this.autopilotModule.setForcedSteering(steer);
                        this.autopilotModule.setForcedSteeringTime(20);
                        //System.out.println("SIMPLEX-Steering changed to " + steer);
                        collisionSimplex.setATurningAround(true);
                        //I'm at the right and can avoid
                        return false;
                    } else {
                        //System.out.println("SIMPLEX-Avoid failed");
                        collisionSimplex.setATurningAround(false);
                        if (collisionSimplex.getCollisionTime() > 200 * 8 && entity.world.rand.nextInt(10) == 0) {
                            System.out.println("SIMPLEX-Dead " + entity + " " + this.entity + " " + collisionSimplex);
                            entity.setDead();
                        }
                        return true;
                    }
                }
            }
            return true;
        }
        collisionCluster = new CollisionCluster(this, oth);
        collisionCluster.incrementCollision(this);
        collisionCluster.incrementCollision(oth);
        collisionClusters.add(collisionCluster);
        oth.collisionClusters.add(collisionCluster);

        collisionSimplex = new CollisionSimplex(oth);
        collisionSimplex.incrementCollision(oth);
        collisionSimplexs.add(collisionSimplex);
        return true;
    }

    public ObstacleAction rayTraceLineOnVehicles(List<BaseVehicleEntity<?>> vehicles, com.jme3.math.Vector3f origin, com.jme3.math.Vector3f rayVec, float rayDistance, int frontSize, boolean searchPathOut) {
        Vector3f hitVec = null;
        BaseVehicleEntity<?> hitEntity = null;
        double hitDistance = rayDistance;
        boolean needsSlow = false;
        for (int j = 0; j < vehicles.size(); ++j) {
            BaseVehicleEntity<?> other = vehicles.get(j);
            OOBB otherOOBB = getEntityOOBB(other);
            Vector3f intersection = otherOOBB.calculateIntercept(new Vector3f(origin.x, origin.y, origin.z), new Vector3f(rayVec.x, rayVec.y, rayVec.z));
            double d3 = intersection == null ? 0 : origin.subtract(new com.jme3.math.Vector3f(intersection.x, intersection.y, intersection.z)).length();
            if(intersection != null) {
                //check intersection is between the origin and the rayVec
                /*float dot = rayVec.subtract(origin).dot(new com.jme3.math.Vector3f(intersection.x, intersection.y, intersection.z).subtract(origin));
                if(dot < 0) {
                    intersection = null;
                    System.out.println("Enculé de GPT");
                }*/
                double di = origin.distance(new com.jme3.math.Vector3f(intersection.x, intersection.y, intersection.z));
                double dr = origin.distance(rayVec);
                if(di > dr) {
                    intersection = null;
                    //System.out.println("Enculé de GPT");
                }
               // System.out.println("From " + origin + " to " + rayVec + " " + intersection + " " + di + " " + dr + " " + searchPathOut);
            }
            if (myOOBB.collidesWithOOBB(otherOOBB) && false) {
                //System.out.println("8Direct collides with vehicle: " + other);
                if (!updateCollisionWith(other, origin, rayVec, rayDistance, otherOOBB, !searchPathOut)) {
                    //System.out.println("Already collided with vehicle: " + other);
                    needsSlow = true;
                    continue;
                }
                hitEntity = other;
                hitVec = intersection;
                hitDistance = 0.0D;
            } else if (intersection != null) {
                // ObstacleDetection oth = other.getModuleByType(AutopilotModule.class) != null ? other.getModuleByType(AutopilotModule.class).getObstacleDetection() : null;
                /*if (oth != null && oth.collidedWith.contains(entity)) {
                    System.out.println("Already collided with vehicle: " + other);
                    continue;
                }*/
                if (!updateCollisionWith(other, origin, rayVec, rayDistance, otherOOBB, !searchPathOut)) {
                    //System.out.println("Already collided with vehicle: " + other);
                    needsSlow = true;
                    continue;
                }
                if ((d3 < hitDistance || hitDistance == 0.0D)) {
                    hitEntity = other;
                    hitVec = intersection;
                    hitDistance = d3;
                }
            }
        }
        if (hitVec != null)
            lastVehicleHit = hitVec != null ? new Vector3f(hitVec.x, hitVec.y, hitVec.z) : null;
        if (hitEntity != null && false)
            System.out.println("Hit entity: " + hitEntity + " at " + hitVec + " " + rayDistance + " " + hitDistance);
        return hitEntity != null ? ((hitDistance - frontSize) > 4 ? ObstacleAction.SLOW_DOWN : ObstacleAction.STOP) : needsSlow ? ObstacleAction.SLOW_DOWN : ObstacleAction.IGNORE;
    }

    public ObstacleAction avoidCollisionWithEntities(List<Entity> entities, List<com.jme3.math.Vector3f> rayVectors, float rayDistance, int frontSize) {
        Vec3d origin = new Vec3d(entity.posX, entity.posY, entity.posZ);
        ObstacleAction retainedAction = ObstacleAction.IGNORE;
        for (com.jme3.math.Vector3f rayVec : rayVectors) {
            ObstacleAction action = rayTraceLineOnEntities(entities, origin, new Vec3d(rayVec.x, rayVec.y, rayVec.z), rayDistance, frontSize);
            if (retainedAction.ordinal() < action.ordinal()) {
                retainedAction = action;
                if (retainedAction == ObstacleAction.STOP)
                    return retainedAction;
            }
        }
        return retainedAction;
    }

    public Vec3d lastEntityHit = null;

    public ObstacleAction rayTraceLineOnEntities(List<Entity> entities, Vec3d origin, Vec3d rayVec, float rayDistance, int frontSize) {
        Vec3d hitVec = null;
        Entity hitEntity = null;
        double hitDistance = rayDistance;
        for (int j = 0; j < entities.size(); ++j) {
            Entity entity1 = entities.get(j);
            AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow((double) entity1.getCollisionBorderSize());
            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(origin, rayVec);
            if (myOOBB.collidesWithAABB(axisalignedbb)) {
                //System.out.println("Direct collides with entity: " + entity1);
                if (hitDistance >= 0.0D) {
                    hitEntity = entity1;
                    hitVec = raytraceresult == null ? origin : raytraceresult.hitVec;
                    hitDistance = 0.0D;
                }
            } else if (raytraceresult != null) {
                double d3 = origin.distanceTo(raytraceresult.hitVec);

                if (d3 < hitDistance || hitDistance == 0.0D) {
                    if (entity1.getLowestRidingEntity() == entity.getLowestRidingEntity() && !entity1.canRiderInteract()) {
                        if (hitDistance == 0.0D) {
                            hitEntity = entity1;
                            hitVec = raytraceresult.hitVec;
                        }
                    } else {
                        hitEntity = entity1;
                        hitVec = raytraceresult.hitVec;
                        hitDistance = d3;
                    }
                }
            }
        }
        lastEntityHit = hitVec;
        //if (hitEntity != null)
        //    System.out.println("Hit entity: " + hitEntity + " at " + hitVec + " " + rayDistance + " " + hitDistance);
        return hitEntity != null ? ((hitDistance - frontSize) > 4 ? ObstacleAction.SLOW_DOWN : ObstacleAction.STOP) : ObstacleAction.IGNORE;
    }

    public enum ObstacleAction {
        IGNORE, SLOW_DOWN, STOP
    }
}

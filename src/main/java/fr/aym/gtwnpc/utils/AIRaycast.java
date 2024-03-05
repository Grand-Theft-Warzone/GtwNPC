package fr.aym.gtwnpc.utils;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.gtwnpc.dynamx.AutopilotModule;
import fr.aym.gtwnpc.dynamx.ObstacleDetection;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class AIRaycast {
    public Vec3d lastEntityHit = null;
    public org.joml.Vector3f lastVehicleHit = null;

    private final BaseVehicleEntity<?> entity;
    private final Vector3f origin;
    private final Vector3f rayVec;
    private final boolean isInFront;

    public HitInfo rayTraceLineOnEntities(ObstacleDetection obstacleDetection, List<Entity> entities, float rayDistance, int frontSize) {
        if(!isInFront())
            return new HitInfo(null, null, 0, ObstacleDetection.ObstacleAction.IGNORE);
        Vec3d origin = new Vec3d(this.origin.x, this.origin.y, this.origin.z);
        Vec3d rayVec = new Vec3d(this.rayVec.x, this.rayVec.y, this.rayVec.z);
        Vec3d hitVec = null;
        Entity hitEntity = null;
        double hitDistance = rayDistance;
        for (int j = 0; j < entities.size(); ++j) {
            Entity entity1 = entities.get(j);
            AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow((double) entity1.getCollisionBorderSize());
            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(origin, rayVec);
            if (obstacleDetection.getMyOOBB().collidesWithAABB(axisalignedbb)) {
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
        if (hitEntity == null)
            return new HitInfo(null, null, 0, ObstacleDetection.ObstacleAction.IGNORE);
        if (hitDistance - frontSize > 4)
            return new HitInfo(hitEntity, new Vector3f((float) hitVec.x, (float) hitVec.y, (float) hitVec.z), (float) hitDistance, ObstacleDetection.ObstacleAction.SLOW_DOWN);
        return new HitInfo(hitEntity, new Vector3f((float) hitVec.x, (float) hitVec.y, (float) hitVec.z), (float) hitDistance, ObstacleDetection.ObstacleAction.STOP);
    }

    public boolean updateCollisionWith(BaseVehicleEntity<?> otherEntity, com.jme3.math.Vector3f rayOrigin, com.jme3.math.Vector3f rayVec, float rayDistance, OOBB otherOOBB, boolean searchPathOut) {
        ObstacleDetection oth = otherEntity.getModuleByType(AutopilotModule.class) != null ? otherEntity.getModuleByType(AutopilotModule.class).getObstacleDetection() : null;
        if (oth == null)
            return true;
        float otherSpeed = otherEntity.getPhysicsHandler() != null && otherEntity.ticksExisted > 10 ? otherEntity.getPhysicsHandler().getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : 0;
        return true;
    }

    public HitInfo rayTraceLineOnVehicles(ObstacleDetection obstacleDetection, List<BaseVehicleEntity<?>> vehicles, float rayDistance, int frontSize) {
        org.joml.Vector3f hitVec = null;
        BaseVehicleEntity<?> hitEntity = null;
        double hitDistance = rayDistance;
        //boolean needsSlow = false;
        for (int j = 0; j < vehicles.size(); ++j) {
            BaseVehicleEntity<?> other = vehicles.get(j);
            OOBB otherOOBB = obstacleDetection.getEntityOOBB(other);
            org.joml.Vector3f intersection = otherOOBB.calculateIntercept(new org.joml.Vector3f(origin.x, origin.y, origin.z), new org.joml.Vector3f(rayVec.x, rayVec.y, rayVec.z));
            double d3 = intersection == null ? 0 : origin.subtract(new com.jme3.math.Vector3f(intersection.x, intersection.y, intersection.z)).length();
            if (intersection != null) {
                //check intersection is between the origin and the rayVec
                /*float dot = rayVec.subtract(origin).dot(new com.jme3.math.Vector3f(intersection.x, intersection.y, intersection.z).subtract(origin));
                if(dot < 0) {
                    intersection = null;
                    System.out.println("Enculé de GPT");
                }*/
                double di = origin.distance(new com.jme3.math.Vector3f(intersection.x, intersection.y, intersection.z));
                double dr = origin.distance(rayVec);
                if (di > dr) {
                    intersection = null;
                    if(!entity.getPassengers().isEmpty())
                        System.out.println("Enculé de GPT");
                }
                if(!entity.getPassengers().isEmpty())
                    System.out.println("From " + origin + " to " + rayVec + " " + intersection + " " + di + " " + dr);
            }
            if (obstacleDetection.getMyOOBB().collidesWithOOBB(otherOOBB) && false) {
                //System.out.println("8Direct collides with vehicle: " + other);
                /*if (!updateCollisionWith(other, origin, rayVec, rayDistance, otherOOBB, !searchPathOut)) {
                    //System.out.println("Already collided with vehicle: " + other);
                    needsSlow = true;
                    continue;
                }*/
                hitEntity = other;
                hitVec = intersection;
                hitDistance = 0.0D;
            } else if (intersection != null) {
                // ObstacleDetection oth = other.getModuleByType(AutopilotModule.class) != null ? other.getModuleByType(AutopilotModule.class).getObstacleDetection() : null;
                /*if (oth != null && oth.collidedWith.contains(entity)) {
                    System.out.println("Already collided with vehicle: " + other);
                    continue;
                }*/
                /*if (!updateCollisionWith(other, origin, rayVec, rayDistance, otherOOBB, !searchPathOut)) {
                    //System.out.println("Already collided with vehicle: " + other);
                    needsSlow = true;
                    continue;
                }*/
                if ((d3 < hitDistance || hitDistance == 0.0D)) {
                    hitEntity = other;
                    hitVec = intersection;
                    hitDistance = d3;
                }
            }
        }
        if (hitVec != null)
            lastVehicleHit = hitVec != null ? new org.joml.Vector3f(hitVec.x, hitVec.y, hitVec.z) : null;
        //  if (hitEntity != null)
        //    System.out.println("Hit entity: " + hitEntity + " at " + hitVec + " " + rayDistance + " " + hitDistance);
        // else
        //   System.out.println("No hit entity");
        if (hitEntity == null)
            return new HitInfo(null, null, 0, ObstacleDetection.ObstacleAction.IGNORE);
        if (hitDistance - frontSize > 4) {
            // check relative angle of the two vehicles to check if we are following the other
            Quaternion myRot = entity.physicsRotation;
            Quaternion otherRot = hitEntity.physicsRotation;
            float dot = new Quaternionf(myRot.getX(), myRot.getY(), myRot.getZ(), myRot.getW()).dot(new Quaternionf(otherRot.getX(), otherRot.getY(), otherRot.getZ(), otherRot.getW()));
            if (dot > 0.5f) {
                if (!entity.getPassengers().isEmpty())
                    System.out.println("Following vehicle " + hitEntity);
                return new HitInfo(hitEntity, new Vector3f(hitVec.x, hitVec.y, hitVec.z), (float) hitDistance, ObstacleDetection.ObstacleAction.SLOW_DOWN);
            }
            if (hitDistance - frontSize > 12 && isInFront()) {
                if (!entity.getPassengers().isEmpty())
                    System.out.println("Staying away from vehicle " + hitEntity);
                return new HitInfo(hitEntity, new Vector3f(hitVec.x, hitVec.y, hitVec.z), (float) hitDistance, ObstacleDetection.ObstacleAction.SLOW_DOWN);
            }
            if (!entity.getPassengers().isEmpty())
                System.out.println("Collided2 with vehicle " + hitEntity + " " + hitDistance + " " + hitVec + " " + isInFront());
            return new HitInfo(hitEntity, new Vector3f(hitVec.x, hitVec.y, hitVec.z), (float) hitDistance, ObstacleDetection.ObstacleAction.STOP);
        }
        if (!isInFront()) {
            float otherSpeed = hitEntity.getPhysicsHandler() != null && hitEntity.ticksExisted > 10 ? hitEntity.getPhysicsHandler().getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : 0;
            if (otherSpeed < 0.1f) {
                if (!entity.getPassengers().isEmpty())
                    System.out.println("Passing in front of stopped entity " + hitEntity);
                return new HitInfo(hitEntity, new Vector3f(hitVec.x, hitVec.y, hitVec.z), (float) hitDistance, ObstacleDetection.ObstacleAction.SLOW_DOWN);
            }
        }
        if (!entity.getPassengers().isEmpty())
            System.out.println("Collided1 with vehicle " + hitEntity + " " + hitDistance + " " + hitVec + " " + isInFront());
        return new HitInfo(hitEntity, new Vector3f(hitVec.x, hitVec.y, hitVec.z), (float) hitDistance, ObstacleDetection.ObstacleAction.STOP);
        //return hitEntity != null ? ((hitDistance - frontSize) > 4 ? ObstacleDetection.ObstacleAction.SLOW_DOWN : ObstacleDetection.ObstacleAction.STOP) : needsSlow ? ObstacleDetection.ObstacleAction.SLOW_DOWN : ObstacleDetection.ObstacleAction.IGNORE;
    }

    @Getter
    @RequiredArgsConstructor
    public static class HitInfo {
        private final Entity hitEntity;
        private final Vector3f hitPos;
        private final float hitDistance;
        private final ObstacleDetection.ObstacleAction action;

        @Override
        public String toString() {
            return "HitInfo{" +
                    "hitEntity=" + hitEntity +
                    ", hitPos=" + hitPos +
                    ", hitDistance=" + hitDistance +
                    ", action=" + action +
                    '}';
        }
    }
}

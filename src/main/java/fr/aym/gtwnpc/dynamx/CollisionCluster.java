package fr.aym.gtwnpc.dynamx;

import fr.dynamx.common.entities.BaseVehicleEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
public class CollisionCluster
{
    private final ObstacleDetection obstacleDetectionA;
    private final ObstacleDetection obstacleDetectionB;
    private int collisionTime;
    private boolean isCollidingA;
    private boolean isCollidingB;
    private boolean isATurningAround;
    private boolean isBTurningAround;

    public boolean isCollidingWith(BaseVehicleEntity<?> entity) {
        return obstacleDetectionA.getEntity() == entity || obstacleDetectionB.getEntity() == entity;
    }

    public void incrementCollision(ObstacleDetection detection) {
        if (obstacleDetectionA == detection) {
            isCollidingA = true;
        } else if (obstacleDetectionB == detection) {
            isCollidingB = true;
        }
        System.out.println("Incrementing collision " + detection+ " " + this);
        collisionTime++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollisionCluster that = (CollisionCluster) o;
        return Objects.equals(obstacleDetectionA, that.obstacleDetectionA) && Objects.equals(obstacleDetectionB, that.obstacleDetectionB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obstacleDetectionA, obstacleDetectionB);
    }

    @Override
    public String toString() {
        return "CollisionCluster{" +
                "obstacleDetectionA=" + obstacleDetectionA +
                ", obstacleDetectionB=" + obstacleDetectionB +
                ", collisionTime=" + collisionTime +
                ", isCollidingA=" + isCollidingA +
                ", isCollidingB=" + isCollidingB +
                '}';
    }
}

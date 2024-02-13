package fr.aym.gtwnpc.dynamx;

import fr.dynamx.common.entities.BaseVehicleEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
public class CollisionSimplex {
    private final ObstacleDetection obstacleDetectionB;
    private int collisionTime;
    private boolean isCollidingB;
    private boolean isATurningAround;

    public boolean isCollidingWith(BaseVehicleEntity<?> entity) {
        return obstacleDetectionB.getEntity() == entity;
    }

    public void incrementCollision(ObstacleDetection detection) {
        if (obstacleDetectionB == detection) {
            isCollidingB = true;
        }
        //System.out.println("Incrementing collision " + detection + " " + this);
        collisionTime++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollisionSimplex that = (CollisionSimplex) o;
        return Objects.equals(obstacleDetectionB, that.obstacleDetectionB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obstacleDetectionB);
    }

    @Override
    public String toString() {
        return "CollisionSimplex{" +
                "obstacleDetectionB=" + obstacleDetectionB +
                ", collisionTime=" + collisionTime +
                ", isCollidingB=" + isCollidingB +
                '}';
    }
}

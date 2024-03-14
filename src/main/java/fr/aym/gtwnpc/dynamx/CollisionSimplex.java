package fr.aym.gtwnpc.dynamx;

import fr.dynamx.common.entities.BaseVehicleEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.entity.Entity;

import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
public class CollisionSimplex {
    private final Entity obstacle;
    private int collisionTime;
    private boolean isCollidingB;
    private boolean isATurningAround;

    public boolean isCollidingWith(Entity entity) {
        return obstacle == entity;
    }

    public void incrementCollision() {
        //System.out.println("Incrementing collision " + detection + " " + this);
        collisionTime++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollisionSimplex that = (CollisionSimplex) o;
        return Objects.equals(obstacle, that.obstacle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obstacle);
    }

    @Override
    public String toString() {
        return "CollisionSimplex{" +
                "obstacleDetectionB=" + obstacle +
                ", collisionTime=" + collisionTime +
                ", isCollidingB=" + isCollidingB +
                '}';
    }
}

package fr.aym.gtwnpc.path;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;

public enum NodeType
{
    UNDEFINED(() -> {
        throw new IllegalStateException("Undefined node type");
    }, false, 50),
    PEDESTRIAN(PedestrianPathNodes::getInstance, false, 10),
    CAR_CITY_LOW_SPED(CarPathNodes::getInstance, true, 30),
    CAR_CITY(CarPathNodes::getInstance, true, 50),
    CAR_HIGHWAY(CarPathNodes::getInstance, true, 90),
    CAR_OFFROAD(CarPathNodes::getInstance, true, 70);

    private final Callable<PathNodesManager> instanceSupplier;
    private final boolean areOneWayNodes;
    private final int maxSpeed;

    NodeType(Callable<PathNodesManager> instanceSupplier, boolean areOneWayNodes, int maxSpeed) {
        this.instanceSupplier = instanceSupplier;
        this.areOneWayNodes = areOneWayNodes;
        this.maxSpeed = maxSpeed;
    }

    @Nonnull
    public PathNodesManager getManager() {
        try {
            return instanceSupplier.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean areOneWayNodes() {
        return areOneWayNodes;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }
}

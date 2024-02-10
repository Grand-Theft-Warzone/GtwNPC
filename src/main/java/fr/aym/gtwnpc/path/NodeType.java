package fr.aym.gtwnpc.path;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;

public enum NodeType
{
    UNDEFINED(() -> {
        throw new IllegalStateException("Undefined node type");
    }, false),
    PEDESTRIAN(PedestrianPathNodes::getInstance, false),
    CAR(CarPathNodes::getInstance, true);

    private final Callable<PathNodesManager> instanceSupplier;
    private final boolean areOneWayNodes;

    NodeType(Callable<PathNodesManager> instanceSupplier, boolean areOneWayNodes) {
        this.instanceSupplier = instanceSupplier;
        this.areOneWayNodes = areOneWayNodes;
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
}

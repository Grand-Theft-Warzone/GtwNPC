package fr.aym.gtwnpc.path;

import javax.annotation.Nonnull;

public enum NodeType
{
    UNDEFINED,
    PEDESTRIAN,
    VEHICLE;

    @Nonnull
    public PathNodesManager getManager() {
        return this == PEDESTRIAN ? PedestrianPathNodes.getInstance() : null;
    }
}

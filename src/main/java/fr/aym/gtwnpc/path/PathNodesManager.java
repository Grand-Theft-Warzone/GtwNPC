package fr.aym.gtwnpc.path;

import net.minecraft.util.math.AxisAlignedBB;

import java.util.Collection;
import java.util.UUID;

public interface PathNodesManager {
    PathNode getNode(UUID id);

    NodeType getNodeType();

    Collection<PathNode> getNodes();

    Collection<PathNode> getNodesWithinAABB(AxisAlignedBB aabb);

    void addNode(PathNode pathNode);

    void removeNode(PathNode pathNode);

    void markDirty();

    boolean hasNode(UUID id);
}

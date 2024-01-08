package fr.aym.gtwnpc.path;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;
import java.util.Queue;
import java.util.UUID;

public interface PathNodesManager {
    PathNode getNode(UUID id);

    NodeType getNodeType();

    Collection<PathNode> getNodes();

    Collection<PathNode> getNodesWithinAABB(AxisAlignedBB aabb);

    void addNode(PathNode pathNode);

    void removeNode(PathNode pathNode);

    void markDirty2();

    boolean hasNode(UUID id);

    PathNode selectRandomPathNode(Vec3d around, float radiusMin, float radiusMax);

    Queue<PathNode> createPathToNode(Vec3d start, PathNode end);
}

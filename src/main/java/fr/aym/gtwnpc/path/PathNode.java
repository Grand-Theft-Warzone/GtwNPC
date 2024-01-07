package fr.aym.gtwnpc.path;

import fr.aym.acslib.utils.nbtserializer.ISerializable;
import fr.aym.acslib.utils.packetserializer.ISerializablePacket;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.network.BBMessagePathNodes;
import lombok.Getter;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import javax.vecmath.Vector3f;
import java.util.*;
import java.util.stream.Collectors;

public class PathNode implements ISerializable, ISerializablePacket {
    @Getter
    private UUID id;
    @Getter
    private Vector3f position;
    private List<PathNode> neighbors;
    private List<UUID> neighborsIds;

    public PathNode() {
    }

    public PathNode(Vector3f position, List<PathNode> neighbors) {
        this.position = position;
        this.neighbors = neighbors;
        this.id = UUID.randomUUID();
    }

    public List<PathNode> getNeighbors(PathNodesManager manager) {
        if (neighbors == null)
            resolveNeighbors(manager);
        return neighbors;
    }

    protected void resolveNeighbors(PathNodesManager manager) {
        neighbors = neighborsIds.stream().map(manager::getNode).collect(Collectors.toList());
        neighborsIds.clear();
        neighborsIds = null;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Object[] getObjectsToSave() {
        if (neighborsIds != null) // Nodes not resolved yet
            return new Object[]{id, position.x, position.y, position.z, neighborsIds};
        return new Object[]{id, position.x, position.y, position.z, neighbors.stream().map(PathNode::getId).collect(Collectors.toList())};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        id = (UUID) objects[0];
        position = new Vector3f((float) objects[1], (float) objects[2], (float) objects[3]);
        neighborsIds = (List<UUID>) objects[4];
        if (neighbors != null) {
            neighbors.clear();
            neighbors = null;
        }
    }

    public AxisAlignedBB getBoundingBox() {
        //TODO CACHE THE VALUE
        return new AxisAlignedBB(position.x - 0.5, position.y - 0.5, position.z - 0.5, position.x + 0.5, position.y + 0.5, position.z + 0.5);
    }

    public void create(PathNodesManager manager, boolean isRemote) {
        if (isRemote)
            GtwNpcMod.network.sendToServer(new BBMessagePathNodes(manager.getNodeType(), this));
        else {
            if (manager.getNodeType() == NodeType.PEDESTRIAN) {
                PedestrianPathNodes.getInstance().addNode(this);
            } else {
                throw new UnsupportedOperationException("Unsupported node type: " + manager.getNodeType());
            }
        }
    }

    public void delete(PathNodesManager manager, boolean isRemote) {
        if (isRemote)
            GtwNpcMod.network.sendToServer(new BBMessagePathNodes(BBMessagePathNodes.Action.REMOVE, manager.getNodeType(), Collections.singletonList(getId())));
        else {
            if (manager.getNodeType() == NodeType.PEDESTRIAN) {
                for (PathNode neighbor : getNeighbors(manager))
                    neighbor.getNeighbors(manager).remove(this);
                PedestrianPathNodes.getInstance().removeNode(this);
            } else {
                throw new UnsupportedOperationException("Unsupported node type: " + manager.getNodeType());
            }
        }
    }

    public void addNeighbor(PathNodesManager manager, PathNode pointedNode, boolean isRemote) {
        if (isRemote)
            GtwNpcMod.network.sendToServer(new BBMessagePathNodes(BBMessagePathNodes.Action.LINK_NODES, manager.getNodeType(), Arrays.asList(this.getId(), pointedNode.getId())));
        else {
            getNeighbors(manager).add(pointedNode);
            pointedNode.getNeighbors(manager).add(this);
            manager.markDirty();
        }
    }

    public void removeNeighbor(PathNodesManager manager, PathNode pointedNode, boolean isRemote) {
        if (isRemote)
            GtwNpcMod.network.sendToServer(new BBMessagePathNodes(BBMessagePathNodes.Action.UNLINK_NODES, manager.getNodeType(), Arrays.asList(this.getId(), pointedNode.getId())));
        else {
            getNeighbors(manager).remove(pointedNode);
            pointedNode.getNeighbors(manager).remove(this);
            manager.markDirty();
        }
    }

    public boolean isIn(AxisAlignedBB box) {
        return position.x > box.minX && position.x < box.maxX && position.y > box.minY && position.y < box.maxY && position.z > box.minZ && position.z < box.maxZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathNode node = (PathNode) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public float getDistance(Vec3d around) {
        return (float) around.subtract(position.x, position.y, position.z).length();
    }

    @Override
    public String toString() {
        return "PathNode{" +
                "id=" + id +
                ", position=" + position +
                ", neighbors=" + neighbors +
                ", neighborsIds=" + neighborsIds +
                '}';
    }

    public double getDistance(Vector3f position) {
        return Math.sqrt(Math.pow(position.x - this.position.x, 2) + Math.pow(position.y - this.position.y, 2) + Math.pow(position.z - this.position.z, 2));
    }
}

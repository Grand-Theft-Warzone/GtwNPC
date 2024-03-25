package fr.aym.gtwnpc.path;

import fr.aym.gtwnpc.block.TETrafficLight;
import fr.dynamx.common.entities.BaseVehicleEntity;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

@Getter
public class TrafficLightNode extends PathNode {
    private BlockPos trafficLightPos;

    public TrafficLightNode() {
    }

    public TrafficLightNode(BlockPos pos, PathNode fromNode, NodeType type) {
        super(fromNode.position, fromNode.neighbors, type);
        this.id = fromNode.id;
        this.neighbors = fromNode.neighbors != null ? new HashSet<>(fromNode.neighbors) : null;
        this.neighborsIds = fromNode.neighborsIds != null ? new HashSet<>(fromNode.neighborsIds) : null;
        this.trafficLightPos = pos;
    }

    @Override
    public void create(PathNodesManager manager, boolean isRemote) {
        super.create(manager, isRemote);
        if(!isRemote) {
            // TODO ASYNC JOB
            for(PathNode node : manager.getNodes()) {
                if(node.neighbors != null) {
                    // Replace the old instance
                    if(node.neighbors.contains(this)) {
                        node.neighbors.remove(this);
                        node.neighbors.add(this);
                    }
                }
            }
        }
    }

    @Override
    public Object[] getObjectsToSave() {
        if (neighborsIds != null) // Nodes not resolved yet
            return new Object[]{id, position.x, position.y, position.z, neighborsIds, nodeType, trafficLightPos};
        return new Object[]{id, position.x, position.y, position.z, neighbors.stream().map(PathNode::getId).collect(Collectors.toList()), nodeType, trafficLightPos};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        super.populateWithSavedObjects(objects);
        if(objects[5] instanceof BlockPos)
            trafficLightPos = (BlockPos) objects[5];
        else if(objects.length > 6 && objects[6] instanceof BlockPos)
            trafficLightPos = (BlockPos) objects[6];
    }

    @Override
    public String toString() {
        return "TrafficLightNode{" + super.toString() +
                ", trafficLightPos=" + trafficLightPos +
                '}';
    }

    @Override
    public boolean canPassThrough(Entity entity) {
        if (trafficLightPos.getY() == 1080)
            return super.canPassThrough(entity);
        TileEntity te = entity.world.getTileEntity(trafficLightPos);
        if (!(te instanceof TETrafficLight)) {
            trafficLightPos = new BlockPos(trafficLightPos.getX(), 1080, trafficLightPos.getZ());
            CarPathNodes.getInstance().markDirty2();
            return super.canPassThrough(entity);
        }
        byte state = ((TETrafficLight) te).getLightState();
        return state == 1 || state == 3;
    }

    public byte getTrafficLightState(World world) {
        if (trafficLightPos.getY() == 1080)
            return -1;
        TileEntity te = world.getTileEntity(trafficLightPos);
        if (!(te instanceof TETrafficLight)) {
            trafficLightPos = new BlockPos(trafficLightPos.getX(), 1080, trafficLightPos.getZ());
            CarPathNodes.getInstance().markDirty2();
            return 0;
        }
        return ((TETrafficLight) te).getLightState();
    }

    public boolean isTrafficLightValid(World world) {
        if (trafficLightPos.getY() == 1080)
            return false;
        TileEntity te = world.getTileEntity(trafficLightPos);
        return te instanceof TETrafficLight;
    }

    @Override
    public boolean isValidSpawnNode(World world, PathNodesManager manager, int maxNeighbors) {
        return !isTrafficLightValid(world) && super.isValidSpawnNode(world, manager, maxNeighbors);
    }
}

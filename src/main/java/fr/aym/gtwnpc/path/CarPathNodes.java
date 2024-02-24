package fr.aym.gtwnpc.path;

import fr.aym.acslib.utils.nbtserializer.ISerializable;
import fr.aym.acslib.utils.nbtserializer.NBTSerializer;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import lombok.Getter;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class CarPathNodes extends WorldSavedData implements PathNodesManager, ISerializable {
    @Getter
    private static CarPathNodes instance;

    private final Map<UUID, PathNode> nodes = new ConcurrentHashMap<>();

    public CarPathNodes(String name) {
        super(name);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Object[] getObjectsToSave() {
        return new Object[]{nodes};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        nodes.clear();
        nodes.putAll((Map<UUID, PathNode>) objects[0]);
    }

    @Override
    public PathNode getNode(UUID id) {
        return nodes.get(id);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CAR_CITY;
    }

    @Override
    public Collection<PathNode> getNodes() {
        return nodes.values();
    }

    @Override
    public Collection<PathNode> getNodesWithinAABB(AxisAlignedBB aabb) {
        return nodes.values().stream().filter(pathNode -> pathNode.isIn(aabb)).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void addNode(PathNode pathNode) {
        nodes.put(pathNode.getId(), pathNode);
        markDirty();
    }

    @Override
    public void markDirty2() {
        markDirty();
    }

    @Override
    public void removeNode(PathNode pathNode) {
        // This DOES NOT remove the links with other nodes
        nodes.remove(pathNode.getId());
        markDirty();
    }

    @Override
    public boolean hasNode(UUID id) {
        return nodes.containsKey(id);
    }

    @Override
    public PathNode selectRandomPathNode(Vec3d around, float radiusMin, float radiusMax) {
        Collection<PathNode> nodes = getNodesWithinAABB(new AxisAlignedBB(around.x - radiusMax, around.y - radiusMax, around.z - radiusMax, around.x + radiusMax, around.y + radiusMax, around.z + radiusMax));
        // System.out.println("Selecting random node from " + nodes.size() + " nodes " + nodes.stream().map(n -> n.getDistance(around )).collect(java.util.stream.Collectors.toList()));
        nodes = nodes.stream().filter(pathNode -> pathNode.getDistance(around) >= radiusMin).collect(java.util.stream.Collectors.toList());
        if (!nodes.isEmpty())
            return nodes.stream().skip(new Random().nextInt(nodes.size())).findFirst().get();
        return null;
    }

    @Override
    public PathNode findNearestNode(Vec3d around, List<PathNode> avoidNodes) {
        return nodes.values().stream().filter(n -> !avoidNodes.contains(n)).min(Comparator.comparingDouble(pathNode -> {
            return pathNode.getDistance(around);
        })).orElse(null);
    }

    @Override
    public Queue<PathNode> createPathToNode(PathNode startNode, PathNode end) {
        //TOO REWORK
        // System.out.println("Start node : " + startNode + " from " + startPos);
        Queue<RouteNode> openSet = new PriorityQueue<>();
        Map<PathNode, RouteNode> allNodes = new HashMap<>();
        RouteNode start = new RouteNode(startNode, null, 0d, startNode.getDistance(end));
        openSet.add(start);
        allNodes.put(startNode, start);
        while (!openSet.isEmpty()) {
            RouteNode next = openSet.poll();
            if (next.getCurrent().equals(end)) {
                List<PathNode> route = new ArrayList<>();
                RouteNode current = next;
                do {
                    route.add(0, current.getCurrent());
                    current = allNodes.get(current.getPrevious());
                } while (current != null);
                //Reverse
                Queue<PathNode> path = new ArrayDeque<>(route.size());
                for (PathNode p : route)
                    path.add(p);
                //System.out.println("Created path : " + route.stream().map(PathNode::getPosition).collect(java.util.stream.Collectors.toList()));
                return path;
            }
            next.getCurrent().getNeighbors(this).forEach(connection -> {
                RouteNode nextNode = allNodes.getOrDefault(connection, new RouteNode(connection));
                allNodes.put(connection, nextNode);
                double newScore = next.getRouteScore() + next.getCurrent().getDistance(connection);
                if (newScore < nextNode.getRouteScore() && (nextNode.getCurrent() == end || nextNode.getCurrent().isIntermediateNode())) {
                    nextNode.setPrevious(next.getCurrent());
                    nextNode.setRouteScore(newScore);
                    nextNode.setEstimatedScore(newScore + connection.getDistance(end));
                    openSet.add(nextNode);
                }
            });
        }
        //System.out.println("No path found from " + startNode + " to " + end);
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTSerializer.unserialize(nbt.getCompoundTag("nodes"), this);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTBase base = NBTSerializer.serialize(this);
        compound.setTag("nodes", base);
        return compound;
    }

    @SubscribeEvent
    public static void load(WorldEvent.Load event) {
        if (event.getWorld().provider.getDimensionType() == DimensionType.OVERWORLD) {
            if (!event.getWorld().isRemote) {
                try {
                    instance = (CarPathNodes) event.getWorld().getPerWorldStorage().getOrLoadData(CarPathNodes.class, "GtwNpcCarPathNodes");
                } catch (Exception e) {
                    instance = null;
                    GtwNpcMod.log.fatal("Cannot load saved car path nodes !", e);
                }
                if (instance == null) {
                    instance = new CarPathNodes("GtwNpcCarPathNodes");
                    event.getWorld().getPerWorldStorage().setData("GtwNpcCarPathNodes", instance);
                }
            } else if (instance == null) {
                instance = new CarPathNodes("ClientCarPathNodes");
            }
        }
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        if (event.getWorld().provider.getDimensionType() == DimensionType.OVERWORLD && instance != null && (!event.getWorld().isRemote || FMLCommonHandler.instance().getMinecraftServerInstance() == null)) {
            instance.nodes.clear();
            instance = null;
        }
    }
}

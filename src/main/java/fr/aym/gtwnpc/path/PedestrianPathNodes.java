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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class PedestrianPathNodes extends WorldSavedData implements PathNodesManager, ISerializable {
    @Getter
    private static PedestrianPathNodes instance;

    private final Map<UUID, PathNode> nodes = new HashMap<>();

    public PedestrianPathNodes(String name) {
        super(name);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Object[] getObjectsToSave() {
        System.out.println("Saving nodes : " + nodes);
        return new Object[]{nodes};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        System.out.println("Loading nodes : " + objects[0]);
        nodes.clear();
        nodes.putAll((Map<UUID, PathNode>) objects[0]);
    }

    @Override
    public PathNode getNode(UUID id) {
        return nodes.get(id);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PEDESTRIAN;
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
        nodes = nodes.stream().filter(pathNode -> pathNode.getDistance(around) >= radiusMin).collect(java.util.stream.Collectors.toList());
        if (nodes.size() > 0)
            return nodes.stream().skip(new Random().nextInt(nodes.size())).findFirst().get();
        return null;
    }

    @Override
    public Queue<PathNode> createPathToNode(Vec3d start, PathNode end) {
        //TOO REWORK
        PathNode startNode = nodes.values().stream().sorted(Comparator.comparingDouble(pathNode -> pathNode.getDistance(start))).findFirst().get();
        System.out.println("Start node : " + startNode +" from " + start);
        Queue<PathNode> path = new ArrayDeque<>();
        Set<PathNode> visited = new HashSet<>();
        path.add(startNode);
        int step = 0;
        // Dijkstra algorithm
        while (path.peek() != end) {
            PathNode currentNode = path.peek();
            PathNode nextNode = currentNode.getNeighbors(this).stream().sorted(Comparator.comparingDouble(pathNode -> pathNode.getDistance(end.getPosition()))).findFirst().get();
            path.add(nextNode);
            step++;
            if(step > 600) {
                System.out.println("Pathfinding failed ! Too many steps in path from " + start + " to " + end.getPosition());
                return null;
            }
        }
        return path;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTSerializer.unserialize(nbt.getCompoundTag("nodes"), this);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTBase base = NBTSerializer.serialize(this);
        System.out.println("Saving nodes : " + base);
        compound.setTag("nodes", base);
        return compound;
    }

    @SubscribeEvent
    public static void load(WorldEvent.Load event) {
        if (event.getWorld().provider.getDimensionType() == DimensionType.OVERWORLD) {
            if (!event.getWorld().isRemote) {
                try {
                    instance = (PedestrianPathNodes) event.getWorld().getPerWorldStorage().getOrLoadData(PedestrianPathNodes.class, "GtwNpcPedestrianPathNodes");
                } catch (Exception e) {
                    instance = null;
                    GtwNpcMod.log.fatal("Cannot load saved garages !", e);
                }
                if (instance == null) {
                    instance = new PedestrianPathNodes("GtwNpcPedestrianPathNodes");
                    event.getWorld().getPerWorldStorage().setData("GtwNpcPedestrianPathNodes", instance);
                }
            } else if (instance == null) {
                instance = new PedestrianPathNodes("ClientPedestrianPathNodes");
            }
        }
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        System.out.println("Unloading nodes");
        if (event.getWorld().provider.getDimensionType() == DimensionType.OVERWORLD && instance != null && !event.getWorld().isRemote) {
            //TODO CLEAR SUR LES CLIENTS EN MULTIJOUEUR
            instance.nodes.clear();
            instance = null;
        }
    }
}

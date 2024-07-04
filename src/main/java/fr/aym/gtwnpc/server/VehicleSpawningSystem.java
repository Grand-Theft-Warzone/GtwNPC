package fr.aym.gtwnpc.server;

import com.jme3.math.Vector3f;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.dynamx.VehicleType;
import fr.aym.gtwnpc.dynamx.spawning.VehicleSpawnConfig;
import fr.aym.gtwnpc.dynamx.spawning.VehicleSpawnConfigs;
import fr.aym.gtwnpc.path.CarPathNodes;
import fr.aym.gtwnpc.path.NodeType;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import fr.aym.gtwnpc.utils.VehiclesSpawningRatios;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class VehicleSpawningSystem {
    @SubscribeEvent
    public static void tickServer(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && GtwNpcsConfig.vehiclesSpawningRules.isVehicleSpawningEnabled()) {
            Vector3fPool.openPool();
            for (EntityPlayer player : FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().playerEntities) {
                tickAroundPlayer(player);
            }
            Vector3fPool.closePool();
        }
    }

    public static PathNode findEligibleSpawnNode(World world, Vec3d around) {
        PathNode node;
        for (NodeType type : NodeType.values()) {
            if (type == NodeType.PEDESTRIAN)
                continue;
            node = CarPathNodes.getInstance().selectRandomPathNode(world, around,
                    GtwNpcsConfig.vehiclesSpawningRules.getSpawnRadiusMin(), GtwNpcsConfig.vehiclesSpawningRules.getVehicleSpawningRadius(), n -> n.getNodeType() == type && n.isValidSpawnNode(world, CarPathNodes.getInstance(), 3));
            if (node != null) {
                AxisAlignedBB bb1 = new AxisAlignedBB(node.getPosition().x, node.getPosition().y, node.getPosition().z, node.getPosition().x, node.getPosition().y, node.getPosition().z).grow(10);
                if (world.getEntitiesWithinAABB(BaseVehicleEntity.class, bb1).isEmpty())
                    return node;
            }
        }
        return null;
    }

    public static VehicleType getSpawnVehicleType(Random random, VehicleSpawnConfigs spawnConfigs, int wantedLevel) {
        VehiclesSpawningRatios ratios = GtwNpcsConfig.vehiclesSpawningRatios;
        List<VehiclesSpawningRatios.SpawnRatio> weightedRandoms = ratios.getVehicleRatios(wantedLevel);
        if (weightedRandoms == null) {
            System.out.println("Fils de pute " + weightedRandoms + " " + wantedLevel + " " + ratios.getRatios());
        }
        if(weightedRandoms.stream().anyMatch(ratio -> spawnConfigs.getVehicleSpawnConfig(random, ratio.getType()) == null)) {
            weightedRandoms = new ArrayList<>(weightedRandoms); //copy before removing elements
            weightedRandoms.removeIf(ratio -> spawnConfigs.getVehicleSpawnConfig(random, ratio.getType()) == null);
        }
        System.out.println("Ratios : " + weightedRandoms + " " + wantedLevel + " " + ratios.getRatios());
        weightedRandoms.forEach(ratio -> System.out.println("Ratio : " + ratio + " : " + spawnConfigs.getVehicleSpawnConfig(random, ratio.getType()) + " " + ratio.getType() + " " + ratio.itemWeight));
        if (weightedRandoms.isEmpty())
            return VehicleType.CIVILIAN;
        return WeightedRandom.getRandomItem(random, weightedRandoms).getType();
    }

    public static void tickAroundPlayer(EntityPlayer player) {
        World world = player.world;
        if (world.rand.nextInt(40) != 0)
            return;
        VehicleSpawnConfigs spawnConfigs = VehicleSpawnConfigs.getInstance();
        if (spawnConfigs == null || spawnConfigs.getVehicleSpawnConfigs().isEmpty())
            return;
        AxisAlignedBB bb = player.getEntityBoundingBox().grow(GtwNpcsConfig.vehiclesSpawningRules.getVehicleSpawningRadius());
        List<BaseVehicleEntity> vehiclesAround = world.getEntitiesWithinAABB(BaseVehicleEntity.class, bb, vehicle -> vehicle.hasModuleOfType(GtwNpcModule.class) && ((GtwNpcModule) vehicle.getModuleByType(GtwNpcModule.class)).hasAutopilot());
        PlayerInformation info = PlayerManager.getPlayerInformation(player.getUniqueID());
        int wantedLevel = info == null ? 0 : info.getWantedLevel();
        boolean hasPolice = wantedLevel > 0;
        int policeVehicleCount = hasPolice ? (int) vehiclesAround.stream().filter(vehicle -> ((GtwNpcModule) vehicle.getModuleByType(GtwNpcModule.class)).getVehicleType() != VehicleType.CIVILIAN).count() : 0;
        int maxVehicleCount = GtwNpcsConfig.vehiclesSpawningRules.getVehicleSpawningLimit() + (hasPolice && policeVehicleCount < 4 ? 5 : 0);
        if (vehiclesAround.size() < maxVehicleCount) {
            VehicleType type = getSpawnVehicleType(world.rand, spawnConfigs, wantedLevel);
            VehicleSpawnConfig config = spawnConfigs.getVehicleSpawnConfig(world.rand, type);
            System.out.println("Spawning " + type + " at " + player.getPositionVector() + " : " + config + " !");
            if (config == null)
                return;
            PathNode spawnNode = findEligibleSpawnNode(world, player.getPositionVector());
            if (spawnNode == null)
                return;
            System.out.println("Spawning " + config + " at " + spawnNode + " ! WtLevel: " + wantedLevel);
            int rotationYaw = spawnNode.estimateTargetRotationYaw();
            BaseVehicleEntity<?> vehicle = config.createVehicle(world, player,
                    new Vector3f(spawnNode.getPosition().x, spawnNode.getPosition().y, spawnNode.getPosition().z), rotationYaw);
            if (vehicle != null)
                world.spawnEntity(vehicle);
        } else if (vehiclesAround.size() > maxVehicleCount + 5) {
            //System.out.println("Aggressive despawning go brrrr");
            vehiclesAround.stream().filter(vehicle -> vehicle.getDistance(player) > GtwNpcsConfig.vehiclesSpawningRules.getVehicleSpawningRadius()).findAny().ifPresent(vehicle -> vehicle.setDead());
        }
    }
}

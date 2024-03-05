package fr.aym.gtwnpc.server;

import com.jme3.math.Vector3f;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.dynamx.AutopilotModule;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.dynamx.spawning.VehicleSpawnConfig;
import fr.aym.gtwnpc.dynamx.spawning.VehicleSpawnConfigs;
import fr.aym.gtwnpc.path.CarPathNodes;
import fr.aym.gtwnpc.path.NodeType;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

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

    public static void tickAroundPlayer(EntityPlayer player) {
        World world = player.world;
        if (world.rand.nextInt(40) != 0)
            return;
        if (VehicleSpawnConfigs.getInstance().getVehicleSpawnConfigs().isEmpty())
            return;
        AxisAlignedBB bb = player.getEntityBoundingBox().grow(GtwNpcsConfig.vehiclesSpawningRules.getVehicleSpawningRadius());
        List<BaseVehicleEntity> vehiclesAround = world.getEntitiesWithinAABB(BaseVehicleEntity.class, bb, vehicle -> vehicle.hasModuleOfType(GtwNpcModule.class) && ((GtwNpcModule) vehicle.getModuleByType(GtwNpcModule.class)).hasAutopilot());
        if (vehiclesAround.size() < GtwNpcsConfig.vehiclesSpawningRules.getVehicleSpawningLimit()) {
            //world.rand.nextInt(1000);
            if (true) {
                VehicleSpawnConfig config = VehicleSpawnConfigs.getInstance().getVehicleSpawnConfig(world.rand, VehicleSpawnConfig.VehicleType.CIVILIAN);
                if (config == null)
                    return;
                PathNode spawnNode = findEligibleSpawnNode(world, player.getPositionVector());
                if (spawnNode == null)
                    return;
                System.out.println("Spawning " + config + " at " + spawnNode + " !");
                int rotationYaw = spawnNode.estimateTargetRotationYaw();
                BaseVehicleEntity<?> vehicle = config.createVehicle(world, player,
                        new Vector3f(spawnNode.getPosition().x, spawnNode.getPosition().y, spawnNode.getPosition().z), rotationYaw);
                vehicle.setInitCallback((entity, modules) -> {
                    entity.getModuleByType(GtwNpcModule.class).enableAutopilot();
                });
                world.spawnEntity(vehicle);
            }
        } else if (vehiclesAround.size() > GtwNpcsConfig.vehiclesSpawningRules.getVehicleSpawningLimit() + 5) {
            System.out.println("Aggressive despawning go brrrr");
            vehiclesAround.stream().filter(vehicle -> vehicle.getDistance(player) > GtwNpcsConfig.vehiclesSpawningRules.getVehicleSpawningRadius()).findAny().ifPresent(vehicle -> vehicle.setDead());
        }
    }
}

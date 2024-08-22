package fr.aym.gtwnpc.common;

import fr.aym.dynamxgarageaddon.api.event.GarageEvent;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.dynamx.VehicleType;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.network.BBMessagePathNodes;
import fr.aym.gtwnpc.path.CarPathNodes;
import fr.aym.gtwnpc.path.NodeType;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.sqript.EventGNpcInit;
import fr.aym.gtwnpc.sqript.EventOnPlayerAttack2;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.nico.sqript.ScriptManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class CommonEventHandler {
    @SubscribeEvent
    public static void onConnected(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) 20) { // Wait for world load on client side
            @Override
            public void run() {
                GtwNpcMod.network.sendTo(new BBMessagePathNodes(NodeType.PEDESTRIAN, PedestrianPathNodes.getInstance().getNodes()), (EntityPlayerMP) event.player);
                GtwNpcMod.network.sendTo(new BBMessagePathNodes(NodeType.CAR_CITY, CarPathNodes.getInstance().getNodes()), (EntityPlayerMP) event.player);
            }
        });
    }

    @SubscribeEvent
    public static void onSpawnCheck(EntityJoinWorldEvent event) {
        // System.out.println("Brutal " + event.getEntity());
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityGtwNpc && ScriptManager.callEvent(new EventGNpcInit((EntityGtwNpc) event.getEntity()))) {
            System.out.println("Event cancelled the spawn");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void livingHurt(LivingAttackEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer && ScriptManager.callEvent(new EventOnPlayerAttack2(event.getEntity(), (EntityPlayer) event.getSource().getTrueSource()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void spawnCar(PhysicsEntityEvent.CreateModules<BaseVehicleEntity> event) {
        if (event.getEntity().hasModuleOfType(CarEngineModule.class)) {
            CarEngineModule module = (CarEngineModule) event.getEntity().getModuleByType(CarEngineModule.class);
            event.getModuleList().removeIf(m -> m instanceof CarEngineModule);
            event.getModuleList().add(new GtwNpcModule((BaseVehicleEntity<?>) event.getEntity(), module));
            // System.out.println("Modules: " + event.getModuleList());
        }
    }

    @SubscribeEvent
    public static void onPlayerInteractWithVehicle(VehicleEntityEvent.PlayerInteract event) {
        if (!event.getEntity().hasModuleOfType(GtwNpcModule.class)) {
            return;
        }
        GtwNpcModule autopilot = event.getEntity().getModuleByType(GtwNpcModule.class);
        if (autopilot.hasAutopilot() && autopilot.getStolenTime() == 0) {
            event.setCanceled(!autopilot.isStealable());
        }
    }

    @SubscribeEvent
    public static void onEntityMountVehicle(VehicleEntityEvent.EntityMount event) {
        if (event.getEntity().world.isRemote || !event.getEntity().hasModuleOfType(GtwNpcModule.class)) {
            return;
        }
        GtwNpcModule autopilot = event.getEntity().getModuleByType(GtwNpcModule.class);
        if (!autopilot.hasAutopilot() || autopilot.getStolenTime() != 0 || !autopilot.isStealable()) {
            return;
        }
        autopilot.stealVehicle("stolen");
        autopilot.dismountNpcPassengers(event.getEntityMounted().rotationYaw + 180);
        if (!(event.getEntityMounted() instanceof EntityPlayer)) {
            return;
        }
        if (autopilot.getVehicleType() == VehicleType.CIVILIAN && event.getEntity().world.rand.nextBoolean()) {
            return;
        }
        PlayerInformation info = PlayerManager.getPlayerInformation((EntityPlayer) event.getEntityMounted());
        info.setWantedLevel(info.getWantedLevel() + 1);
        event.getEntityMounted().sendMessage(new TextComponentString("You stole a vehicle, your wanted level is now " + info.getWantedLevel()));
    }

    @SubscribeEvent
    public static void onPhysicsCollision(PhysicsEvent.PhysicsCollision event) {
        if (!(event.getObject1().getObjectIn() instanceof BaseVehicleEntity<?>) || !(event.getObject2().getObjectIn() instanceof BaseVehicleEntity<?>)) {
            return;
        }
        BaseVehicleEntity<?> vehicle1 = (BaseVehicleEntity<?>) event.getObject1().getObjectIn();
        BaseVehicleEntity<?> vehicle2 = (BaseVehicleEntity<?>) event.getObject2().getObjectIn();
        if (vehicle1.hasModuleOfType(GtwNpcModule.class) && vehicle2.getControllingPassenger() instanceof EntityPlayer) {
            GtwNpcModule module = vehicle1.getModuleByType(GtwNpcModule.class);
            if (module.getVehicleType() != VehicleType.CIVILIAN && module.getAutopilotModule() != null && module.getStolenTime() == 0) {
                PlayerInformation info = PlayerManager.getPlayerInformation((EntityPlayer) vehicle2.getControllingPassenger());
                if (!info.getCollidedVehicles().containsKey(vehicle1)) {
                    info.getCollidedVehicles().put(vehicle1, vehicle1.ticksExisted);
                    info.setWantedLevel(info.getWantedLevel() + 1);
                    vehicle2.getControllingPassenger().sendMessage(new TextComponentString("You collided with a police vehicle, your wanted level is now " + info.getWantedLevel()));
                }
            }
        } else if (vehicle2.hasModuleOfType(GtwNpcModule.class) && vehicle1.getControllingPassenger() instanceof EntityPlayer) {
            GtwNpcModule module = vehicle2.getModuleByType(GtwNpcModule.class);
            if (module.getVehicleType() != VehicleType.CIVILIAN && module.getAutopilotModule() != null && module.getStolenTime() == 0) {
                PlayerInformation info = PlayerManager.getPlayerInformation((EntityPlayer) vehicle1.getControllingPassenger());
                if (!info.getCollidedVehicles().containsKey(vehicle2)) {
                    info.getCollidedVehicles().put(vehicle2, vehicle2.ticksExisted);
                    info.setWantedLevel(info.getWantedLevel() + 1);
                    vehicle1.getControllingPassenger().sendMessage(new TextComponentString("You collided with a police vehicle, your wanted level is now " + info.getWantedLevel()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void putVehicleInGarage(GarageEvent.PutVehicleInside event) {
        if (event.getVehicle().hasModuleOfType(GtwNpcModule.class)) {
            GtwNpcModule module = event.getVehicle().getModuleByType(GtwNpcModule.class);
            if (module.hasAutopilot())
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void getInWorldVehicles(GarageEvent.ListInWorldVehicles listInWorldVehicles) {
        listInWorldVehicles.getInWorldVehicles().removeIf(vehicle -> vehicle.hasModuleOfType(GtwNpcModule.class)
                && vehicle.getModuleByType(GtwNpcModule.class).hasAutopilot());
    }
}

package fr.aym.gtwnpc.common;

import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.client.skin.SkinRepository;
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
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.common.core.DismountHelper;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
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
        //System.out.println("Sending nodes to " + event.player.getName());
        GtwNpcMod.network.sendTo(new BBMessagePathNodes(NodeType.PEDESTRIAN, PedestrianPathNodes.getInstance().getNodes()), (EntityPlayerMP) event.player);
        GtwNpcMod.network.sendTo(new BBMessagePathNodes(NodeType.CAR_CITY, CarPathNodes.getInstance().getNodes()), (EntityPlayerMP) event.player);
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
    public static void onPlayerInteractWithVehicle(VehicleEntityEvent.EntityMount event) {
        if (!event.getEntity().hasModuleOfType(GtwNpcModule.class)) {
            return;
        }
        GtwNpcModule autopilot = event.getEntity().getModuleByType(GtwNpcModule.class);
        if (!autopilot.hasAutopilot() || autopilot.getAutopilotModule().getStolenTime() != 0) {
            return;
        }
        autopilot.getAutopilotModule().stealVehicle();
        if (event.getEntity().world.isRemote) {
            return;
        }
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
}

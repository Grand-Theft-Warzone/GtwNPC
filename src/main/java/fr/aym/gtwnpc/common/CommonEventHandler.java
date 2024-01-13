package fr.aym.gtwnpc.common;

import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.entity.EntityNpcTypes;
import fr.aym.gtwnpc.network.BBMessagePathNodes;
import fr.aym.gtwnpc.path.NodeType;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.sqript.EventGNpcInit;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.nico.sqript.ScriptManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class CommonEventHandler {
    @SubscribeEvent
    public static void onConnected(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        //System.out.println("Sending nodes to " + event.player.getName());
        GtwNpcMod.network.sendTo(new BBMessagePathNodes(NodeType.PEDESTRIAN, PedestrianPathNodes.getInstance().getNodes()), (EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public static void onSpawnListing(WorldEvent.PotentialSpawns event) {
        if (event.getType().getCreatureClass() == EntityGtwNpc.class) {
            Vec3d pos = new Vec3d(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
            if(event.getType() == EntityNpcTypes.getNpcType()) {
                event.getList().removeIf(e -> {
                    boolean deny = event.getWorld().rand.nextInt(100) >= 25 || PedestrianPathNodes.getInstance().getNodes().stream().noneMatch(node -> node.getDistance(pos) < 37);
                    //System.out.println("Checking spawn at " + pos);
                    return deny;
                });
            } else if(event.getType() == EntityNpcTypes.getPoliceNpcType()) {
                event.getList().removeIf(e -> {
                    boolean deny = PlayerManager.getPlayerInfos().values().stream().noneMatch(info -> info.getWantedLevel() > 0 && info.getPlayerIn().getPositionVector().distanceTo(pos) < 50);
                    //System.out.println("Checking spawn at " + pos);
                    System.out.println("Spawn police: " + deny);
                    return deny;
                });
            }
        }
    }

    @SubscribeEvent
    public static void onSpawnCheck0(LivingSpawnEvent.CheckSpawn event) {
        //System.out.println("Checking spawn at " + event.getEntity().getPositionVector());
        if(event.getEntity() instanceof EntityGtwNpc) {
            event.getSpawner().
        }
    }

    @SubscribeEvent
    public static void onSpawnCheck(EntityJoinWorldEvent event) {
       // System.out.println("Brutal " + event.getEntity());
        if(!event.getWorld().isRemote && event.getEntity() instanceof EntityGtwNpc && ScriptManager.callEvent(new EventGNpcInit((EntityGtwNpc) event.getEntity()))) {
            System.out.println("Event cancelled the spawn");
            event.setCanceled(true);
        }
    }
}

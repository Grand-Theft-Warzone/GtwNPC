package fr.aym.gtwnpc.common;

import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.network.BBMessagePathNodes;
import fr.aym.gtwnpc.path.NodeType;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import fr.aym.gtwnpc.sqript.EventGNpcInit;
import fr.aym.gtwnpc.sqript.EventOnPlayerAttack2;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.nico.sqript.ScriptManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class CommonEventHandler {
    private static Vec3d nextPolicePos;

    @SubscribeEvent
    public static void onConnected(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        //System.out.println("Sending nodes to " + event.player.getName());
        GtwNpcMod.network.sendTo(new BBMessagePathNodes(NodeType.PEDESTRIAN, PedestrianPathNodes.getInstance().getNodes()), (EntityPlayerMP) event.player);
    }

    /*@SubscribeEvent
    public static void onSpawnListing(WorldEvent.PotentialSpawns event) {
        if (event.getType().getCreatureClass() == EntityGtwNpc.class || event.getType().getCreatureClass() == EntityGtwPoliceNpc.class) {
            Vec3d pos = new Vec3d(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
            event.getList().removeIf(e -> {
                if(e.entityClass == EntityGtwPoliceNpc.class) {
                    boolean deny = PlayerManager.getPlayerInfos().values().stream().noneMatch(info -> info.getWantedLevel() > 0 && info.getPlayerIn().getPositionVector().distanceTo(pos) < 50);
                    //System.out.println("Checking spawn at " + pos);
                    System.out.println("Spawn police: " + deny);
                    return deny;
                }
                boolean deny = event.getWorld().rand.nextInt(100) >= 25 || PedestrianPathNodes.getInstance().getNodes().stream().noneMatch(node -> node.getDistance(pos) < 37);
                //System.out.println("Checking spawn at " + pos);
                int radius = 40;
                AxisAlignedBB bb = new AxisAlignedBB(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius, pos.z + radius);
                List<EntityGtwNpc> npcs = event.getWorld().getEntitiesWithinAABB(EntityGtwNpc.class, bb);
                deny = deny || npcs.size() > 10;
                return deny;
            });
        }
    }*/

    @SubscribeEvent
    public static void onSpawnCheck(EntityJoinWorldEvent event) {
       // System.out.println("Brutal " + event.getEntity());
        if(!event.getWorld().isRemote && event.getEntity() instanceof EntityGtwNpc && ScriptManager.callEvent(new EventGNpcInit((EntityGtwNpc) event.getEntity()))) {
            System.out.println("Event cancelled the spawn");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void livingHurt(LivingAttackEvent event) {
        if(event.getSource().getTrueSource() instanceof EntityPlayer && ScriptManager.callEvent(new EventOnPlayerAttack2(event.getEntity(), (EntityPlayer) event.getSource().getTrueSource()))) {
            event.setCanceled(true);
        }
    }
}

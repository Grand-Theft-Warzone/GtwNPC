package fr.aym.gtwnpc.common;

import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.network.BBMessagePathNodes;
import fr.aym.gtwnpc.path.NodeType;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class CommonEventHandler {
    @SubscribeEvent
    public static void onConnected(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        //System.out.println("Sending nodes to " + event.player.getName());
        GtwNpcMod.network.sendTo(new BBMessagePathNodes(NodeType.PEDESTRIAN, PedestrianPathNodes.getInstance().getNodes()), (EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public static void onSpawnCheck(WorldEvent.PotentialSpawns event) {
        if (event.getType().getCreatureClass() == EntityGtwNpc.class) {
            Vec3d pos = new Vec3d(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
            event.getList().removeIf(e -> {
                boolean deny = event.getWorld().rand.nextInt(4) >= 1 || PedestrianPathNodes.getInstance().getNodes().stream().noneMatch(node -> node.getDistance(pos) < 37);
                //System.out.println("Checking spawn at " + pos);
                return deny;
            });
        }
    }
}

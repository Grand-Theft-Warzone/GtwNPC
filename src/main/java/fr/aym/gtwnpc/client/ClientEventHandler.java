package fr.aym.gtwnpc.client;

import fr.aym.gtwnpc.client.render.NodesRenderer;
import fr.aym.gtwnpc.common.GtwNpcsItems;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.aym.gtwnpc.utils.GtwNpcsUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.vecmath.Vector3f;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID, value = Side.CLIENT)
public class ClientEventHandler {
    public static final Minecraft MC = Minecraft.getMinecraft();

    @SubscribeEvent
    public static void onMouseClick(MouseEvent event) {
        if (MC.player != null && MC.player.getHeldItemMainhand().getItem() == GtwNpcsItems.itemNodes) {
            PathNode pointedNode = GtwNpcsUtils.rayTracePathNode(MC.player, 0);
            if (pointedNode != null) {
                event.setCanceled(true);
                if (event.getButton() == 0) {
                    if (event.isButtonstate()) {
                        if (NodesRenderer.selectedNode != null && NodesRenderer.selectedNode != pointedNode && !MC.player.isSneaking()) {
                            if (NodesRenderer.selectedNode.getNeighbors(PedestrianPathNodes.getInstance()).contains(pointedNode))
                                NodesRenderer.selectedNode.removeNeighbor(PedestrianPathNodes.getInstance(), pointedNode, true);
                        } else if (MC.player.isSneaking()) {
                            pointedNode.delete(PedestrianPathNodes.getInstance(), true);
                            if (NodesRenderer.selectedNode == pointedNode)
                                NodesRenderer.selectedNode = null;
                        }
                    }
                } else if (event.getButton() == 1) {
                    if (event.isButtonstate()) {
                        if (NodesRenderer.selectedNode != null && NodesRenderer.selectedNode != pointedNode && !MC.player.isSneaking()) {
                            if (!NodesRenderer.selectedNode.getNeighbors(PedestrianPathNodes.getInstance()).contains(pointedNode)) {
                                NodesRenderer.selectedNode.addNeighbor(PedestrianPathNodes.getInstance(), pointedNode, true);
                                NodesRenderer.selectedNode = pointedNode; //chaining
                            }
                        } else if (MC.player.isSneaking()) {
                            NodesRenderer.selectedNode = pointedNode;
                        }
                    }
                }
            } else if (NodesRenderer.selectedNode != null && MC.player.isSneaking() && event.getButton() == 0 && event.isButtonstate()) {
                event.setCanceled(true);
                NodesRenderer.selectedNode = null;
            } else if (MC.player.isSneaking() && event.getButton() == 1 && event.isButtonstate()) {
                event.setCanceled(true);
                RayTraceResult result = MC.objectMouseOver;
                if (result == null)
                    return;
                PathNode node = new PathNode(new Vector3f((float) result.hitVec.x, (float) result.hitVec.y + 0.5f, (float) result.hitVec.z), new ArrayList<>());
                node.create(PedestrianPathNodes.getInstance(), true);
            }
        }
    }
}

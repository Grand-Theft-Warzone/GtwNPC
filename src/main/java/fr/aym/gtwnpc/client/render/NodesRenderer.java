package fr.aym.gtwnpc.client.render;

import fr.aym.gtwnpc.common.GtwNpcsItems;
import fr.aym.gtwnpc.entity.ai.EntityAIMoveToNodes;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.aym.gtwnpc.utils.GtwNpcsUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;

import javax.vecmath.Vector3f;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID, value = Side.CLIENT)
public class NodesRenderer {
    public static final Minecraft MC = Minecraft.getMinecraft();

    public static PathNode selectedNode;

    @SubscribeEvent
    public static void renderWorldLast(RenderWorldLastEvent event) {
        if (MC.player != null && MC.player.getHeldItemMainhand().getItem() == GtwNpcsItems.itemNodes) {
            Entity e = MC.getRenderViewEntity();
            Collection<PathNode> nodes = PedestrianPathNodes.getInstance().getNodes();
            PathNode pointedNode = GtwNpcsUtils.rayTracePathNode(MC.player, event.getPartialTicks());
            //GlStateManager.disableDepth();
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.translate(-(e.prevPosX + (e.posX - e.prevPosX) * event.getPartialTicks()), -(e.prevPosY + (e.posY - e.prevPosY) * event.getPartialTicks()), -(e.prevPosZ + (e.posZ - e.prevPosZ) * event.getPartialTicks()));
            Sphere sphere = new Sphere();
            Set<UUID> renderedNodes = new HashSet<>();
            NodeColor color;
            byte state = 0;
            if (selectedNode != null && pointedNode != null) {
                if (selectedNode.getNeighbors(PedestrianPathNodes.getInstance()).contains(pointedNode))
                    state = -1;
                else
                    state = 1;
            }
            for (PathNode node : nodes) {
                Vector3f center = node.getPosition();
                if (e.getDistance(center.x, center.y, center.z) > 100)
                    continue;
                GlStateManager.pushMatrix();
                // render lines to next nodes
                for (PathNode neighbor : node.getNeighbors(PedestrianPathNodes.getInstance())) {
                    if (!renderedNodes.contains(neighbor.getId())) {
                        Vector3f neighborCenter = neighbor.getPosition();
                        if ((node == selectedNode && neighbor == pointedNode) || (node == pointedNode && neighbor == selectedNode)) {
                            color = state == 1 ? NodeColor.LINK_LINKING : NodeColor.LINK_POINTED;
                            color.apply();
                        } else if (node == pointedNode || neighbor == pointedNode || node == selectedNode || neighbor == selectedNode) {
                            NodeColor.LINK_ACTIVE.apply();
                        } else {
                            NodeColor.LINK_IDLE.apply();
                        }
                        GlStateManager.glBegin(GL11.GL_LINES);
                        GlStateManager.glVertex3f(center.x, center.y, center.z);
                        GlStateManager.glVertex3f(neighborCenter.x, neighborCenter.y, neighborCenter.z);
                        GlStateManager.glEnd();
                    }
                }
                if (node == selectedNode) {
                    color = state == -1 ? NodeColor.POINTED_UNLINKING : NodeColor.POINTED_LINKING;
                    if (pointedNode == null) {
                        NodeColor.LINK_LINKING.apply();
                        GlStateManager.glBegin(GL11.GL_LINES);
                        GlStateManager.glVertex3f(center.x, center.y, center.z);
                        Vec3d eyes = e.getPositionEyes(event.getPartialTicks());
                        GlStateManager.glVertex3f((float) eyes.x, (float) eyes.y - 0.1f, (float) eyes.z);
                        GlStateManager.glEnd();
                    } else if(state == 1) {
                        NodeColor.LINK_LINKING.apply();
                        GlStateManager.glBegin(GL11.GL_LINES);
                        GlStateManager.glVertex3f(center.x, center.y, center.z);
                        GlStateManager.glVertex3f(pointedNode.getPosition().x, pointedNode.getPosition().y, pointedNode.getPosition().z);
                        GlStateManager.glEnd();
                    }
                    color.apply();
                } else if (node == pointedNode) {
                    color = state == 1 ? NodeColor.POINTED_LINKING : state == -1 ? NodeColor.POINTED_UNLINKING : NodeColor.POINTED;
                    color.apply();
                } else {
                    NodeColor.IDLE.apply();
                }
                GlStateManager.translate(center.x, center.y, center.z);
                if(EntityAIMoveToNodes.BIG_TARGET != null && node.getId().equals(EntityAIMoveToNodes.BIG_TARGET.getId())) {
                    NodeColor.POINTED_LINKING.apply();
                } else if(EntityAIMoveToNodes.INTERMEDIATE_TARGET != null && node.getId().equals(EntityAIMoveToNodes.INTERMEDIATE_TARGET.getId())) {
                    NodeColor.POINTED_UNLINKING.apply();
                }
                sphere.draw(0.9f, 30, 30);
                GlStateManager.popMatrix();
                renderedNodes.add(node.getId());
            }
            if(pointedNode == null && MC.player.isSneaking() && MC.objectMouseOver != null) {
                //draw pointed node
                NodeColor.POINTED_LINKING.apply();
                Vec3d hit = MC.objectMouseOver.hitVec;
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) hit.x, (float) hit.y + 0.5f, (float) hit.z);
                sphere.draw(0.9f, 30, 30);
                GlStateManager.popMatrix();

            }
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    public enum NodeColor {
        IDLE(1, 1, 1, 0.5f),
        POINTED(0f, 0.3f, 1, 0.7f),
        POINTED_LINKING(0, 1, 0, 0.7f),
        POINTED_UNLINKING(1, 0, 0, 0.7f),
        LINK_IDLE(0.5f, 0.5f, 0.5f, 0.5f),
        LINK_ACTIVE(0.5f, 0.5f, 1, 1),
        LINK_LINKING(0, 1, 0, 1),
        LINK_POINTED(1, 0, 0, 1);

        private final float r, g, b, a;

        NodeColor(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public void apply() {
            GlStateManager.color(r, g, b, a);
        }
    }
}

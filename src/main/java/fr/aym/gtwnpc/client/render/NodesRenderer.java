package fr.aym.gtwnpc.client.render;

import fr.aym.gtwnpc.common.GtwNpcsItems;
import fr.aym.gtwnpc.entity.ai.GEntityAIMoveToNodes;
import fr.aym.gtwnpc.path.*;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.aym.gtwnpc.utils.GtwNpcsUtils;
import fr.dynamx.client.renders.mesh.shapes.ArrowMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
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

    public static ArrowMesh arrowMesh;

    public static PathNode selectedNode;

    @SubscribeEvent
    public static void renderWorldLast(RenderWorldLastEvent event) {
        if (MC.player != null && (MC.player.getHeldItemMainhand().getItem() == GtwNpcsItems.itemNodes || MC.player.getHeldItemOffhand().getItem() == GtwNpcsItems.itemNodes)) {
            Entity e = MC.getRenderViewEntity();
            ItemStack stack = MC.player.getHeldItemMainhand();
            if(stack.getItem() != GtwNpcsItems.itemNodes)
                stack = MC.player.getHeldItemOffhand();
            PathNodesManager manager = PedestrianPathNodes.getInstance();
            if (stack.hasTagCompound() && stack.getTagCompound().getInteger("mode") != 0) {
                manager = CarPathNodes.getInstance();
            }
            Collection<PathNode> nodes = manager.getNodes();
            PathNode pointedNode = GtwNpcsUtils.rayTracePathNode(manager, MC.player, event.getPartialTicks());
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
                if (selectedNode.getNeighbors(manager).contains(pointedNode))
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
                for (PathNode neighbor : node.getNeighbors(manager)) {
                    if (!renderedNodes.contains(neighbor.getId()) || manager.getNodeType().areOneWayNodes()) {
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
                        if (manager.getNodeType().areOneWayNodes()) {
                            //if(arrowMesh == null)
                            arrowMesh = new ArrowMesh(0.6F, 0.46F, 0.11F);
                            GlStateManager.pushMatrix();
                            GlStateManager.glLineWidth(14);
                            GlStateManager.translate(center.x, center.y, center.z);
                            // horizontal rotation
                            GlStateManager.rotate((float) -Math.toDegrees(Math.atan2(neighborCenter.z - center.z, neighborCenter.x - center.x)) + 90, 0, 1, 0);
                            // vertical rotation
                            GlStateManager.rotate((float) -Math.toDegrees(Math.atan2(neighborCenter.y - center.y, Math.sqrt((neighborCenter.x - center.x) * (neighborCenter.x - center.x) + (neighborCenter.z - center.z) * (neighborCenter.z - center.z)))), 1, 0, 0);
                            GlStateManager.translate(0, 0, 1);
                            GlStateManager.scale(2, 2, 2);
                            GlStateManager.color(0, 1, 0, 1);
                            arrowMesh.render();
                            GlStateManager.scale(0.5, 0.5, 0.5);
                            double dist = node.getDistance(neighbor) - 2;
                            if (dist > 0) {
                                //System.out.println(System.currentTimeMillis() +" " + System.currentTimeMillis() % 1000f);
                                float anim = (float) (((System.currentTimeMillis() % 10000d) / 10000d) * dist);
                                GlStateManager.translate(0, 0, anim);
                                GlStateManager.scale(2, 2, 2);
                                GlStateManager.color(0.5f, 0, 0.5f, 1);
                                arrowMesh.render();
                            }
                            GlStateManager.popMatrix();
                            GlStateManager.glLineWidth(1.0F);
                        }
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
                    } else if (state == 1) {
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
                } else if (node instanceof TrafficLightNode) {
                    switch (((TrafficLightNode) node).getTrafficLightState(MC.world)) {
                        case 0:
                            NodeColor.TR_LIGHT_ORANGE.apply();
                            break;
                        case 1:
                            NodeColor.TR_LIGHT_GREEN.apply();
                            break;
                        case 2:
                            NodeColor.TR_LIGHT_RED.apply();
                            break;
                        default:
                            NodeColor.TR_LIGHT_ERRORED.apply();
                            break;
                    }
                    BlockPos pos = ((TrafficLightNode) node).getTrafficLightPos();
                    if (pos.getY() != 1080) {
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
                        sphere.draw(0.4f, 30, 30);
                        GlStateManager.popMatrix();

                        GlStateManager.glBegin(GL11.GL_LINES);
                        GlStateManager.glVertex3f(center.x, center.y, center.z);
                        GlStateManager.glVertex3f(pos.getX(), pos.getY(), pos.getZ());
                        GlStateManager.glEnd();
                    }
                } else if (node instanceof SeatNode) {
                    NodeColor.IDLE_SEAT.apply();
                } else {
                    switch (node.getNodeType()) {
                        case CAR_CITY_LOW_SPED:
                            NodeColor.IDLE_30.apply();
                            break;
                        case CAR_HIGHWAY:
                            NodeColor.IDLE_90.apply();
                            break;
                        case CAR_OFFROAD:
                            NodeColor.IDLE_70.apply();
                            break;
                        default:
                            NodeColor.IDLE.apply();
                            break;
                    }
                }
                GlStateManager.translate(center.x, center.y, center.z);
                if (GEntityAIMoveToNodes.BIG_TARGET != null && node.getId().equals(GEntityAIMoveToNodes.BIG_TARGET.getId())) {
                    NodeColor.IDLE_90.apply();
                } else if (GEntityAIMoveToNodes.INTERMEDIATE_TARGET != null && node.getId().equals(GEntityAIMoveToNodes.INTERMEDIATE_TARGET.getId())) {
                    NodeColor.IDLE_70.apply();
                }
                sphere.draw(0.9f, 30, 30);
                GlStateManager.popMatrix();
                renderedNodes.add(node.getId());
            }
            if (pointedNode == null && MC.player.isSneaking() && MC.objectMouseOver != null) {
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
        IDLE_30(0.5f, 1f, 1f, 0.5f),
        IDLE_70(1f, 1f, 0.5f, 0.5f),
        IDLE_90(1f, 0.5f, 1f, 0.5f),
        IDLE_SEAT(1, 0.8f, 0.8f, 0.5f),
        TR_LIGHT_ERRORED(1, 0.5f, 1, 0.5f),
        TR_LIGHT_RED(1, 0, 0, 0.5f),
        TR_LIGHT_ORANGE(1, 0.5f, 0, 0.5f),
        TR_LIGHT_GREEN(0, 1, 0, 0.5f),
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

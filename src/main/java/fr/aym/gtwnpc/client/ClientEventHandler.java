package fr.aym.gtwnpc.client;

import com.mia.props.common.entities.TileMountable;
import fr.aym.gtwmap.api.GtwMapApi;
import fr.aym.gtwmap.api.ITrackableObject;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.block.TETrafficLight;
import fr.aym.gtwnpc.client.render.NodesRenderer;
import fr.aym.gtwnpc.client.render.NpcSeatNode;
import fr.aym.gtwnpc.client.render.NpcSeatsRoot;
import fr.aym.gtwnpc.common.GtwNpcsItems;
import fr.aym.gtwnpc.dynamx.AutopilotModule;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.dynamx.IObstacleDetection;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.item.ItemNodes;
import fr.aym.gtwnpc.network.CSMessageSetNodeMode;
import fr.aym.gtwnpc.path.*;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.AIRaycast;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.aym.gtwnpc.utils.GtwNpcsUtils;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.api.events.client.BuildSceneGraphEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector3f;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID, value = Side.CLIENT)
public class ClientEventHandler {
    public static final Minecraft MC = Minecraft.getMinecraft();

    @SubscribeEvent
    public static void onMouseClick(MouseEvent event) {
        if (MC.player != null && MC.player.getHeldItemMainhand().getItem() == GtwNpcsItems.itemNodes) {
            ItemStack stack = MC.player.getHeldItemMainhand();
            PathNodesManager manager = PedestrianPathNodes.getInstance();
            if (stack.hasTagCompound() && stack.getTagCompound().getInteger("mode") != 0) {
                manager = CarPathNodes.getInstance();
            }
            PathNode pointedNode = GtwNpcsUtils.rayTracePathNode(manager, MC.player, 0);
            if (pointedNode != null) {
                event.setCanceled(true);
                if (event.getButton() == 0) {
                    if (event.isButtonstate()) {
                        if (NodesRenderer.selectedNode != null && NodesRenderer.selectedNode != pointedNode && !MC.player.isSneaking()) {
                            if (NodesRenderer.selectedNode.getNeighbors(manager).contains(pointedNode))
                                NodesRenderer.selectedNode.removeNeighbor(manager, pointedNode, true);
                        } else if (MC.player.isSneaking()) {
                            pointedNode.delete(manager, true);
                            if (NodesRenderer.selectedNode == pointedNode)
                                NodesRenderer.selectedNode = null;
                        }
                    }
                } else if (event.getButton() == 1) {
                    if (event.isButtonstate()) {
                        if (NodesRenderer.selectedNode != null && NodesRenderer.selectedNode != pointedNode && !MC.player.isSneaking()) {
                            if (!NodesRenderer.selectedNode.getNeighbors(manager).contains(pointedNode)) {
                                NodesRenderer.selectedNode.addNeighbor(manager, pointedNode, true);
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
                if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK)
                    return;
                PathNode node;
                TileEntity te = MC.world.getTileEntity(result.getBlockPos());
                NodeType type = NodeType.values()[(!stack.hasTagCompound() ? 0 : stack.getTagCompound().getInteger("mode")) + 1];
                if (te instanceof TETrafficLight) {
                    if (NodesRenderer.selectedNode == null)
                        return;
                    node = new TrafficLightNode(result.getBlockPos(), NodesRenderer.selectedNode, type);
                } else if (te instanceof TileMountable && type == NodeType.PEDESTRIAN) {
                    node = new SeatNode(new Vector3f((float) result.hitVec.x, (float) result.hitVec.y + 0.5f, (float) result.hitVec.z), new HashSet<>(), result.getBlockPos(), MC.player.rotationYaw);
                } else {
                    node = new PathNode(new Vector3f((float) result.hitVec.x, (float) result.hitVec.y + 0.5f, (float) result.hitVec.z), new HashSet<>(), type);
                }
                node.create(manager, true);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseEvent(MouseEvent event) {
        if (MC.player != null && MC.player.isSneaking() && MC.player.getHeldItemMainhand().getItem() instanceof ItemNodes && Mouse.getEventDWheel() != 0) {
            GtwNpcMod.network.sendToServer(new CSMessageSetNodeMode(-15815));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void initRenderer(PhysicsEntityEvent.InitRenderer event) {
        if (event.getRenderer() instanceof RenderBaseVehicle) {
            System.out.println("Adding debug renderers");
            event.addDebugRenderers(new DebugRenderer<BaseVehicleEntity<?>>() {
                @Override
                public boolean shouldRender(BaseVehicleEntity<?> physicsEntity) {
                    return physicsEntity.hasModuleOfType(GtwNpcModule.class) && physicsEntity.getModuleByType(GtwNpcModule.class).hasAutopilot();
                }

                @Override
                public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderPhysicsEntity, double x, double y, double z, float partialTicks) {
                    GlStateManager.pushMatrix();
                    x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
                    y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
                    z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
                    GlStateManager.translate(-x, -y, -z);

                    AutopilotModule mod = entity.getModuleByType(GtwNpcModule.class).getAutopilotModule();
                    IObstacleDetection detection = mod.getObstacleDetection();
                    float mySpeed = entity.getPhysicsHandler() != null && entity.ticksExisted > 10 ? entity.getPhysicsHandler().getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : 0;
                    int rayDistance = mySpeed > 35 ? 16 : mySpeed > 20 ? 9 : mySpeed > 5 ? 7 : mySpeed > 2 ? 4 : 3;

                    AxisAlignedBB front = detection.getDetectionAABB(rayDistance);
                    RenderGlobal.drawSelectionBoundingBox(front, 0, 1, 0, 1);

                    //com.jme3.math.Vector3f rayOrigin = new com.jme3.math.Vector3f((float) entity.posX, (float) entity.posY, (float) entity.posZ);
                    //List<AIRaycast> rayVecs = detection.createRayVectors(rayDistance * 2);
                    List<AIRaycast> rayVecs = detection.getLastVectors();
                    if (rayVecs != null) {
                        for (AIRaycast raycast : rayVecs) {
                            //System.out.println("Raycast " + raycast.getRayVec() + " " + raycast.getOrigin() + " " + raycast.lastVehicleHit + " " + raycast.lastEntityHit);
                            com.jme3.math.Vector3f vec = raycast.getRayVec();
                            GlStateManager.glBegin(GL11.GL_LINES);
                            GlStateManager.color(1, 0, 0, 1);
                            GlStateManager.glVertex3f(raycast.getOrigin().x, raycast.getOrigin().y, raycast.getOrigin().z);
                            GlStateManager.glVertex3f(vec.x, vec.y, vec.z);
                            GlStateManager.glEnd();

                        /*if (mod.getForcedSteeringTime() > 0) {
                            vec = vec.subtract(rayOrigin);
                            Quaternion q = QuaternionPool.get();
                            q.fromAngles(0, -(mod.getForcedSteering() - mod.getSteerForce()) * 0.3f, 0);
                            vec = DynamXGeometry.rotateVectorByQuaternion(vec, q);
                            vec.addLocal(rayOrigin);
                            GlStateManager.glBegin(GL11.GL_LINES);
                            GlStateManager.color(0, 0, 1, 1);
                            GlStateManager.glVertex3f(raycast.getOrigin().x, raycast.getOrigin().y, raycast.getOrigin().z);
                            GlStateManager.glVertex3f(vec.x, vec.y, vec.z);
                            GlStateManager.glEnd();
                        }*/

                            if (raycast.lastVehicleHit != null) {
                                RenderGlobal.drawBoundingBox(raycast.lastVehicleHit.x - 0.05f, raycast.lastVehicleHit.y - 0.05f, raycast.lastVehicleHit.z - 0.05f,
                                        raycast.lastVehicleHit.x + 0.05f, raycast.lastVehicleHit.y + 0.05f, raycast.lastVehicleHit.z + 0.05f,
                                        1, 1, 0, 1);
                            }

                            if (raycast.lastEntityHit != null) {
                                RenderGlobal.drawBoundingBox(raycast.lastEntityHit.x - 0.05f, raycast.lastEntityHit.y - 0.05f, raycast.lastEntityHit.z - 0.05f,
                                        raycast.lastEntityHit.x + 0.05f, raycast.lastEntityHit.y + 0.05f, raycast.lastEntityHit.z + 0.05f,
                                        1, 0, 1, 1);
                            }
                        }
                    }

                    detection.getEntityOOBB(entity).drawOOBB(0, 0, 1, 1);

                    Vector3f c1 = new Vector3f(-86.05212f, 4.89501f, -1261.2246f);
                    RenderGlobal.drawBoundingBox(c1.x - 0.05f, c1.y - 0.05f, c1.z - 0.05f,
                            c1.x + 0.05f, c1.y + 0.05f, c1.z + 0.05f,
                            1, 1, 1, 1);
                    c1 = new Vector3f(-88.2944f, 4.881034f, -1264.537f);
                    RenderGlobal.drawBoundingBox(c1.x - 0.05f, c1.y - 0.05f, c1.z - 0.05f,
                            c1.x + 0.05f, c1.y + 0.05f, c1.z + 0.05f,
                            1, 1, 1, 1);
                    c1 = new Vector3f(-8.637E+1f, 4.893E+0f, -1.262E+3f);
                    RenderGlobal.drawBoundingBox(c1.x - 0.05f, c1.y - 0.05f, c1.z - 0.05f,
                            c1.x + 0.05f, c1.y + 0.05f, c1.z + 0.05f,
                            1, 0, 0, 1);

                    GlStateManager.popMatrix();
                }

                @Override
                public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
                    return false;
                }
            });
        }
    }

    @SubscribeEvent
    public static void sceneBuild(BuildSceneGraphEvent.BuildEntityScene event) {
        if (!(event.getPackInfo() instanceof ModularVehicleInfo))
            return;
        List<PartEntitySeat> seats = ((ModularVehicleInfo) event.getPackInfo()).getPartsByType(PartEntitySeat.class);
        if (seats.isEmpty())
            return;
        event.addSceneNode("npc.seats", (scale, list) -> (SceneNode) new NpcSeatsRoot(scale, seats.stream().map(seat -> new NpcSeatNode(seat, scale)).collect(Collectors.toList())));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && MC.world != null) {
            PlayerManager.tick();
        }
    }

    @SubscribeEvent
    public static void worldUnload(WorldEvent.Unload event) {
        PlayerManager.clear();
    }

    @SubscribeEvent
    public static void renderTE(DynamXBlockEvent.RenderTileEntity entity) {
        GlStateManager.disableCull();
    }

    /*@SubscribeEvent
    public static void entityJoinWorld(EntityJoinWorldEvent event) {
        if (MC.player == null) {
            return;
        }
        PlayerInformation info = PlayerManager.getPlayerInformation(MC.player);
        if (info.getWantedLevel() == 0) {
            return;
        }
        if (event.getEntity() instanceof EntityGtwNpc && ((EntityGtwNpc) event.getEntity()).getNpcType().isPolice()) {
            EntityGtwNpc npc = (EntityGtwNpc) event.getEntity();
            GtwMapApi.addTrackedObject(new ITrackableObject.TrackedEntity(npc, "Police", "player_white") {
                @Override
                public int renderPoliceCircleAroundRadius() {
                    return 30;
                }
            });
        }
    }

    @SubscribeEvent
    public static void vehicleEntityInit(PhysicsEntityEvent.Init event) {
        if (event.getEntity() instanceof BaseVehicleEntity && event.getEntity().hasModuleOfType(GtwNpcModule.class) && event.getEntity().getModuleByType(GtwNpcModule.class).getVehicleType().isPolice()) {
            BaseVehicleEntity<?> vehicle = (BaseVehicleEntity<?>) event.getEntity();
            GtwMapApi.addTrackedObject(new ITrackableObject.TrackedEntity(vehicle, "Police", "car_white") {
                @Override
                public int renderPoliceCircleAroundRadius() {
                    return 60;
                }
            });
        }
    }

    /*@SubscribeEvent
    public static void npcDeathEvent(LivingDeathEvent event) {
        if (event.getEntity() instanceof EntityGtwNpc && ((EntityGtwNpc) event.getEntity()).getNpcType().isPolice()) {
            GtwMapApi.getTrackedObjects().remove(event.getEntity());
        }
    }*//*

    @SubscribeEvent
    public static void vehicleRemoveEvent(PhysicsEvent.PhysicsEntityRemoved event) {
        if (event.getPhysicsEntity() instanceof BaseVehicleEntity && event.getPhysicsEntity().hasModuleOfType(GtwNpcModule.class) && event.getPhysicsEntity().getModuleByType(GtwNpcModule.class).getVehicleType().isPolice()) {
            GtwMapApi.removeTrackedObject(event.getPhysicsEntity());
        }
    }


    /*@SubscribeEvent
    public static void sceneBuilding(DynamXEntityRenderEvents.BuildSceneGraph event) {
        System.out.println("Building scene graph " + event.getPackInfo());
        ((SceneBuilder) event.getSceneBuilder()).addNode(event.getPackInfo(), new IDrawablePart<BaseVehicleEntity<?>, IModelPackObject>() {
            @Override
            public SceneGraph<BaseVehicleEntity<?>, IModelPackObject> createSceneGraph(com.jme3.math.Vector3f vector3f, List<SceneGraph<BaseVehicleEntity<?>, IModelPackObject>> list) {
                return new SceneGraph.Node<BaseVehicleEntity<?>, IModelPackObject>(null, null, event.getModelScale(), null) {
                    @Override
                    public void render(@Nullable BaseVehicleEntity<?> baseVehicleEntity, EntityRenderContext entityRenderContext, IModelPackObject iModelPackObject) {
                        System.out.println("Rendering the noode " + baseVehicleEntity);
                    }

                    @Override
                    public void renderDebug(@Nullable BaseVehicleEntity<?> entity, EntityRenderContext context, IModelPackObject packInfo) {
                        GlStateManager.pushMatrix();
                        double x = entity.prevPosX + (entity.posX - entity.prevPosX) * context.getPartialTicks();
                        double y = entity.prevPosY + (entity.posY - entity.prevPosY) * context.getPartialTicks();
                        double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * context.getPartialTicks();
                        GlStateManager.translate(-x, -y, -z);

                        float mySpeed = entity.getPhysicsHandler() != null && entity.ticksExisted > 10 ? entity.getPhysicsHandler().getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : 0;
                        boolean checkFar = mySpeed > 30;
                        AxisAlignedBB front = new AxisAlignedBB(entity.getPositionVector().add(entity.getLookVec().scale(4)), entity.getPositionVector().add(entity.getLookVec().scale(checkFar ? 15 : 7))).grow(4);
                        com.jme3.math.Vector3f right = entity.physicsRotation.mult(com.jme3.math.Vector3f.UNIT_X, Vector3fPool.get()).multLocal(-4);
                        front = front.expand((float) right.x, (float) right.y, (float) right.z);
                        Vec3d center = front.getCenter();
                        Matrix3f rot = entity.physicsRotation.toRotationMatrix();
                        org.joml.Matrix3f jomlRot = new org.joml.Matrix3f(rot.get(0, 0), rot.get(0, 1), rot.get(0, 2), rot.get(1, 0), rot.get(1, 1), rot.get(1, 2), rot.get(2, 0), rot.get(2, 1), rot.get(2, 2));
                        OOBB oobb = new OOBB(new org.joml.Vector3f((float) center.x, (float) center.y, (float) center.z + 1 + (checkFar ? 5 : 0)), new org.joml.Vector3f(4, 1, checkFar ? 10 : 4), jomlRot);

                        RenderGlobal.drawSelectionBoundingBox(front, 1, 0, 0, 1);
                        RenderGlobal.drawSelectionBoundingBox(oobb.toAABB(), 0, 0, 1, 1);

                        GlStateManager.popMatrix();
                    }
                };
            }

            @Override
            public String getNodeName() {
                return "obstacle_debug";
            }

            @Override
            public String getObjectName() {
                return null;
            }
        });
    }*/
}

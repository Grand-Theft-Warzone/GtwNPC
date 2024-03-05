package fr.aym.gtwnpc.client.render;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import fr.aym.gtwnpc.dynamx.AutopilotModule;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nonnull;

public class NpcSeatNode extends SimpleNode<BaseRenderContext.EntityRenderContext, IModelPackObject> {
    private static EntityGtwNpc gtwNpc;
    private final PartEntitySeat seat;

    public NpcSeatNode(PartEntitySeat seat, @Nonnull Vector3f scale) {
        super(seat.getPosition(), GlQuaternionPool.newGlQuaternion(seat.getRotation()), scale, null);
        this.seat = seat;
    }

    @Override
    public void render(BaseRenderContext.EntityRenderContext context, IModelPackObject packInfo) {
        BaseVehicleEntity<?> entity = (BaseVehicleEntity<?>) context.getEntity();
        if(entity == null || entity.getControllingPassenger() != null) {
            return;
        }
        GtwNpcModule autopilot = entity.getModuleByType(GtwNpcModule.class);
        if(autopilot == null || !autopilot.hasAutopilot() || autopilot.getAutopilotModule().getStolenTime() > 0) {
            return;
        }
        if(gtwNpc == null) {
            gtwNpc = new EntityGtwNpc(entity.world);
        }
        if (MinecraftForgeClient.getRenderPass() == 0 && entity instanceof IModuleContainer.ISeatsContainer) {
            SeatsModule seats = ((IModuleContainer.ISeatsContainer)entity).getSeats();
            assert seats != null;
            gtwNpc.setSkin(autopilot.getNpcSkin());
            gtwNpc.setRidingHack(entity);
            Entity seatRider = gtwNpc;
            seatRider.setPosition(entity.posX, entity.posY, entity.posZ);
            fr.dynamx.client.handlers.ClientEventHandler.renderingEntity = seatRider.getPersistentID();
            DynamXRenderUtils.popGlAllAttribBits();
            float partialTicks = context.getPartialTicks();
            this.transformToRotationPoint();
            EnumSeatPlayerPosition position = seat.getPlayerPosition();
            RenderPhysicsEntity.shouldRenderPlayerSitting = position == EnumSeatPlayerPosition.SITTING;
            if (seat.getPlayerSize() != null)
                transform.scale(seat.getPlayerSize().x, seat.getPlayerSize().y, seat.getPlayerSize().z);
            if (position == EnumSeatPlayerPosition.LYING) transform.rotate(FastMath.PI / 2, 1, 0, 0);
            GlStateManager.pushMatrix();
            GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
            Minecraft.getMinecraft().getRenderManager().renderEntity(seatRider, 0.0, 0.0, 0.0, 0, 0, false);
            GlStateManager.popMatrix();
            fr.dynamx.client.handlers.ClientEventHandler.renderingEntity = null;
        }
    }

    @Override
    public void renderDebug(BaseRenderContext.EntityRenderContext context, IModelPackObject packInfo) {
        BaseVehicleEntity<?> entity = (BaseVehicleEntity<?>) context.getEntity();
        if(entity == null) {
            return;
        }
        GtwNpcModule autopilot = entity.getModuleByType(GtwNpcModule.class);
        if(autopilot == null || !autopilot.hasAutopilot()) {
            return;
        }
        if (DynamXDebugOptions.SEATS_AND_STORAGE.isActive()) {
            GlStateManager.pushMatrix();
            this.transformForDebug();
            AxisAlignedBB box = seat.getBox();
            RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, seat.isDriver() ? 0.0F : 1.0F, seat.isDriver() ? 1.0F : 0.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
        super.renderDebug(context, packInfo);
    }
}

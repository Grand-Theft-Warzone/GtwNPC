package fr.aym.gtwnpc.client.render;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
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
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;

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
        // Existence already checked by NpcSeatsPadre
        BaseVehicleEntity<?> entity = (BaseVehicleEntity<?>) context.getEntity();
        GtwNpcModule autopilot = entity.getModuleByType(GtwNpcModule.class);
        if (autopilot.getNpcSkins().length <= seat.getId()) {
            return;
        }
        if (gtwNpc == null) {
            gtwNpc = new EntityGtwNpc(entity.world);
        }
        SeatsModule seats = ((IModuleContainer.ISeatsContainer) entity).getSeats();
        assert seats != null;
        if (seats.getSeatToPassengerMap().containsKey(seat)) {
            return;
        }
        gtwNpc.setSkin(autopilot.getNpcSkins()[seat.getId()]);
        NonNullList<ItemStack> inv = autopilot.getNpcInventories();
        if (inv != null && inv.size() > seat.getId() * 6 + 5) {
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, inv.get(seat.getId() * 6 + 0));
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, inv.get(seat.getId() * 6 + 1));
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.HEAD, inv.get(seat.getId() * 6 + 2));
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.CHEST, inv.get(seat.getId() * 6 + 3));
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.LEGS, inv.get(seat.getId() * 6 + 4));
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.FEET, inv.get(seat.getId() * 6 + 5));
        } else {
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, ItemStack.EMPTY);
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.CHEST, ItemStack.EMPTY);
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.LEGS, ItemStack.EMPTY);
            gtwNpc.setItemStackToSlot(EntityEquipmentSlot.FEET, ItemStack.EMPTY);
        }
        gtwNpc.setRidingHack(entity);
        Entity seatRider = gtwNpc;
        seatRider.setPosition(entity.posX, entity.posY, entity.posZ);
        fr.dynamx.client.handlers.ClientEventHandler.renderingEntity = seatRider.getPersistentID();
        DynamXRenderUtils.popGlAllAttribBits();
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

    @Override
    public void renderDebug(BaseRenderContext.EntityRenderContext context, IModelPackObject packInfo) {
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

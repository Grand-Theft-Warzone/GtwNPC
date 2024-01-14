package fr.aym.gtwnpc.client.render;

import com.modularwarfare.client.model.layers.RenderLayerHeldGun;
import com.modularwarfare.common.guns.ItemGun;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class RenderGtwNpc extends RenderBiped<EntityGtwNpc> {
    public RenderGtwNpc(RenderManager rendermanagerIn) {
        super(rendermanagerIn, new ModelBiped(0, 0, 64, 64) {
            @Override
            public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTickTime) {
                rightArmPose = ArmPose.EMPTY;
                leftArmPose = ArmPose.EMPTY;
                super.setLivingAnimations(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTickTime);
            }

            @Override
            public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
                super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
                if (((EntityLiving) entityIn).getHeldItemMainhand().getItem() instanceof ItemGun) {
                    this.bipedRightArm.rotateAngleY = -0.1F + this.bipedHead.rotateAngleY;
                    //this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY + 0.4F;
                    this.bipedRightArm.rotateAngleX = -((float) Math.PI / 2F) + this.bipedHead.rotateAngleX;
                    //this.bipedLeftArm.rotateAngleX = -((float) Math.PI / 2F) + this.bipedHead.rotateAngleX;
                }
            }
        }, 0.5f);
        this.addLayer(new LayerBipedArmor(this));
        RenderLayerHeldGun layer = new RenderLayerHeldGun(this);
        this.addLayer(layer);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityGtwNpc entity) {
        return entity.getSkinRes();
    }
}

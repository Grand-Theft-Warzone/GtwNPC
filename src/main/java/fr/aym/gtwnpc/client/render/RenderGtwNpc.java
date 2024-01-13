package fr.aym.gtwnpc.client.render;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class RenderGtwNpc extends RenderBiped<EntityGtwNpc> {
    public RenderGtwNpc(RenderManager rendermanagerIn) {
        super(rendermanagerIn, new ModelBiped(0, 0, 64, 64), 0.5f);
        this.addLayer(new LayerBipedArmor(this));
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityGtwNpc entity) {
        return entity.getSkin();
    }
}

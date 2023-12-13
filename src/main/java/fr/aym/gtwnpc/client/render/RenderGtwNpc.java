package fr.aym.gtwnpc.client.render;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class RenderGtwNpc extends RenderLiving<EntityGtwNpc> {
    //Temporary
    private static final ResourceLocation TEXTURE_STEVE = new ResourceLocation("textures/entity/steve.png");
    private static final ResourceLocation TEXTURE_ALEX = new ResourceLocation("textures/entity/alex.png");

    public RenderGtwNpc(RenderManager rendermanagerIn) {
        super(rendermanagerIn, new ModelBiped(0, 0, 64, 64), 0.5f);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityGtwNpc entity) {
        return TEXTURE_STEVE;
    }
}

package fr.aym.gtwnpc.client.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SkinTexture extends AbstractTexture {
    private final ResourceLocation location;
    private final File file;

    public SkinTexture(ResourceLocation location, File file) {
        this.location = location;
        this.file = file;
        load(Minecraft.getMinecraft().getTextureManager());
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        this.deleteGlTexture();
        try (FileInputStream fis = new FileInputStream(file)) {
            BufferedImage bufferedimage = TextureUtil.readBufferedImage(fis);
            TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), bufferedimage, false, false);
        }
    }

    public void load(TextureManager into) {
        into.loadTexture(location, this);
    }
}

package fr.aym.gtwnpc.client;

import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID, value = Side.CLIENT)
public class HudEventHandler {
    public static final ResourceLocation STAR_EMPTY = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/star_empty.png");
    public static final ResourceLocation STAR_FULL = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/star_full.png");

    private static float counter;

    @SubscribeEvent
    public static void tickHud(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ClientEventHandler.MC.player != null) {
            PlayerInformation info = PlayerManager.getPlayerInformation(ClientEventHandler.MC.player);
            if (info.getWantedLevel() == 0)
                return;
            int thres = (GtwNpcsConfig.config.getPlayerHideCooldown() * 20) / 4;
            int hidden = info.getHiddenTime() < thres ? info.getHiddenTime() : info.getHiddenTime() - thres;
            if (info.getHiddenTime() > thres) {
                float a = 0.04f;
                float y = 0.16f;
                float x = GtwNpcsConfig.config.getPlayerHideCooldown() * 20 - thres;
                float b = (float) (Math.log(y / a) / x);
                float increment = (float) (a * Math.exp(b * hidden));
                counter += increment;
            } else {
                counter = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);
            PlayerInformation info = PlayerManager.getPlayerInformation(ClientEventHandler.MC.player);
            if (info.getWantedLevel() == 0)
                return;
            int size = 20;
            int x = event.getResolution().getScaledWidth() - 5 - size;
            int y = 260+5;
            for (int i = 0; i < 5; i++) {
                boolean full = i < info.getWantedLevel() && (int) counter % 2 == 0;
                ClientEventHandler.MC.getTextureManager().bindTexture(full ? STAR_FULL : STAR_EMPTY);
                Gui.drawScaledCustomSizeModalRect(x - i * (size + 2), y, 0, 0, 330, 330, size, size, 330, 330);
            }
        }
    }
}

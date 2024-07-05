package fr.aym.gtwnpc.client;

import com.grandtheftwarzone.gtwclient.mod.GTWClient;
import fr.aym.acsguis.cssengine.font.CssFontHelper;
import fr.aym.gtwnpc.GtwNpcMod;
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

import java.util.Collections;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID, value = Side.CLIENT)
public class HudEventHandler {
    public static final ResourceLocation FONT = new ResourceLocation(GtwNpcConstants.ID, "pricedownbl.otf");

    public static final ResourceLocation HEART = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/hearthud.png");
    public static final ResourceLocation ARMOR = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/armor.png");
    public static final ResourceLocation MONEY = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/money.png");
    public static final ResourceLocation LEVEL = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/level.png");
    public static final ResourceLocation FOOD = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/food.png");
    public static final ResourceLocation STAR_EMPTY = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/star_empty.png");
    public static final ResourceLocation STAR_FULL = new ResourceLocation(GtwNpcConstants.ID, "textures/hud/star_full.png");

    private static float counter;
    private static double money = Double.MIN_VALUE;

    private static boolean hasGTWClient;

    static {
        try {
            Class.forName("com.grandtheftwarzone.gtwclient.mod.GTWClient");
            hasGTWClient = true;
            GtwNpcMod.log.info("GTWClient found, enabling GTW Client hud integration");
        } catch (ClassNotFoundException e) {
            hasGTWClient = false;
            GtwNpcMod.log.warn("GTWClient not found, disabling GTW Client hud integration");
        }
    }

    @SubscribeEvent
    public static void tickHud(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ClientEventHandler.MC.player != null) {
            PlayerInformation info = PlayerManager.getPlayerInformation(ClientEventHandler.MC.player);
            if (info.getWantedLevel() == 0)
                return;
            int thres = (GtwNpcsConfig.config.getPlayerHideCooldown() * 2) / 4;
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

    private static void drawFirstLine(int x, int y) {
        ClientEventHandler.MC.getTextureManager().bindTexture(HEART);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 209, 191, 20, 20, 209, 191);
        x += 22;
        GlStateManager.scale(0.5, 0.5, 1);
        CssFontHelper.getBoundFont().draw(x * 2 + 4, y * 2 + 3, String.format("%d", (int) ClientEventHandler.MC.player.getHealth()), 0xE94F21);
        GlStateManager.scale(2, 2, 1);
        x += 22;
        ClientEventHandler.MC.getTextureManager().bindTexture(ARMOR);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 51, 59, 20, 20, 51, 59);
        x += 22;
        GlStateManager.scale(0.5, 0.5, 1);
        CssFontHelper.getBoundFont().draw(x * 2 + 4, y * 2 + 3, String.format("%d", (int) ClientEventHandler.MC.player.getTotalArmorValue() * 5), 0xBFBFBF);
        GlStateManager.scale(2, 2, 1);
        x += 32;
        ClientEventHandler.MC.getTextureManager().bindTexture(FOOD);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 63, 56, 20, 20, 63, 56);
        x += 22;
        GlStateManager.scale(0.5, 0.5, 1);
        CssFontHelper.getBoundFont().draw(x * 2 + 4, y * 2 + 3, String.format("%d", (int) ClientEventHandler.MC.player.getFoodStats().getFoodLevel()), 0xC55A11);
        GlStateManager.scale(2, 2, 1);
    }

    private static void drawMoney(int x, int y) {
        ClientEventHandler.MC.getTextureManager().bindTexture(MONEY);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 212, 195, 20, 20, 212, 195);
        x += 22;
        GlStateManager.scale(0.5, 0.5, 1);
        String s = money == Double.MIN_VALUE ? "--" : String.format("%.2f", money);
        CssFontHelper.getBoundFont().draw(x * 2 + 4, y * 2 + 3, s, 0xA9D18E);
        GlStateManager.scale(2, 2, 1);
    }

    private static void drawLevel(int x, int y) {
        ClientEventHandler.MC.getTextureManager().bindTexture(LEVEL);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 216, 213, 20, 20, 216, 213);
        GlStateManager.scale(0.4, 0.4, 1);

        String s = "" + GTWClient.instance.getPlayerData().getLevel();
        int w = CssFontHelper.getBoundFont().getWidth(s);
        int h = CssFontHelper.getBoundFont().getHeight(s);
        CssFontHelper.getBoundFont().draw(x / 0.4f + 10 / 0.4f - w * 0.4f, y / 0.4f - 5 / 0.4f + h * 0.4f + 12, s, 0xFFFFFF);
        GlStateManager.scale(0.5f / 0.4, 0.5f / 0.4, 1);
        x += 22;
        CssFontHelper.getBoundFont().draw(x * 2 + 4, y * 2 + 3, ClientEventHandler.MC.player.getDisplayNameString(), 0xF6A123);
        GlStateManager.scale(2, 2, 1);
    }

    private static void drawWantedLevel(int x, int y) {
        PlayerInformation info = PlayerManager.getPlayerInformation(ClientEventHandler.MC.player);
        if (info.getWantedLevel() == 0)
            return;
        int size = 20;
        x += 5 * (size + 2);
        //int x = frameWidth - 5 - size;
        for (int i = 0; i < 5; i++) {
            boolean full = i < info.getWantedLevel() && (int) counter % 2 == 0;
            ClientEventHandler.MC.getTextureManager().bindTexture(full ? STAR_FULL : STAR_EMPTY);
            Gui.drawScaledCustomSizeModalRect(x - i * (size + 2), y, 0, 0, 330, 330, size, size, 330, 330);
        }
    }

    @SubscribeEvent
    public static void onPreRenderHud(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.HEALTH
                || event.getType() == RenderGameOverlayEvent.ElementType.ARMOR
                || event.getType() == RenderGameOverlayEvent.ElementType.FOOD) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPostRenderHud(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);

            CssFontHelper.pushDrawing(FONT, Collections.emptyList());
            int size = 20 + 20 + 20 + 30 + 20 + 20 + 2 * 5;
            int x = event.getResolution().getScaledWidth() - 5 - size;
            drawFirstLine(x, 5);
            drawMoney(x, 30);
            if (hasGTWClient)
                drawLevel(x, 55);
            drawWantedLevel(x, 80);
            CssFontHelper.popDrawing();
        }
    }
}

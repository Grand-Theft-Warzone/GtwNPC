package fr.aym.gtwnpc.server;

import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class ServerEventHandler {
    @SubscribeEvent
    public static void playerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerManager.removePlayerInformation(event.player.getUniqueID());
    }
}

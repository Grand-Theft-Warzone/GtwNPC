package fr.aym.gtwnpc.server;

import fr.aym.gtwnpc.common.NpcSpawningSystem;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class ServerEventHandler {
    @SubscribeEvent
    public static void playerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerManager.removePlayerInformation(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.START && FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
            if(world == null)
                return;
            NpcSpawningSystem.tick((WorldServer) world);
        }
    }
}

package fr.aym.gtwnpc.server;

import fr.aym.dynamxgarageaddon.DynamXGarageAddon;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.common.NpcSpawningSystem;
import fr.aym.gtwnpc.network.SCMessagePlayerMoney;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class ServerEventHandler {
    private static final Map<EntityPlayer, Double> playerMoneyCache = new HashMap<>();

    @SubscribeEvent
    public static void playerConnect(PlayerEvent.PlayerLoggedInEvent event) {
        if (DynamXGarageAddon.moneyAccessor != null) {
            DynamXGarageAddon.moneyAccessor.getMoney(event.player.getName()).thenAccept(money -> {
                playerMoneyCache.put(event.player, money);
                GtwNpcMod.network.sendTo(new SCMessagePlayerMoney(money), (EntityPlayerMP) event.player);
            }).exceptionally(t -> {
                t.printStackTrace();
                GtwNpcMod.network.sendTo(new SCMessagePlayerMoney(Double.MIN_VALUE), (EntityPlayerMP) event.player);
                return null;
            });
        }
    }

    @SubscribeEvent
    public static void playerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerManager.removePlayerInformation(event.player.getUniqueID());
        playerMoneyCache.remove(event.player);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.world.isRemote && event.phase == TickEvent.Phase.END && event.player.ticksExisted % 600 == 0) { // Each 30 seconds
            if (DynamXGarageAddon.moneyAccessor != null) {
                EntityPlayer player = event.player;
                if (playerMoneyCache.containsKey(player)) {
                    DynamXGarageAddon.moneyAccessor.getMoney(player.getName()).thenAccept(money -> {
                        if (!Objects.equals(playerMoneyCache.get(player), money)) {
                            playerMoneyCache.put(player, money);
                            GtwNpcMod.network.sendTo(new SCMessagePlayerMoney(money), (EntityPlayerMP) player);
                        }
                    }).exceptionally(t -> {
                        t.printStackTrace();
                        playerMoneyCache.remove(player);
                        GtwNpcMod.network.sendTo(new SCMessagePlayerMoney(Double.MIN_VALUE), (EntityPlayerMP) player);
                        return null;
                    });
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().world.isRemote && event.getEntity() instanceof EntityPlayer) {
            PlayerManager.removePlayerInformation(event.getEntity().getUniqueID());
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
            if (world == null)
                return;
            PlayerManager.tick();
            NpcSpawningSystem.tick((WorldServer) world);

            // Traffic light timings
            if (FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter() % 15 == 0) {
                if (trafficLightModeToState.get((byte) 4) == 0)
                    trafficLightModeToState.put((byte) 4, (byte) 3);
                else
                    trafficLightModeToState.put((byte) 4, (byte) 0);
            }
            tfTimerShort++;
            switch (tfTimerShort) {
                case 140: //SHORT B ORANGE
                    trafficLightModeToState.put((byte) 2, (byte) 0);
                    break;
                case 170: //SHORT A GREEN & SHORT B RED
                    trafficLightModeToState.put((byte) 0, (byte) 1);
                    trafficLightModeToState.put((byte) 2, (byte) 2);
                    break;
                case 270: //SHORT A ORANGE
                    trafficLightModeToState.put((byte) 0, (byte) 0);
                    break;
                case 300: //SHORT A RED & SHORT B GREEN, CYCLE END
                    trafficLightModeToState.put((byte) 0, (byte) 2);
                    trafficLightModeToState.put((byte) 2, (byte) 1);
                    tfTimerShort = 0;
                    break;
            }

            tfTimerLong++;
            switch (tfTimerLong) {
                case 280: //LONG B ORANGE
                    trafficLightModeToState.put((byte) 3, (byte) 0);
                    break;
                case 340: //LONG A GREEN & LONG B RED
                    trafficLightModeToState.put((byte) 1, (byte) 1);
                    trafficLightModeToState.put((byte) 3, (byte) 2);
                    break;
                case 540: //LONG A ORANGE
                    trafficLightModeToState.put((byte) 1, (byte) 0);
                    break;
                case 600: //LONG A RED & LONG B GREEN, CYCLE END
                    trafficLightModeToState.put((byte) 1, (byte) 2);
                    trafficLightModeToState.put((byte) 3, (byte) 1);
                    tfTimerLong = 0;
                    break;
            }
        }
    }

    /**
     * Traffic light modes :
     * - 0 : short, orientation A
     * - 1 : long, orientation A
     * - 2 : short, orientation B
     * - 3 : long, orientation B
     * - 4 : off
     * <p>
     * States :
     * - 0 : orange
     * - 1 : green
     * - 2 : red
     * - 3 : off
     */
    private static final Map<Byte, Byte> trafficLightModeToState = new HashMap();

    static {
        trafficLightModeToState.put((byte) 0, (byte) 2);
        trafficLightModeToState.put((byte) 1, (byte) 2);
        trafficLightModeToState.put((byte) 2, (byte) 1);
        trafficLightModeToState.put((byte) 3, (byte) 1);
        trafficLightModeToState.put((byte) 4, (byte) 3);
    }

    private static short tfTimerShort, tfTimerLong;

    public static byte getTFStateByMode(byte mode) {
        return trafficLightModeToState.getOrDefault(mode, (byte) 3);
    }
}

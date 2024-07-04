package fr.aym.gtwnpc.player;

import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager
{
    private static final Map<UUID, PlayerInformation> playerInfos = new HashMap<>();

    @Nullable
    public static PlayerInformation getPlayerInformation(UUID uuid) {
        return playerInfos.get(uuid);
    }

    @Nonnull
    public static PlayerInformation getPlayerInformation(EntityPlayer player) {
        if(!playerInfos.containsKey(player.getUniqueID())) {
            //System.out.println("Creating player info for " + player.getUniqueID());
            playerInfos.put(player.getUniqueID(), new PlayerInformation(player));
        }
        return playerInfos.get(player.getUniqueID());
    }

    public static void removePlayerInformation(UUID uuid) {
        //System.out.println("Removing player info for " + uuid);
        playerInfos.remove(uuid);
    }

    public static Map<UUID, PlayerInformation> getPlayerInfos() {
        return playerInfos;
    }

    public static void tick() {
        playerInfos.values().forEach(PlayerInformation::update);
    }

    public static void clear() {
        playerInfos.clear();
    }
}

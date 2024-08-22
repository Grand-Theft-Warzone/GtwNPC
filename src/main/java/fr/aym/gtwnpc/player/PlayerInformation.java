package fr.aym.gtwnpc.player;

import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.network.SCMessagePlayerInformation;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import fr.dynamx.common.entities.BaseVehicleEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PlayerInformation {
    private final EntityPlayer playerIn;
    private int wantedLevel;
    private final List<EntityGtwNpc> trackingPolicemen = new ArrayList<>();
    private final List<GtwNpcModule> trackingVehicles = new ArrayList<>();
    private final Map<BaseVehicleEntity<?>, Integer> collidedVehicles = new HashMap<>();
    @Setter
    private int hiddenTime;

    public PlayerInformation(EntityPlayer playerIn) {
        this.playerIn = playerIn;
    }

    public void update() {
       /* if (playerIn.world.isRemote) {
            if (wantedLevel > 0 && hiddenTime > 0)
                hiddenTime++;
            return;
        }*/
        if (wantedLevel > 0 && playerIn.ticksExisted % 10 == 0) {
            //trackingPolicemen.removeIf(npc -> (npc.isDead || !npc.getState().equals("tracking_wanted")));
            trackingVehicles.removeIf(vehicle -> !vehicle.isTrackingWanted());
            int trackingPolicemenCount = getSeeingPolicemenCount();
            if (trackingPolicemenCount > 0 || (playerIn.world.isRemote && hiddenTime == 0)) {
                if (!playerIn.world.isRemote && hiddenTime > 0) {
                    GtwNpcMod.network.sendTo(new SCMessagePlayerInformation(wantedLevel, 0), (EntityPlayerMP) playerIn);
                }
                hiddenTime = 0;
            } else {
                if (hiddenTime == 0) {
                    GtwNpcMod.network.sendTo(new SCMessagePlayerInformation(wantedLevel, 1), (EntityPlayerMP) playerIn);
                }
                hiddenTime++;
                if (!playerIn.world.isRemote && hiddenTime > GtwNpcsConfig.config.getPlayerHideCooldown() * 2) {
                    setWantedLevel(0);
                }
            }
        }
        if (!playerIn.world.isRemote && !collidedVehicles.isEmpty()) {
            collidedVehicles.keySet().removeIf(vehicle -> vehicle.isDead || (vehicle.ticksExisted - collidedVehicles.get(vehicle)) > 100);
        }
    }

    public void setWantedLevel(int wantedLevel) {
        wantedLevel = Math.max(0, Math.min(5, wantedLevel));
        this.wantedLevel = wantedLevel;
        hiddenTime = 0;
        if (!playerIn.world.isRemote) {
            if (wantedLevel == 0) {
                trackingPolicemen.forEach(npc -> npc.setState("wandering"));
                trackingPolicemen.clear();
                trackingVehicles.forEach(vehicle -> vehicle.getPoliceAI().setPlayerTarget(null));
                trackingVehicles.clear();
                playerIn.sendMessage(new TextComponentString(TextFormatting.GREEN + "You are no longer wanted by the police"));
            }
            GtwNpcMod.network.sendTo(new SCMessagePlayerInformation(wantedLevel, 0), (EntityPlayerMP) playerIn);
        }
    }

    public int getSeeingPolicemenCount() {
        return (int) trackingPolicemen.stream().filter(npc -> npc.getDistance(playerIn) < 30 && npc.getEntitySenses().canSee(playerIn)).count() +
                (int) trackingVehicles.stream().filter(vehicle -> vehicle.getEntity().getDistance(playerIn) < 60).count();
    }
}

package fr.aym.gtwnpc.player;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PlayerInformation
{
    private final EntityPlayer playerIn;
    private int wantedLevel;
    private final List<EntityGtwNpc> trackingPolicemen = new ArrayList<>();

    public PlayerInformation(EntityPlayer playerIn) {
        this.playerIn = playerIn;
    }

    public void update() {
        trackingPolicemen.removeIf(npc -> (npc.isDead || !npc.getState().equals("tracking_wanted")));
    }

    public void setWantedLevel(int wantedLevel) {
        wantedLevel = Math.max(0, Math.min(5, wantedLevel));
        this.wantedLevel = wantedLevel;
    }
}

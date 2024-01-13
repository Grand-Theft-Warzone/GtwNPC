package fr.aym.gtwnpc.player;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.EntityPlayer;

@Getter
public class PlayerInformation
{
    private final EntityPlayer playerIn;
    @Setter
    private int wantedLevel;

    public PlayerInformation(EntityPlayer playerIn) {
        this.playerIn = playerIn;
    }
}

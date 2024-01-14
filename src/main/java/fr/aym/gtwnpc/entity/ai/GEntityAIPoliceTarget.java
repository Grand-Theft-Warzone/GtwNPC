package fr.aym.gtwnpc.entity.ai;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.player.EntityPlayer;

public class GEntityAIPoliceTarget extends EntityAINearestAttackableTarget<EntityPlayer>
{
    private final EntityGtwNpc npc;

    public GEntityAIPoliceTarget(EntityGtwNpc creature) {
        super(creature, EntityPlayer.class, 10, false, false, p -> {
            PlayerInformation info = PlayerManager.getPlayerInformation(p.getPersistentID());
            return info != null && info.getWantedLevel() > 0;
        });
        this.npc = creature;
    }

    @Override
    public boolean shouldExecute() {
        if(!npc.getState().equals("tracking_wanted"))
            return false;
        return super.shouldExecute();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if(!npc.getState().equals("tracking_wanted"))
            return false;
        return super.shouldContinueExecuting();
    }
}

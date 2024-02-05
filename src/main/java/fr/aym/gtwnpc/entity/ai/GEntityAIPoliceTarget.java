package fr.aym.gtwnpc.entity.ai;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.player.EntityPlayer;

public class GEntityAIPoliceTarget extends EntityAINearestAttackableTarget<EntityPlayer> {
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
        if (npc.getNpcType() != SkinRepository.NpcType.POLICE)
            return false;
        boolean b = super.shouldExecute();
        if (b && (npc.getState().equals("wandering") || npc.getState().equals("sitting"))) {
            npc.setState("tracking_wanted");
        } else if (!b && npc.getState().equals("tracking_wanted")) {
            npc.setState("wandering");
        }
        return b;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (!npc.getState().equals("tracking_wanted") || target == null)
            return false;
        PlayerInformation info = PlayerManager.getPlayerInformation(target.getPersistentID());
        if(info == null || info.getWantedLevel() <= 0)
            return false;
        return super.shouldContinueExecuting();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if(target == null)
            return;
        PlayerInformation info = PlayerManager.getPlayerInformation(target.getPersistentID());
        if (info != null && !info.getTrackingPolicemen().contains(npc)) {
            info.getTrackingPolicemen().add(npc);
        }
    }

    @Override
    public void resetTask() {
        if(target != null) {
            PlayerInformation info = PlayerManager.getPlayerInformation(target.getPersistentID());
            if (info != null) {
                info.getTrackingPolicemen().remove(npc);
            }
        }
        super.resetTask();
    }

    public void setTargetEntity(EntityPlayer player) {
        this.targetEntity = player;
    }
}

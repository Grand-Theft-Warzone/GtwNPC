package fr.aym.gtwnpc.entity.ai;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.dynamx.common.entities.BaseVehicleEntity;
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
        if (npc.getNpcType() == SkinRepository.NpcType.NPC)
            return false;
        boolean b = super.shouldExecute();
        if (b) {// && (npc.getState().equals("wandering") || npc.getState().equals("sitting"))) {
            npc.setState("tracking_wanted");
        } else if (!b && npc.getState().equals("tracking_wanted")) {
            npc.setState("wandering");
        }
        return b;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (!npc.getState().equals("tracking_wanted") || targetEntity == null) {
            System.out.println("Bz " + npc.getState() + " " + targetEntity);
            return false;
        }
        PlayerInformation info = PlayerManager.getPlayerInformation(targetEntity.getPersistentID());
        if (info == null || info.getWantedLevel() <= 0)
            return false;
        return super.shouldContinueExecuting();
    }

    @Override
    public void updateTask() {
        super.updateTask();
        /*if(targetEntity != null)
            System.out.println("Target " + targetEntity+" "+npc.getOwnerVehicle() + " " + npc.getDistance(targetEntity));
        else
            System.out.println("Target null");*/
        if (targetEntity != null && npc.getOwnerVehicle() != null && npc.getDistance(targetEntity) > Math.max(30, npc.getDistance(npc.getOwnerVehicle()))) {
            BaseVehicleEntity<?> vehicle = npc.getOwnerVehicle();
            System.out.println("Going to owner " + vehicle+" "+vehicle.isDead);
            if (vehicle.isDead) {
                npc.setOwnerVehicle(null);
            } else if (npc.getReturnToCarAI() != null) {
                npc.getReturnToCarAI().setTargetVehicle(vehicle);
                npc.setState("returning_to_car");
            }
        }
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (targetEntity == null)
            return;
        PlayerInformation info = PlayerManager.getPlayerInformation(targetEntity.getPersistentID());
        if (info != null && !info.getTrackingPolicemen().contains(npc)) {
            info.getTrackingPolicemen().add(npc);
        }
    }

    @Override
    public void resetTask() {
        if (targetEntity != null) {
            PlayerInformation info = PlayerManager.getPlayerInformation(targetEntity.getPersistentID());
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

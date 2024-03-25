package fr.aym.gtwnpc.dynamx;

import fr.aym.gtwnpc.path.CarPathNodes;
import fr.aym.gtwnpc.path.NodeType;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.dynamx.common.entities.BaseVehicleEntity;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;

import javax.vecmath.Vector3f;
import java.util.HashSet;

public class VehiclePoliceAI {
    private final BaseVehicleEntity<?> entity;
    private final GtwNpcModule engineModule;
    @Getter
    private EntityPlayer playerTarget;

    private PathNode targetNode;

    @Getter
    private boolean isCatchingVilains;

    public VehiclePoliceAI(BaseVehicleEntity<?> entity, GtwNpcModule engineModule) {
        this.entity = entity;
        this.engineModule = engineModule;
    }

    public void setPlayerTarget(EntityPlayer playerTarget) {
        this.playerTarget = playerTarget;
        this.targetNode = null;
        this.isCatchingVilains = playerTarget != null;
    }

    public void update() {
        if (!engineModule.hasAutopilot()) {
            return;
        }
        if (engineModule.getStolenTime() > 0) {
            setPlayerTarget(null);
            return;
        }
        if (playerTarget != null) {
            PathNode targetNode = CarPathNodes.getInstance().findNearestNode(playerTarget.getPositionVector(), engineModule.getAutopilotModule().nodeBlacklist);
            if (targetNode == null) {
                this.targetNode = null;
                //  System.out.println("Target node is null");
                return;
            }
            boolean direct = false;
            if (entity.getDistance(playerTarget) < (engineModule.getAutopilotModule().getObstacleDetection().getStuckTime() > 40 ? 36 : 20)) {
                if (Math.abs(engineModule.getVehicleSpeed()) < 5) {
                    if (entity.world.rand.nextInt(100) < 5) {
                        System.out.println("Dismount police");
                        if (!entity.world.isRemote) {
                            engineModule.stealVehicle("dismounted");
                            engineModule.dismountNpcPassengers(entity.rotationYaw + 180);
                        }
                    } else {
                        engineModule.getAutopilotModule().stopNavigation(4);
                    }
                    return;
                } else {
                    //System.out.println("Rerouting near player");
                    targetNode = new PathNode(new Vector3f((float) playerTarget.posX, (float) playerTarget.posY, (float) playerTarget.posZ), new HashSet<>(), NodeType.CAR_CITY_LOW_SPED);
                    direct = true;
                }
            }
            if (!direct && targetNode.getDistance(playerTarget.getPositionVector()) > 30) {
                targetNode = new PathNode(new Vector3f((float) playerTarget.posX, (float) playerTarget.posY, (float) playerTarget.posZ), new HashSet<>(), NodeType.UNDEFINED);
                direct = true;
                //System.out.println("Direct path");
            }
            if (targetNode != this.targetNode) {
                this.targetNode = targetNode;
                if (!engineModule.getAutopilotModule().makePathToNode(targetNode, direct)) {
                    // System.out.println("Tg you " + targetNode.getPosition());
                    this.targetNode = null;
                }// else
                //  System.out.println("New target node " + targetNode.getPosition());
            } else {
                //System.out.println("Same target node " + targetNode.getPosition());
            }
        } else {
            EntityPlayer target = entity.world.getNearestAttackablePlayer(entity.posX, entity.posY, entity.posZ, 1000, 1000, null, p -> {
                PlayerInformation info = PlayerManager.getPlayerInformation(p.getPersistentID());
                return info != null && info.getWantedLevel() > 0;
            });
            if (target != null) {
                setPlayerTarget(target);
                PlayerManager.getPlayerInformation(target).getTrackingVehicles().add(engineModule);
                //  System.out.println("Set target " + playerTarget);
            } else {
                //   System.out.println("No target");
            }
        }
    }
}

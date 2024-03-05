package fr.aym.gtwnpc.dynamx;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import fr.dynamx.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

@SynchronizedEntityVariable.SynchronizedPhysicsModule(
        modid = GtwNpcConstants.ID
)
public class GtwNpcModule extends CarEngineModule {
    @Getter
    private AutopilotModule autopilotModule;

    @SynchronizedEntityVariable(name="hasAutopilot")
    private final EntityVariable<Boolean> hasAutopilot = new EntityVariable<>((var, val) -> {
        if (val) {
            if (autopilotModule == null) {
                autopilotModule = new AutopilotModule(entity, this);
            }
        } else {
            autopilotModule = null;
        }
    }, SynchronizationRules.SERVER_TO_CLIENTS, false);

    @SynchronizedEntityVariable(name="npcSkin")
    private final EntityVariable<String> npcSkin = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, "");

    public GtwNpcModule(BaseVehicleEntity<?> vehicleEntity, CarEngineModule engineModule) {
        super(vehicleEntity, engineModule.getEngineInfo());
    }

    public void enableAutopilot() {
        if (autopilotModule == null) {
            autopilotModule = new AutopilotModule(entity, this);
        }
        hasAutopilot.set(true);
        this.npcSkin.set(SkinRepository.getRandomSkin(SkinRepository.NpcType.NPC, entity.world.rand).toString());
    }

    public boolean hasAutopilot() {
        return hasAutopilot.get();
    }

    public String getNpcSkin() {
        return npcSkin.get();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        hasAutopilot.set(tag.getBoolean("hasAutopilot"));
        npcSkin.set(tag.getString("npcSkin"));
        if (hasAutopilot()) {
            autopilotModule = new AutopilotModule(entity, this);
            autopilotModule.readFromNBT(tag);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("hasAutopilot", hasAutopilot.get());
        tag.setString("npcSkin", npcSkin.get());
        if (autopilotModule != null)
            autopilotModule.writeToNBT(tag);
    }

    @Override
    public boolean listenEntityUpdates(Side side) {
        return true;
    }

    @Override
    public void updateEntity() {
       // System.out.println("Pils " + autopilotModule + " // " + hasAutopilot.get());
        if (autopilotModule != null) {
            autopilotModule.updateEntity();
        }
        if (entity.world.isRemote) {
            super.updateEntity();
        } else {
          //  hasAutopilot.setChanged(true);
        }
    }

    @Override
    public void onSetSimulationHolder(SimulationHolder simulationHolder, EntityPlayer simulationPlayerHolder, SimulationHolder.UpdateContext changeContext) {
        super.onSetSimulationHolder(simulationHolder, simulationPlayerHolder, changeContext);
        if (changeContext == SimulationHolder.UpdateContext.NORMAL && entity.getSynchronizer() instanceof SPPhysicsEntitySynchronizer) {
            //  System.out.println("Set simulation holder: " + simulationHolder);
            if (simulationHolder != SimulationHolder.DRIVER_SP) {
                entity.getSynchronizer().setSimulationHolder(SimulationHolder.DRIVER_SP, null);
            }
        }
    }

    @Override
    public void initPhysicsEntity(@Nullable BaseVehiclePhysicsHandler<?> handler) {
        if (handler != null) {
            physicsHandler = new EnginePhysicsHandler(this, handler, handler.getWheels()) {
                @Override
                public void steer(float strength) {
                    if (autopilotModule == null || autopilotModule.getStolenTime() > 0) {
                        super.steer(strength);
                        return;
                    }
                    //System.out.println("Intercepted steer: " + strength + " // " + steerForce);
                    if (autopilotModule.getForcedSteeringTime() > 0) {
                        autopilotModule.setForcedSteeringTime(autopilotModule.getForcedSteeringTime() - 1);
                        super.steer(autopilotModule.getForcedSteering());
                    } else {
                        super.steer(autopilotModule.getSteerForce());
                    }
                }
            };
        }
    }
}

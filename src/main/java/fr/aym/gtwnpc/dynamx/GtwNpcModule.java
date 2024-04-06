package fr.aym.gtwnpc.dynamx;

import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.Vector3f;
import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import fr.dynamx.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.Objects;

@SynchronizedEntityVariable.SynchronizedPhysicsModule(
        modid = GtwNpcConstants.ID
)
public class GtwNpcModule extends CarEngineModule {

    @Getter
    private AutopilotModule autopilotModule;

    @Getter
    private VehiclePoliceAI policeAI;

    @SynchronizedEntityVariable(name = "stolenTime")
    private final EntityVariable<Integer> stolenTime = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, 0);

    @SynchronizedEntityVariable(name = "hasAutopilot")
    private final EntityVariable<Boolean> hasAutopilot = new EntityVariable<>((var, val) -> {
        if (val) {
            if (autopilotModule == null) {
                autopilotModule = new AutopilotModule(entity, this);
            }
        } else {
            autopilotModule = null;
        }
    }, SynchronizationRules.SERVER_TO_CLIENTS, false);

    @SynchronizedEntityVariable(name = "vehicleType")
    private final EntityVariable<VehicleType> vehicleType = new EntityVariable<>((var, vehicleType) -> {
        if (Objects.requireNonNull(vehicleType) == VehicleType.CIVILIAN) {
            if (policeAI != null) {
                policeAI = null;
            }
        } else {
            if (policeAI == null) {
                policeAI = new VehiclePoliceAI(entity, this);
            }
        }
    }, SynchronizationRules.SERVER_TO_CLIENTS, VehicleType.CIVILIAN);

    @SynchronizedEntityVariable(name = "npcSkin")
    private final EntityVariable<String[]> npcSkins = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, new String[0]);

    @SynchronizedEntityVariable(name = "effectiveSteer")
    private final EntityVariable<Float> effectiveSteer = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, -10f);

    private final boolean stealable;

    public GtwNpcModule(BaseVehicleEntity<?> vehicleEntity, CarEngineModule engineModule) {
        super(vehicleEntity, engineModule.getEngineInfo());
        stealable = vehicleEntity.world.rand.nextBoolean();
    }

    public BaseVehicleEntity<?> getEntity() {
        return entity;
    }


    public boolean isStealable() {
        return vehicleType.get() != VehicleType.CIVILIAN || stealable;
    }

    public void enableAutopilot(VehicleType vehicleType, int passengerCount) {
        if (autopilotModule == null) {
            autopilotModule = new AutopilotModule(entity, this);
        }
        hasAutopilot.set(true);
        String[] skins = new String[passengerCount];
        for (int i = 0; i < passengerCount; i++) {
            skins[i] = SkinRepository.getRandomSkin(vehicleType.getNpcType(), entity.world.rand).toString();
        }
        //System.out.println("Autopilot:enable. Skins: " + Arrays.toString(skins));
        npcSkins.set(skins);
        setVehicleType(vehicleType);
    }

    public boolean hasAutopilot() {
        return hasAutopilot.get();
    }

    public String[] getNpcSkins() {
        return npcSkins.get();
    }

    public VehicleType getVehicleType() {
        return vehicleType.get();
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType.set(vehicleType);
        if (Objects.requireNonNull(vehicleType) == VehicleType.CIVILIAN) {
            if (policeAI != null) {
                policeAI = null;
            }
        } else {
            if (policeAI == null) {
                policeAI = new VehiclePoliceAI(entity, this);
            }
        }
    }

    public int getStolenTime() {
        return stolenTime.get();
    }

    public void stealVehicle(String state) {
        stolenTime.set(1);
        if (autopilotModule != null) {
            autopilotModule.stopNavigation(Integer.MAX_VALUE);
            autopilotModule.setState(state);
        }
        setSpeedLimit(Float.MAX_VALUE);
    }

    public float getVehicleSpeed() {
        BaseVehiclePhysicsHandler<?> phycites = entity.getPhysicsHandler();
        if (phycites == null && entity.getSynchronizer() instanceof SPPhysicsEntitySynchronizer<?>) {
            Entity e = ((SPPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).getOtherSideEntity();
            if (e instanceof BaseVehicleEntity<?> && ((BaseVehicleEntity<?>) e).initialized == PhysicsEntity.EnumEntityInitState.ALL) {
                phycites = ((BaseVehicleEntity<?>) e).getPhysicsHandler();
            }
        }
        if (phycites != null && ((PhysicsVehicle) phycites.getCollisionObject()).getController() == null) {
            System.out.println("ENCULE DE FILS DE PUTE DE MERDE " + entity);
            return Float.MIN_VALUE;
        }
        return entity.ticksExisted > 10 && phycites != null ? phycites.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : Float.MIN_VALUE;
    }

    public void restoreAi() {
        stolenTime.set(0);
        if (autopilotModule != null) {
            autopilotModule.setState("restored");
            autopilotModule.stopNavigation(80); // wait for other npcs to join
        }
    }

    public boolean isTrackingWanted() {
        return !entity.isDead && policeAI != null && policeAI.isCatchingVilains() && getStolenTime() == 0;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        hasAutopilot.set(tag.getBoolean("hasAutopilot"));
        if (hasAutopilot()) {
            autopilotModule = new AutopilotModule(entity, this);
            autopilotModule.readFromNBT(tag);
        }
        vehicleType.set(VehicleType.values()[tag.getInteger("vehicleType")]);
        NBTTagList skins = tag.getTagList("npcSkins", Constants.NBT.TAG_STRING);
        String[] npcSkins = new String[skins.tagCount()];
        for (int i = 0; i < skins.tagCount(); i++) {
            npcSkins[i] = skins.getStringTagAt(i);
        }
        this.npcSkins.set(npcSkins);
        stolenTime.set(tag.getInteger("stolenTime"));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("hasAutopilot", hasAutopilot.get());
        if (autopilotModule != null)
            autopilotModule.writeToNBT(tag);
        tag.setInteger("vehicleType", vehicleType.get().ordinal());
        NBTTagList skins = new NBTTagList();
        for (String skin : npcSkins.get()) {
            skins.appendTag(new NBTTagString(skin));
        }
        tag.setTag("npcSkins", skins);
        tag.setInteger("stolenTime", stolenTime.get());
    }

    @Override
    public boolean listenEntityUpdates(Side side) {
        return true;
    }

    @Override
    public void updateEntity() {
        // System.out.println("Pils " + autopilotModule + " // " + hasAutopilot.get());
        if (autopilotModule != null) {
            if (policeAI != null) {
                policeAI.update();
            }
            autopilotModule.updateEntity();

            if (!entity.world.isRemote) {
                if (stolenTime.get() > 0) {
                    if (entity.getControllingPassenger() != null) {
                        stolenTime.set(1);
                    } else if (entity.ticksExisted % 10 == 0) {
                        stolenTime.set(stolenTime.get() + 10);
                        if (stolenTime.get() > 20 * 60 * 5) {
                            System.out.println("Killing stolen entity");
                            entity.setDead();
                        }
                    }
                }
            }
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
                    if (autopilotModule == null || getStolenTime() > 0) {
                        super.steer(strength);
                        if(!entity.world.isRemote)
                            effectiveSteer.set(-10f);
                        return;
                    }
                    if(entity.world.isRemote) {
                        super.steer(effectiveSteer.get() == -10 ? strength : effectiveSteer.get());
                        return;
                    }
                    //System.out.println("Intercepted steer: " + strength + " // " + steerForce);
                    if (autopilotModule.getForcedSteeringTime() > 0) {
                        autopilotModule.setForcedSteeringTime(autopilotModule.getForcedSteeringTime() - 1);
                        super.steer(autopilotModule.getForcedSteering());
                        effectiveSteer.set(autopilotModule.getForcedSteering());
                    } else {
                        super.steer(autopilotModule.getSteerForce());
                        effectiveSteer.set(autopilotModule.getSteerForce());
                    }
                }
            };
        }
    }

    public void dismountNpcPassengers(float rotationYaw) {
        String[] skins = getNpcSkins();
        for (int i = 0; i < skins.length; i++) {
            String skin = skins[i];
            PartEntitySeat seat = entity.getPackInfo().getPartByTypeAndId(PartEntitySeat.class, (byte) i);
            EntityGtwNpc npc = new EntityGtwNpc(entity.world);
            npc.rotationYaw = rotationYaw;
            Vector3fPool.openPool();
            Vector3f dismountPosition = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition().add(Vector3fPool.get(seat.getPosition().x > 0.0F ? 1.0F : -1.0F, 0.0F, 0.0F)), entity.physicsRotation).addLocal(entity.physicsPosition);
            AxisAlignedBB collisionDetectionBox = new AxisAlignedBB(dismountPosition.x, dismountPosition.y + 1.0F, dismountPosition.z, dismountPosition.x + 1.0F, dismountPosition.y + 2.0F, dismountPosition.z + 1.0F);
            if (!npc.world.collidesWithAnyBlock(collisionDetectionBox)) {
                npc.setPositionAndUpdate(dismountPosition.x, collisionDetectionBox.minY, dismountPosition.z);
            } else {
                dismountPosition = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition().add(Vector3fPool.get(seat.getPosition().x > 0.0F ? -2.0F : 2.0F, 0.0F, 0.0F)), entity.physicsRotation).addLocal(entity.physicsPosition);
                collisionDetectionBox = new AxisAlignedBB(dismountPosition.x, dismountPosition.y + 1.0F, dismountPosition.z, dismountPosition.x + 1.0F, dismountPosition.y + 2.0F, dismountPosition.z + 1.0F);
                npc.setPositionAndUpdate(dismountPosition.x, collisionDetectionBox.minY, dismountPosition.z);
            }
            Vector3fPool.closePool();
            npc.setSkin(skin);
            npc.setNpcType(getVehicleType().getNpcType());
            npc.setOwnerVehicle(entity);
            npc.world.spawnEntity(npc);
        }
        npcSkins.set(new String[0]);
    }

    public boolean mountNpcPassenger(EntityGtwNpc passenger) {
        String[] skins = getNpcSkins();
        if (skins.length + 1 < entity.getPackInfo().getPartsByType(PartEntitySeat.class).size()) {
            String[] newSkins = new String[skins.length + 1];
            System.arraycopy(skins, 0, newSkins, 0, skins.length);
            newSkins[skins.length] = passenger.getSkin();
            npcSkins.set(newSkins);
            restoreAi();
            return true;
        }
        return false;
    }
}

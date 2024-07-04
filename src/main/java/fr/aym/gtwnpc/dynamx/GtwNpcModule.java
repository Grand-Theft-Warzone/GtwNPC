package fr.aym.gtwnpc.dynamx;

import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.Vector3f;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.sqript.EventGNpcInit;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.dynamx.addons.basics.common.modules.FuelTankModule;
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
import fr.nico.sqript.ScriptManager;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private final List<EntityGtwNpc> ridingNpcs = new ArrayList<>();

    @SynchronizedEntityVariable(name = "npcSkin")
    private final EntityVariable<String[]> npcSkins = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, new String[0]);

    @SynchronizedEntityVariable(name = "npcInventory")
    private final EntityVariable<NonNullList<ItemStack>> npcInventories = new EntityVariable<>(SynchronizationRules.SERVER_TO_CLIENTS, null);

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

    @Override
    protected void playStartingSound() {
        if (!hasAutopilot())
            super.playStartingSound();
    }

    public boolean isStealable() {
        return vehicleType.get() != VehicleType.CIVILIAN || stealable;
    }

    public void enableAutopilot(VehicleType vehicleType, int passengerCount) {
        if (autopilotModule == null) {
            autopilotModule = new AutopilotModule(entity, this);
        }
        hasAutopilot.set(true);
        ridingNpcs.clear();
        String[] skins = new String[passengerCount];
        NonNullList<ItemStack> invs = NonNullList.withSize(passengerCount * 6, ItemStack.EMPTY);
        for (int i = 0; i < passengerCount; i++) {
            EntityGtwNpc npc = new EntityGtwNpc(entity.world);
            npc.setNpcType(vehicleType.getNpcType());
            ScriptManager.callEvent(new EventGNpcInit(npc));
            skins[i] = npc.getSkin();
            invs.set(i * 6 + 0, npc.getHeldItemMainhand());
            invs.set(i * 6 + 1, npc.getHeldItemOffhand());
            invs.set(i * 6 + 2, npc.getItemStackFromSlot(EntityEquipmentSlot.HEAD));
            invs.set(i * 6 + 3, npc.getItemStackFromSlot(EntityEquipmentSlot.CHEST));
            invs.set(i * 6 + 4, npc.getItemStackFromSlot(EntityEquipmentSlot.LEGS));
            invs.set(i * 6 + 5, npc.getItemStackFromSlot(EntityEquipmentSlot.FEET));
            ridingNpcs.add(npc);
        }
        System.out.println("Autopilot:enable. Skins: " + Arrays.toString(skins));
        npcSkins.set(skins);
        npcInventories.set(invs);
        setVehicleType(vehicleType);
    }

    public boolean hasAutopilot() {
        return hasAutopilot.get();
    }

    public String[] getNpcSkins() {
        return npcSkins.get();
    }

    public NonNullList<ItemStack> getNpcInventories() {
        return npcInventories.get();
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
        FuelTankModule fuelTankModule = entity.getModuleByType(FuelTankModule.class);
        if (fuelTankModule != null) {
            fuelTankModule.setFuel(fuelTankModule.getInfo().getTankSize());
        }
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
            //System.out.println("ENCULE DE FILS DE PUTE DE MERDE " + entity);
            return Float.MIN_VALUE;
        }
        return entity.ticksExisted > 10 && phycites != null ? phycites.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : Float.MIN_VALUE;
    }

    public void restoreAi() {
        stolenTime.set(0);
        if (autopilotModule != null) {
            autopilotModule.setState("restored");
            System.out.println("Restoring autopilot");
            autopilotModule.stopNavigation(80); // wait for other npcs to join
        }
        FuelTankModule fuelTankModule = entity.getModuleByType(FuelTankModule.class);
        if (fuelTankModule != null) {
            fuelTankModule.disableFuelConsumption();
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
        NBTTagList ridingNpcs = tag.getTagList("ridingNpcs", Constants.NBT.TAG_COMPOUND);
        this.ridingNpcs.clear();
        String[] skins = new String[ridingNpcs.tagCount()];
        NonNullList<ItemStack> invs = NonNullList.withSize(ridingNpcs.tagCount() * 6, ItemStack.EMPTY);
        for (int i = 0; i < ridingNpcs.tagCount(); i++) {
            EntityGtwNpc npc = new EntityGtwNpc(entity.world);
            npc.readFromNBT(ridingNpcs.getCompoundTagAt(i));
            skins[i] = npc.getSkin();
            invs.set(i * 6 + 0, npc.getHeldItemMainhand());
            invs.set(i * 6 + 1, npc.getHeldItemOffhand());
            invs.set(i * 6 + 2, npc.getItemStackFromSlot(EntityEquipmentSlot.HEAD));
            invs.set(i * 6 + 3, npc.getItemStackFromSlot(EntityEquipmentSlot.CHEST));
            invs.set(i * 6 + 4, npc.getItemStackFromSlot(EntityEquipmentSlot.LEGS));
            invs.set(i * 6 + 5, npc.getItemStackFromSlot(EntityEquipmentSlot.FEET));
            this.ridingNpcs.add(npc);
        }
        //System.out.println("Autopilot:enable. Skins: " + Arrays.toString(skins));
        npcSkins.set(skins);
        npcInventories.set(invs);
        stolenTime.set(tag.getInteger("stolenTime"));
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("hasAutopilot", hasAutopilot.get());
        if (autopilotModule != null)
            autopilotModule.writeToNBT(tag);
        tag.setInteger("vehicleType", vehicleType.get().ordinal());
        NBTTagList ridingNpcs = new NBTTagList();
        for (EntityGtwNpc npc : this.ridingNpcs) {
            NBTTagCompound npcTag = new NBTTagCompound();
            npc.writeToNBT(npcTag);
            ridingNpcs.appendTag(npcTag);
        }
        tag.setTag("ridingNpcs", ridingNpcs);
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
                } else {
                    FuelTankModule fuelTankModule = entity.getModuleByType(FuelTankModule.class);
                    if (fuelTankModule != null && fuelTankModule.isFuelConsumptionDisabled()) {
                        fuelTankModule.disableFuelConsumption();
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
                        if (!entity.world.isRemote)
                            effectiveSteer.set(-10f);
                        return;
                    }
                    if (entity.world.isRemote) {
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
        for (int i = 0; i < ridingNpcs.size(); i++) {
            PartEntitySeat seat = entity.getPackInfo().getPartByTypeAndId(PartEntitySeat.class, (byte) i);
            EntityGtwNpc npc = ridingNpcs.get(i);
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
            npc.setOwnerVehicle(entity);
            npc.world.spawnEntity(npc);
        }
        ridingNpcs.clear();
        npcSkins.set(new String[0]);
        npcInventories.set(null);
    }

    public boolean mountNpcPassenger(EntityGtwNpc passenger) {
        String[] skins = getNpcSkins();
        if (skins.length + 1 < entity.getPackInfo().getPartsByType(PartEntitySeat.class).size()) {
            ridingNpcs.add(passenger);
            String[] newSkins = new String[ridingNpcs.size()];
            System.arraycopy(skins, 0, newSkins, 0, skins.length);
            newSkins[skins.length] = passenger.getSkin();
            npcSkins.set(newSkins);
            NonNullList<ItemStack> invs = NonNullList.withSize(ridingNpcs.size() * 6, ItemStack.EMPTY);
            if(this.npcInventories.get() != null && this.ridingNpcs.size() > 0) {
                for (int i = 0; i < this.npcInventories.get().size(); i++) {
                    invs.set(i, this.npcInventories.get().get(i));
                }
            }
            invs.set(ridingNpcs.size() - 1 + 0, passenger.getHeldItemMainhand());
            invs.set(ridingNpcs.size() - 1 + 1, passenger.getHeldItemOffhand());
            invs.set(ridingNpcs.size() - 1 + 2, passenger.getItemStackFromSlot(EntityEquipmentSlot.HEAD));
            invs.set(ridingNpcs.size() - 1 + 3, passenger.getItemStackFromSlot(EntityEquipmentSlot.CHEST));
            invs.set(ridingNpcs.size() - 1 + 4, passenger.getItemStackFromSlot(EntityEquipmentSlot.LEGS));
            invs.set(ridingNpcs.size() - 1 + 5, passenger.getItemStackFromSlot(EntityEquipmentSlot.FEET));
            npcInventories.set(invs);
            restoreAi();
            return true;
        }
        return false;
    }
}

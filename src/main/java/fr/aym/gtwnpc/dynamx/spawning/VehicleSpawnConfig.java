package fr.aym.gtwnpc.dynamx.spawning;

import com.jme3.math.Vector3f;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.aym.acslib.utils.nbtserializer.ISerializable;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.dynamx.VehicleType;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.World;

@Getter
public class VehicleSpawnConfig extends WeightedRandom.Item implements ISerializable {
    private String vehicleName;
    private byte vehicleMeta;
    private VehicleType vehicleType;

    public VehicleSpawnConfig() {
        super(0);
    }

    public VehicleSpawnConfig(String vehicleName, byte vehicleMeta, VehicleType vehicleType, int spawnWeight) {
        super(spawnWeight);
        this.vehicleName = vehicleName;
        this.vehicleMeta = vehicleMeta;
        this.vehicleType = vehicleType;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Object[] getObjectsToSave() {
        return new Object[]{vehicleName, vehicleMeta, vehicleType, itemWeight};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        vehicleName = (String) objects[0];
        vehicleMeta = (byte) objects[1];
        vehicleType = (VehicleType) objects[2];
        itemWeight = (int) objects[3];
    }

    public BaseVehicleEntity<?> createVehicle(World world, EntityPlayer player, Vector3f position, float yaw) {
        ModularVehicleInfo info = DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(vehicleName);
        if (info == null)
            return null;
        CarEntity<?> e = new CarEntity<>(vehicleName, world, position, yaw, vehicleMeta);
        int seats = info.getPartsByType(PartEntitySeat.class).size();
        int passengers = seats == 1 ? 1 : world.rand.nextInt( seats - 1) + 1;
        if (passengers == 1 && vehicleType != VehicleType.CIVILIAN && seats > 1 && world.rand.nextInt(3) < 2) {
            passengers = 2;
        }
        int finalPassengers = passengers;
        e.setInitCallback((entity, modules) -> e.getModuleByType(GtwNpcModule.class).enableAutopilot(vehicleType, finalPassengers));
        return e;
    }
}

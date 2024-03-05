package fr.aym.gtwnpc.dynamx.spawning;

import fr.aym.acslib.utils.nbtserializer.ISerializable;
import fr.aym.acslib.utils.nbtserializer.NBTSerializer;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import lombok.Getter;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.DimensionType;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = GtwNpcConstants.ID)
public class VehicleSpawnConfigs extends WorldSavedData implements ISerializable
{
    @Getter
    private static VehicleSpawnConfigs instance;

    @Getter
    private final List<VehicleSpawnConfig> vehicleSpawnConfigs = new ArrayList<>();

    public VehicleSpawnConfigs(String name) {
        super(name);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Object[] getObjectsToSave() {
        return new Object[]{vehicleSpawnConfigs};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        vehicleSpawnConfigs.clear();
        vehicleSpawnConfigs.addAll((List<VehicleSpawnConfig>) objects[0]);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTSerializer.unserialize(nbt.getCompoundTag("vehicles"), this);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTBase base = NBTSerializer.serialize(this);
        compound.setTag("vehicles", base);
        return compound;
    }

    @SubscribeEvent
    public static void load(WorldEvent.Load event) {
        if (event.getWorld().provider.getDimensionType() == DimensionType.OVERWORLD && !event.getWorld().isRemote) {
            try {
                instance = (VehicleSpawnConfigs) event.getWorld().getPerWorldStorage().getOrLoadData(VehicleSpawnConfigs.class, "GtwNpcVehicleSpawnConfigs");
            } catch (Exception e) {
                instance = null;
                GtwNpcMod.log.fatal("Cannot load saved vehicle spawn configs", e);
            }
            if (instance == null) {
                instance = new VehicleSpawnConfigs("GtwNpcVehicleSpawnConfigs");
                event.getWorld().getPerWorldStorage().setData("GtwNpcVehicleSpawnConfigs", instance);
            }
        }
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        if (event.getWorld().provider.getDimensionType() == DimensionType.OVERWORLD && instance != null && !event.getWorld().isRemote) {
            instance.vehicleSpawnConfigs.clear();
            instance = null;
        }
    }

    public VehicleSpawnConfig getVehicleSpawnConfig(Random rand, VehicleSpawnConfig.VehicleType type) {
        return WeightedRandom.getRandomItem(rand, vehicleSpawnConfigs.stream().filter(config -> config.getVehicleType() == type).collect(Collectors.toList()));
    }
}

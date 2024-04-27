package fr.aym.gtwnpc.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.aym.gtwnpc.GtwNpcMod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.*;

public class GtwNpcsConfig {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(SpawningConfig.CitizenSpawningConfig.class, SpawningConfig.CitizenSpawningConfig.adapter)
            .registerTypeAdapter(SpawningConfig.PoliceSpawningConfig.class, SpawningConfig.PoliceSpawningConfig.adapter)
            .registerTypeAdapter(VehiclesSpawningRules.class, VehiclesSpawningRules.adapter)
            .registerTypeAdapter(VehiclesSpawningRatios.class, VehiclesSpawningRatios.adapter)
            .create();
    private static File configFile;

    public static BaseConfig config;
    public static SpawningConfig.CitizenSpawningConfig citizenSpawningConfig;
    public static SpawningConfig.PoliceSpawningConfig policeSpawningConfig;
    public static VehiclesSpawningRules vehiclesSpawningRules;
    public static VehiclesSpawningRatios vehiclesSpawningRatios;

    public static void load(File configFile) {
        GtwNpcsConfig.configFile = configFile;
        if (configFile.exists()) {
            try {
                config = gson.fromJson(new FileReader(configFile), BaseConfig.class);
                citizenSpawningConfig = config.getCitizenSpawningConfig();
                policeSpawningConfig = config.getPoliceSpawningConfig();
                vehiclesSpawningRules = config.getVehiclesSpawningRules();
                vehiclesSpawningRatios = config.getVehiclesSpawningRatios();
                if(config.getVersion() != 3 || vehiclesSpawningRules == null || vehiclesSpawningRatios == null) {
                    if(vehiclesSpawningRules == null) // Maj from v1 to v2: create default
                        config.setVehiclesSpawningRules(new VehiclesSpawningRules(true, 100, 100,
                                20, 50, 40));
                    if(vehiclesSpawningRatios == null) // Maj from v2 to v3: create default
                        config.setVehiclesSpawningRatios(new VehiclesSpawningRatios());
                    GtwNpcMod.log.warn("Updating config file to version 3");
                    config.setVersion(3);
                    save();
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        if (config == null) { //Error or not existing: create default
            citizenSpawningConfig = new SpawningConfig.CitizenSpawningConfig(60, 20, 1, 20, 42, 128);
            policeSpawningConfig = new SpawningConfig.PoliceSpawningConfig(80, 10, 1, new int[]{1, 40, 50, 60, 70, 80}, 40, 100, new int[]{4, 8, 14, 20, 30});
            vehiclesSpawningRules = new VehiclesSpawningRules(true, 100, 100, 20, 50, 40);
            vehiclesSpawningRatios = new VehiclesSpawningRatios();
            config = new BaseConfig(1, true, 60000, 1000, 0.45f, 0.60f,
                    0.8f, 50, 0.65f, 2, 4, 20, 60,
                    citizenSpawningConfig, policeSpawningConfig, vehiclesSpawningRules, vehiclesSpawningRatios);
            save();
            GtwNpcMod.log.info("Config file created at " + configFile.getAbsolutePath());
        }
    }

    public static void reload() {
        load(configFile);
    }

    public static void save() {
        try {
            FileWriter wri = new FileWriter(configFile);
            gson.toJson(config, wri);
            wri.flush();
            wri.close();
            citizenSpawningConfig = config.getCitizenSpawningConfig();
            policeSpawningConfig = config.getPoliceSpawningConfig();
            vehiclesSpawningRules = config.getVehiclesSpawningRules();
            vehiclesSpawningRatios = config.getVehiclesSpawningRatios();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config", e);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseConfig {
        private int version;
        private boolean enableSpawning;
        private int idleTimeDespawn = 60000;
        private int maxNpcs;
        private float minNpcMoveSpeed;
        private float maxNpcMoveSpeed;
        private float panicMoveSpeed;
        private int attackBackChance;
        private float attackingMoveSpeed;
        private float attackDamage;
        private float attackSpeed;
        private float npcHealth;
        private int playerHideCooldown = 60;
        private SpawningConfig.CitizenSpawningConfig citizenSpawningConfig;
        private SpawningConfig.PoliceSpawningConfig policeSpawningConfig;
        private VehiclesSpawningRules vehiclesSpawningRules;
        private VehiclesSpawningRatios vehiclesSpawningRatios;
    }
}

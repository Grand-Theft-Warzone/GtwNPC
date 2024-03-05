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
            .create();
    private static File configFile;

    public static BaseConfig config;
    public static SpawningConfig.CitizenSpawningConfig citizenSpawningConfig;
    public static SpawningConfig.PoliceSpawningConfig policeSpawningConfig;
    public static VehiclesSpawningRules vehiclesSpawningRules;

    public static void load(File configFile) {
        GtwNpcsConfig.configFile = configFile;
        if (configFile.exists()) {
            try {
                config = gson.fromJson(new FileReader(configFile), BaseConfig.class);
                citizenSpawningConfig = config.getCitizenSpawningConfig();
                policeSpawningConfig = config.getPoliceSpawningConfig();
                vehiclesSpawningRules = config.getVehiclesSpawningRules();
                if(config.getVersion() != 2 || vehiclesSpawningRules == null) {
                    if(vehiclesSpawningRules == null) // Maj from v1 to v2: create default
                        config.setVehiclesSpawningRules(new VehiclesSpawningRules(true, 100, 100,
                                20, 50, 40));
                    GtwNpcMod.log.warn("Updating config file to version 2");
                    config.setVersion(2);
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
            config = new BaseConfig(1, true, 60000, 1000, 0.45f, 0.60f,
                    0.8f, 50, 0.65f, 2, 4, 20,
                    citizenSpawningConfig, policeSpawningConfig, vehiclesSpawningRules);
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
        private SpawningConfig.CitizenSpawningConfig citizenSpawningConfig;
        private SpawningConfig.PoliceSpawningConfig policeSpawningConfig;
        private VehiclesSpawningRules vehiclesSpawningRules;
    }
}

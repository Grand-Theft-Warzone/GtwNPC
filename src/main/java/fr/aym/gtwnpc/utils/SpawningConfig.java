package fr.aym.gtwnpc.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.entity.EntityGtwPoliceNpc;
import fr.aym.gtwnpc.player.PlayerInformation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.function.BiFunction;

@Getter
@Setter
@AllArgsConstructor
public abstract class SpawningConfig {
    private final Class<? extends EntityLiving> entityClass;
    private final BiFunction<World, PlayerInformation, EntityLiving> entityFactory;
    private int npcsLimitRadius;
    private int npcsLimit;
    private int npcsSpawnLimit;
    private int npcSpawnChance;
    private int minSpawnRadius;
    private int maxSpawnRadius;

    public static class CitizenSpawningConfig extends SpawningConfig {
        public static TypeAdapter<SpawningConfig> adapter = new TypeAdapter<SpawningConfig>() {
            @Override
            public void write(JsonWriter out, SpawningConfig value) throws IOException {
                out.beginObject();
                out.name("npcsLimitRadius").value(value.getNpcsLimitRadius());
                out.name("npcsLimit").value(value.getNpcsLimit());
                out.name("npcSpawnChance").value(value.getNpcSpawnChance());
                out.name("npcsSpawnLimit").value(value.getNpcsSpawnLimit());
                out.name("minSpawnRadius").value(value.getMinSpawnRadius());
                out.name("maxSpawnRadius").value(value.getMaxSpawnRadius());
                out.endObject();
            }

            @Override
            public SpawningConfig read(JsonReader in) throws IOException {
                in.beginObject();
                int npcsLimitRadius = 0;
                int npcsLimit = 0;
                int npcSpawnChance = 0;
                int npcsSpawnLimit = 0;
                int minSpawnRadius = 0;
                int maxSpawnRadius = 0;
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "npcsLimitRadius":
                            npcsLimitRadius = in.nextInt();
                            break;
                        case "npcsLimit":
                            npcsLimit = in.nextInt();
                            break;
                        case "npcSpawnChance":
                            npcSpawnChance = in.nextInt();
                            break;
                        case "npcsSpawnLimit":
                            npcsSpawnLimit = in.nextInt();
                            break;
                        case "minSpawnRadius":
                            minSpawnRadius = in.nextInt();
                            break;
                        case "maxSpawnRadius":
                            maxSpawnRadius = in.nextInt();
                            break;
                    }
                }
                in.endObject();
                return new CitizenSpawningConfig(npcsLimitRadius, npcsLimit, npcsSpawnLimit, npcSpawnChance, minSpawnRadius, maxSpawnRadius);
            }
        };

        public CitizenSpawningConfig(int npcsLimitRadius, int npcsLimit, int npcsSpawnLimit, int npcSpawnChance, int minSpawnRadius, int maxSpawnRadius) {
            super(EntityGtwNpc.class, (w, p) -> new EntityGtwNpc(w), npcsLimitRadius, npcsLimit, npcsSpawnLimit, npcSpawnChance, minSpawnRadius, maxSpawnRadius);
        }
    }

    @Getter
    @Setter
    public static class PoliceSpawningConfig extends SpawningConfig {

        public static TypeAdapter<SpawningConfig> adapter = new TypeAdapter<SpawningConfig>() {
            @Override
            public void write(JsonWriter out, SpawningConfig value) throws IOException {
                out.beginObject();
                out.name("npcsLimit").value(value.getNpcsLimit());
                out.name("npcsLimitRadius").value(value.getNpcsLimitRadius());
                out.name("npcsSpawnLimit").value(value.getNpcsSpawnLimit());
                out.name("minSpawnRadius").value(value.getMinSpawnRadius());
                out.name("maxSpawnRadius").value(value.getMaxSpawnRadius());
                out.name("npcSpawnChances").beginArray();
                for (int i : ((PoliceSpawningConfig) value).getNpcSpawnChances()) {
                    out.value(i);
                }
                out.endArray();
                out.name("maxTrackingPolicemen").beginArray();
                for (int i : ((PoliceSpawningConfig) value).getMaxTrackingPolicemen()) {
                    out.value(i);
                }
                out.endArray();
                out.endObject();
            }

            @Override
            public SpawningConfig read(JsonReader in) throws IOException {
                in.beginObject();
                int npcsLimit = 0;
                int npcsLimitRadius = 0;
                int npcsSpawnLimit = 0;
                int minSpawnRadius = 0;
                int maxSpawnRadius = 0;
                int[] npcSpawnChances = new int[6];
                int[] maxTrackingPolicemen = new int[6];
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "npcsLimit":
                            npcsLimit = in.nextInt();
                            break;
                        case "npcsLimitRadius":
                            npcsLimitRadius = in.nextInt();
                            break;
                        case "npcsSpawnLimit":
                            npcsSpawnLimit = in.nextInt();
                            break;
                        case "minSpawnRadius":
                            minSpawnRadius = in.nextInt();
                            break;
                        case "maxSpawnRadius":
                            maxSpawnRadius = in.nextInt();
                            break;
                        case "npcSpawnChances":
                            in.beginArray();
                            for (int i = 0; i < 6; i++) {
                                npcSpawnChances[i] = in.nextInt();
                            }
                            in.endArray();
                            break;
                        case "maxTrackingPolicemen":
                            in.beginArray();
                            for (int i = 0; i < 6; i++) {
                                maxTrackingPolicemen[i] = in.nextInt();
                            }
                            in.endArray();
                            break;
                    }
                }
                in.endObject();
                return new PoliceSpawningConfig(npcsLimitRadius, npcsLimit, npcsSpawnLimit, npcSpawnChances, minSpawnRadius, maxSpawnRadius, maxTrackingPolicemen);
            }
        };

        private int[] npcSpawnChances;
        private int[] maxTrackingPolicemen;

        public PoliceSpawningConfig(int npcsLimitRadius, int npcsLimit, int npcsSpawnLimit, int[] npcSpawnChances, int minSpawnRadius, int maxSpawnRadius, int[] maxTrackingPolicemen) {
            super(EntityGtwNpc.class, EntityGtwPoliceNpc::new, npcsLimitRadius, npcsLimit, npcsSpawnLimit, 0, minSpawnRadius, maxSpawnRadius);
            this.npcSpawnChances = npcSpawnChances;
            this.maxTrackingPolicemen = maxTrackingPolicemen;
        }

        public void setWantedLevel(int level) {
            setNpcSpawnChance(npcSpawnChances[level]);
        }
    }
}

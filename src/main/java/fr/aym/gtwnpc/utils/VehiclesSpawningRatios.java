
package fr.aym.gtwnpc.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.aym.gtwnpc.dynamx.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.util.WeightedRandom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehiclesSpawningRatios {
    public static TypeAdapter<VehiclesSpawningRatios> adapter = new TypeAdapter<VehiclesSpawningRatios>() {
        @Override
        public void write(JsonWriter out, VehiclesSpawningRatios value) throws IOException {
            out.beginObject();
            out.name("civilianRatio");
            out.beginArray();
            for (float f : value.civilianRatio) {
                out.value(f);
            }
            out.endArray();
            out.name("policeRatio");
            out.beginArray();
            for (float f : value.policeRatio) {
                out.value(f);
            }
            out.endArray();
            out.name("swatRatio");
            out.beginArray();
            for (float f : value.swatRatio) {
                out.value(f);
            }
            out.endArray();
            out.name("militaryRatio");
            out.beginArray();
            for (float f : value.militaryRatio) {
                out.value(f);
            }
            out.endArray();
            out.endObject();
        }

        @Override
        public VehiclesSpawningRatios read(JsonReader in) throws IOException {
            in.beginObject();
            float[] civilianRatio = new float[6];
            float[] policeRatio = new float[6];
            float[] swatRatio = new float[6];
            float[] militaryRatio = new float[6];
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "civilianRatio":
                        in.beginArray();
                        for (int i = 0; i < 6; i++) {
                            if(!in.hasNext()) //sixth element was added later
                                civilianRatio[i] = 0.15f;
                            civilianRatio[i] = (float) in.nextDouble();
                        }
                        in.endArray();
                        break;
                    case "policeRatio":
                        in.beginArray();
                        for (int i = 0; i < 6; i++) {
                            if(!in.hasNext()) //sixth element was added later
                                civilianRatio[i] = 0.15f;
                            policeRatio[i] = (float) in.nextDouble();
                        }
                        in.endArray();
                        break;
                    case "swatRatio":
                        in.beginArray();
                        for (int i = 0; i < 6; i++) {
                            if(!in.hasNext()) //sixth element was added later
                                civilianRatio[i] = 0.35f;
                            swatRatio[i] = (float) in.nextDouble();
                        }
                        in.endArray();
                        break;
                    case "militaryRatio":
                        in.beginArray();
                        for (int i = 0; i < 6; i++) {
                            if(!in.hasNext()) //sixth element was added later
                                civilianRatio[i] = 0.35f;
                            militaryRatio[i] = (float) in.nextDouble();
                        }
                        in.endArray();
                        break;
                }
            }
            in.endObject();
            return new VehiclesSpawningRatios(civilianRatio, policeRatio, swatRatio, militaryRatio, null);
        }
    };

    private float[] civilianRatio = new float[]{0.95f, 0.6f, 0.5f, 0.35f, 0.2f, 0.15f};
    private float[] policeRatio = new float[]{0.05f, 0.4f, 0.5f, 0.35f, 0.2f, 0.15f};
    private float[] swatRatio = new float[]{0.0f, 0.0f, 0f, 0.3f, 0.3f, 0.35f};
    private float[] militaryRatio = new float[]{0.0f, 0.0f, 0f, 0f, 0.3f, 0.35f};

    private Map<Integer, List<SpawnRatio>> ratios;

    public List<SpawnRatio> getVehicleRatios(int wantedLevel) {
        if (ratios == null) {
            ratios = new HashMap<>();
            List<SpawnRatio> ratios;
            for (int i = 0; i < 6; i++) {
                ratios = new ArrayList<>();
                ratios.add(new SpawnRatio((int) (civilianRatio[i] * 100), VehicleType.CIVILIAN));
                ratios.add(new SpawnRatio((int) (policeRatio[i] * 100), VehicleType.POLICE));
                ratios.add(new SpawnRatio((int) (swatRatio[i] * 100), VehicleType.SWAT));
                ratios.add(new SpawnRatio((int) (militaryRatio[i] * 100), VehicleType.MILITARY));
                this.ratios.put(i, ratios);
            }
        }
        return ratios.get(wantedLevel);
    }

    @Getter
    public static class SpawnRatio extends WeightedRandom.Item {
        private final VehicleType type;

        public SpawnRatio(int weight, VehicleType type) {
            super(weight);
            this.type = type;
        }
    }
}

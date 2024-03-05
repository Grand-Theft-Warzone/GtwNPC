package fr.aym.gtwnpc.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter
@Setter
@AllArgsConstructor
public class VehiclesSpawningRules
{
    public static TypeAdapter<VehiclesSpawningRules> adapter = new TypeAdapter<VehiclesSpawningRules>() {
        @Override
        public void write(JsonWriter out, VehiclesSpawningRules value) throws IOException {
            out.beginObject();
            out.name("isVehicleSpawningEnabled").value(value.isVehicleSpawningEnabled);
            out.name("vehicleSpawningRadius").value(value.vehicleSpawningRadius);
            out.name("vehicleSpawningChance").value(value.vehicleSpawningChance);
            out.name("vehicleSpawningLimit").value(value.vehicleSpawningLimit);
            out.name("vehicleDespawningRadius").value(value.vehicleDespawningRadius);
            out.name("spawnRadiusMin").value(value.spawnRadiusMin);
            out.endObject();
        }

        @Override
        public VehiclesSpawningRules read(JsonReader in) throws IOException {
            in.beginObject();
            boolean isVehicleSpawningEnabled = false;
            int vehicleSpawningRadius = 100;
            int vehicleSpawningChance = 100;
            int vehicleSpawningLimit = 20;
            int vehicleDespawningRadius = 50;
            int spawnRadiusMin = 40;
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "isVehicleSpawningEnabled":
                        isVehicleSpawningEnabled = in.nextBoolean();
                        break;
                    case "vehicleSpawningRadius":
                        vehicleSpawningRadius = in.nextInt();
                        break;
                    case "vehicleSpawningChance":
                        vehicleSpawningChance = in.nextInt();
                        break;
                    case "vehicleSpawningLimit":
                        vehicleSpawningLimit = in.nextInt();
                        break;
                    case "vehicleDespawningRadius":
                        vehicleDespawningRadius = in.nextInt();
                        break;
                    case "spawnRadiusMin":
                        spawnRadiusMin = in.nextInt();
                        break;
                }
            }
            in.endObject();
            return new VehiclesSpawningRules(isVehicleSpawningEnabled, vehicleSpawningRadius,
                    vehicleSpawningChance, vehicleSpawningLimit, vehicleDespawningRadius, spawnRadiusMin);
        }
    };

    private boolean isVehicleSpawningEnabled = false;
    private int vehicleSpawningRadius = 100;
    private int vehicleSpawningChance = 100;
    private int vehicleSpawningLimit = 20;
    private int vehicleDespawningRadius = 50;
    private int spawnRadiusMin = 40;
}

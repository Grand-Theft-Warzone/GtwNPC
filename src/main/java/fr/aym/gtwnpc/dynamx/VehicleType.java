package fr.aym.gtwnpc.dynamx;

public enum VehicleType {
    CIVILIAN,
    POLICE,
    SWAT,
    MILITARY;

    public boolean isPolice() {
        return this == POLICE || this == SWAT || this == MILITARY;
    }
}

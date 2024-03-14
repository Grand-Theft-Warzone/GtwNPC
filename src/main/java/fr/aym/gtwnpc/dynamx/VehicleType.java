package fr.aym.gtwnpc.dynamx;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import lombok.Getter;

@Getter
public enum VehicleType {
    CIVILIAN(SkinRepository.NpcType.NPC),
    POLICE(SkinRepository.NpcType.POLICE),
    SWAT(SkinRepository.NpcType.SWAT),
    MILITARY(SkinRepository.NpcType.MILITARY);

    private final SkinRepository.NpcType npcType;

    VehicleType(SkinRepository.NpcType npcType) {
        this.npcType = npcType;
    }

    public boolean isPolice() {
        return this == POLICE || this == SWAT || this == MILITARY;
    }
}

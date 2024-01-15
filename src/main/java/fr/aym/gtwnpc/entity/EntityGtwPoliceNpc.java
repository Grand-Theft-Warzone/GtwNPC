package fr.aym.gtwnpc.entity;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import net.minecraft.world.World;

public class EntityGtwPoliceNpc extends EntityGtwNpc {
    public EntityGtwPoliceNpc(World worldIn) {
        super(worldIn);
        setNpcType(SkinRepository.NpcType.POLICE);
        setFriendly(false);
    }

    public EntityGtwPoliceNpc(World worldIn, PlayerInformation information) {
        super(worldIn);
        setNpcType(SkinRepository.NpcType.POLICE);
        setFriendly(false);
        if (information != null && information.getWantedLevel() > 0 && information.getTrackingPolicemen().size() < GtwNpcsConfig.policeSpawningConfig.getMaxTrackingPolicemen()[information.getWantedLevel()]) {
            policeTargetAI.setTargetEntity(information.getPlayerIn());
            information.getTrackingPolicemen().add(this);
            setState("tracking_wanted");
        }
    }
}

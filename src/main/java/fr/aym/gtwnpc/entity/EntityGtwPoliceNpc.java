package fr.aym.gtwnpc.entity;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import net.minecraft.world.World;

public class EntityGtwPoliceNpc extends EntityGtwNpc{
    public EntityGtwPoliceNpc(World worldIn) {
        super(worldIn);
        setNpcType(SkinRepository.NpcType.POLICE);
        setFriendly(false);
        System.out.println("Police spawned");
    }
}

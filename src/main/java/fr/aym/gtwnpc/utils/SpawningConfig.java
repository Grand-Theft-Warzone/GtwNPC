package fr.aym.gtwnpc.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

import java.util.function.Function;

@Getter
@Setter
@AllArgsConstructor
public class SpawningConfig
{
    private Class<? extends EntityLiving> entityClass;
    private Function<World, EntityLiving> entityFactory;
    private int npcsLimitRadius; //TODO integrate
    private int npcsLimit; //integrate
    private int npcsSpawnLimit; //integrate
    private int npcSpawnChance; //integrate

    public static class PoliceSpawningConfig extends SpawningConfig {
        @Getter
        @Setter
        private int[] npcSpawnChances;

        public PoliceSpawningConfig(Class<? extends EntityLiving> entityClass, Function<World, EntityLiving> entityFactory, int npcsLimitRadius, int npcsLimit, int npcsSpawnLimit, int[] npcSpawnChances) {
            super(entityClass, entityFactory, npcsLimitRadius, npcsLimit, npcsSpawnLimit, 0);
            this.npcSpawnChances = npcSpawnChances;
        }

        public void setWantedLevel(int level) {
            setNpcSpawnChance(npcSpawnChances[level]);
        }
    }
}

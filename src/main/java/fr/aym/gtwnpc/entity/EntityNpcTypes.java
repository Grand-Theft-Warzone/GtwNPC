package fr.aym.gtwnpc.entity;

import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import lombok.Getter;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EntityNpcTypes
{
    @Getter
    private static EnumCreatureType npcType, policeNpcType;

    public static void init() {
        List<String> excludedBiomes = Arrays.asList("void", "hell", "sky", "river", "ocean", "ice");
        List<Biome> biomes = new ArrayList<>();
        Biome.REGISTRY.forEach(b -> {
            if (excludedBiomes.stream().noneMatch(biome -> b.getBiomeName().contains(biome)))
                biomes.add(b);
        });
        Biome[] biomesArray = biomes.toArray(new Biome[0]);
        npcType = EnumHelper.addCreatureType("npc", EntityGtwNpc.class, GtwNpcsConfig.spawnClusterSize, Material.AIR, true, false);
        EntityRegistry.addSpawn(EntityGtwNpc.class, 20, 1, 1, npcType, biomesArray);
        //policeNpcType = EnumHelper.addCreatureType("npc_police", EntityGtwPoliceNpc.class, GtwNpcsConfig.policeSpawnClusterSize, Material.AIR, true, false);
        EntityRegistry.addSpawn(EntityGtwPoliceNpc.class, 80, 1, 4, npcType, biomesArray);
    }
}

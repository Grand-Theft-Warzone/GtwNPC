package fr.aym.gtwnpc.client.skin;

import fr.aym.gtwnpc.utils.GtwNpcConstants;
import lombok.Getter;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.File;
import java.util.*;

public class SkinRepository {
    private static final Map<NpcType, List<ResourceLocation>> SKINS = new HashMap<>();

    public static void loadSkins(File skinsFolder) {
        skinsFolder.mkdirs();
        loadDirectory(FMLCommonHandler.instance().getSide().isClient(), skinsFolder, "skin/", null);
    }

    private static void loadDirectory(boolean isClient, File directory, String path, NpcType type) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                NpcType stype = type == null ? NpcType.valueOf(file.getName().split("_")[0].toUpperCase()) : type;
                loadDirectory(isClient, file, path + file.getName() + "/", stype);
            } else {
                if (type == null)
                    throw new IllegalArgumentException("Cannot find skin type for " + file.getPath());
                if (file.getName().endsWith(".png")) {
                    ResourceLocation location = new ResourceLocation(GtwNpcConstants.ID, path + file.getName());
                    SKINS.get(type).add(location);
                    if(isClient)
                        new SkinTexture(location, file);
                    System.out.println("Loaded skin " + location);
                }
            }
        }
    }

    public static Map<NpcType, List<ResourceLocation>> getSkins() {
        return SKINS;
    }

    public static Collection<ResourceLocation> getSkinsOfType(NpcType type) {
        return SKINS.get(type);
    }

    public static ResourceLocation getRandomSkin(NpcType type, Random random) {
        if(SKINS.get(type).isEmpty())
            return new ResourceLocation("textures/entity/steve.png");
        return SKINS.get(type).get(random.nextInt(SKINS.get(type).size()));
    }

    @Getter
    public enum NpcType {
        NPC("npc"),
        POLICE("police"),
        SWAT("swat"),
        MILITARY("military");

        private final String name;

        NpcType(String name) {
            this.name = name;
            SKINS.put(this, new ArrayList<>());
        }
    }
}

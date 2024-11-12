package fr.aym.gtwnpc.impl;

import fr.aym.acslib.api.services.mps.IMpsClassLoader;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.dynamx.AutopilotModule;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class ProtectionStarter {
    public ProtectionStarter(IMpsClassLoader loader) {
        GtwNpcMod.log.info("Protection loader starting");
        //GtwNpcMod.isValidConfig = true;
        //Get MC's game dir
        File f = null;
        try {
            f = FMLCommonHandler.instance().getSide().isClient() ? getMcClientDir() : getMcServerDir();
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        String path = f.getAbsolutePath();
        if (path.endsWith(".")) {
            path = path.substring(0, path.length() - 2);
        }
        path = path.substring(path.lastIndexOf(File.separator) + File.separator.length());
        path = path.replace(".", "");
        GtwNpcMod.isValidConfig = FMLCommonHandler.instance().getSide().isServer()
                || path.toLowerCase().contains("official-grand-theft-warzone") || path.toLowerCase().contains("die_minewache");
        EntityGtwNpc.moveToNodesAiFactory = GEntityAIMoveToNodes::new;
        AutopilotModule.obstacleDetectionFactory = ObstacleDetection::new;
        GtwNpcMod.log.info("Protection loader started");
    }

    private File getMcClientDir() throws InvocationTargetException, IllegalAccessException {
        return (File) ObfuscationReflectionHelper.findField(Minecraft.class, "field_71412_D").get(
                ObfuscationReflectionHelper.findMethod(Minecraft.class, "func_71410_x", Minecraft.class).invoke(null));
    }

    private File getMcServerDir() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory();
    }
}

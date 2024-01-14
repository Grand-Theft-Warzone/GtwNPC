package fr.aym.gtwnpc;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.common.CommonProxy;
import fr.aym.gtwnpc.common.GtwNpcsItems;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.entity.EntityGtwPoliceNpc;
import fr.aym.gtwnpc.entity.EntityNpcTypes;
import fr.aym.gtwnpc.network.BBMessagePathNodes;
import fr.aym.gtwnpc.server.CommandGtwNpcConfig;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.aym.gtwnpc.utils.GtwNpcConstants.*;

@Mod(modid = ID, name = NAME, version = VERSION, dependencies = "required-after:sqript@1.0")
public class GtwNpcMod {
    @Mod.Instance(value = ID)
    public static GtwNpcMod instance;

    public static SimpleNetworkWrapper network;

    @SidedProxy(serverSide = "fr.aym.gtwnpc.server.ServerProxy", clientSide = "fr.aym.gtwnpc.client.ClientProxy")
    public static CommonProxy proxy;

    public static final Logger log = LogManager.getLogger("GtwNpcMod");

    public GtwNpcMod() {
        GtwNpcsItems.registerItems();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityRegistry.registerModEntity(new ResourceLocation(ID, "entity_gtw_npc"), EntityGtwNpc.class, "entity_gtw_npc", 1, this, 80, 3, false, new Color(0, 255, 0).getRGB(), new Color(255, 0, 0).getRGB());
        EntityRegistry.registerModEntity(new ResourceLocation(ID, "entity_police_gtw_npc"), EntityGtwPoliceNpc.class, "entity_police_gtw_npc", 1, this, 80, 3, false, new Color(0, 255, 0).getRGB(), new Color(0, 0, 255).getRGB());
        proxy.preInit(event);
        GtwNpcsConfig.load(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(ID + ".ch");
        network.registerMessage(BBMessagePathNodes.HandlerClient.class, BBMessagePathNodes.class, 1, Side.CLIENT);
        network.registerMessage(BBMessagePathNodes.HandlerServer.class, BBMessagePathNodes.class, 2, Side.SERVER);

        SkinRepository.loadSkins(new File("GtwNpc", "skins"));
    }

    @Mod.EventHandler
    public void postInit(FMLInitializationEvent event) {
        //EntityNpcTypes.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event. registerServerCommand(new CommandGtwNpcConfig());
    }
}

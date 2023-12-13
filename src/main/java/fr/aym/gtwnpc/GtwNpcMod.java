package fr.aym.gtwnpc;

import fr.aym.gtwnpc.common.CommonProxy;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

import static fr.aym.gtwnpc.utils.GtwNpcConstants.*;

@Mod(modid = ID, name = NAME, version = VERSION, dependencies = "required-after:sqript@1.0")
public class GtwNpcMod {
    @Mod.Instance(value = ID)
    public static GtwNpcMod instance;

    @SidedProxy(serverSide = "fr.aym.gtwnpc.server.ServerProxy", clientSide = "fr.aym.gtwnpc.client.ClientProxy")
    public static CommonProxy proxy;

    public static final Logger log = LogManager.getLogger("GtwNpcMod");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityRegistry.registerModEntity(new ResourceLocation(ID, "entity_gtw_npc"), EntityGtwNpc.class, "entity_gtw_npc", 1, this, 80, 3, false, new Color(0, 255, 0).getRGB(), new Color(255, 0, 0).getRGB());
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

    }
}

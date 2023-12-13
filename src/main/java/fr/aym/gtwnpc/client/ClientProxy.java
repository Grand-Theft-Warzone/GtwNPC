package fr.aym.gtwnpc.client;

import fr.aym.gtwnpc.client.render.RenderGtwNpc;
import fr.aym.gtwnpc.common.CommonProxy;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityGtwNpc.class, RenderGtwNpc::new);
    }
}

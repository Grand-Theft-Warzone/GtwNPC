package fr.aym.gtwnpc.network;

import fr.aym.gtwnpc.client.ClientEventHandler;
import fr.aym.gtwnpc.dynamx.AutopilotModule;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SCMessagePutAutopilot implements IMessage {
    private int entityId;

    public SCMessagePutAutopilot() {
    }

    public SCMessagePutAutopilot(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    public static class HandleClient implements IMessageHandler<SCMessagePutAutopilot, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SCMessagePutAutopilot message, MessageContext ctx) {
            ClientEventHandler.MC.addScheduledTask(() -> {
                if(ClientEventHandler.MC.world == null) {
                    return;
                }
                Entity e = ClientEventHandler.MC.world.getEntityByID(message.entityId);
                if(!(e instanceof BaseVehicleEntity)) {
                    return;
                }
                BaseVehicleEntity<?> vehicle = (BaseVehicleEntity<?>) e;
                if(vehicle.initialized == PhysicsEntity.EnumEntityInitState.NOT_INITIALIZED) {
                    vehicle.setInitCallback((entity, modules) -> {
                        CarEngineModule module = entity.getModuleByType(CarEngineModule.class);
                        modules.removeIf(m -> m instanceof CarEngineModule);
                        modules.add(new AutopilotModule((BaseVehicleEntity<?>) entity, module));
                    });
                } else {
                    CarEngineModule module = vehicle.getModuleByType(CarEngineModule.class);
                    vehicle.getModules().removeIf(m -> m instanceof CarEngineModule);
                    vehicle.getModules().add(new AutopilotModule(vehicle, module));
                }
            });
            return null;
        }
    }
}

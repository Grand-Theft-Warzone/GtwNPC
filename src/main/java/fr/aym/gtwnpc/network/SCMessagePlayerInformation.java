package fr.aym.gtwnpc.network;

import fr.aym.gtwmap.api.GtwMapApi;
import fr.aym.gtwmap.api.ITrackableObject;
import fr.aym.gtwnpc.client.ClientEventHandler;
import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.dynamx.common.entities.BaseVehicleEntity;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class SCMessagePlayerInformation implements IMessage {
    private int wantedLevel;
    private int hiddenTime;

    public SCMessagePlayerInformation() {
    }

    public SCMessagePlayerInformation(int wantedLevel, int hiddenTime) {
        this.wantedLevel = wantedLevel;
        this.hiddenTime = hiddenTime;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        wantedLevel = buf.readInt();
        hiddenTime = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(wantedLevel);
        buf.writeInt(hiddenTime);
    }

    public int getWantedLevel() {
        return wantedLevel;
    }

    public int getHiddenTime() {
        return hiddenTime;
    }

    public static class Handler implements IMessageHandler<SCMessagePlayerInformation, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SCMessagePlayerInformation message, MessageContext ctx) {
            PlayerInformation info = PlayerManager.getPlayerInformation(ClientEventHandler.MC.player);
            boolean wasTracked = info.getWantedLevel() > 0;
          //  System.out.println("Received wanted level " + message.getWantedLevel() + " and hidden time " + message.getHiddenTime() + " Was: " + wasTracked);
            if (message.getWantedLevel() > 0) {
                GtwMapApi.setRenderPoliceBlinking(message.getHiddenTime() == 0);
                if (!wasTracked) {
                    List<EntityGtwNpc> npcs = ClientEventHandler.MC.world.getEntities(EntityGtwNpc.class, e -> {
                        //System.out.println("Checking npc " + e + " " + e.getNpcType());
                        return e.getNpcType().isPolice();
                    });
                 //   System.out.println("Found " + npcs.size() + " police npcs");
                    npcs.forEach(npc -> GtwMapApi.addTrackedObject(new ITrackableObject.TrackedEntity(npc, "Police", "player_white") {
                        @Override
                        public int renderPoliceCircleAroundRadius() {
                            return 30;
                        }
                    }));
                    List<BaseVehicleEntity> vehicles = ClientEventHandler.MC.world.getEntities(BaseVehicleEntity.class, e -> e.hasModuleOfType(GtwNpcModule.class) && ((GtwNpcModule) e.getModuleByType(GtwNpcModule.class)).getVehicleType().isPolice());
                 //   System.out.println("Found " + vehicles.size() + " police vehicles");
                    vehicles.forEach(vehicle -> GtwMapApi.addTrackedObject(new ITrackableObject.TrackedEntity(vehicle, "Police", "car_white") {
                        @Override
                        public int renderPoliceCircleAroundRadius() {
                            return 60;
                        }
                    }));
                }
            } else {
                GtwMapApi.setRenderPoliceBlinking(false);
                GtwMapApi.getTrackedObjects().removeIf(o -> o.getDisplayName().equals("Police"));
            }
         //   System.out.println("Tracked objects: " + GtwMapApi.getTrackedObjects());
            info.setWantedLevel(message.getWantedLevel());
            info.setHiddenTime(message.getHiddenTime());
            return null;
        }
    }
}

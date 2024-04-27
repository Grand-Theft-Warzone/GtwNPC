package fr.aym.gtwnpc.network;

import fr.aym.gtwnpc.client.ClientEventHandler;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
            info.setWantedLevel(message.getWantedLevel());
            info.setHiddenTime(message.getHiddenTime());
            return null;
        }
    }
}

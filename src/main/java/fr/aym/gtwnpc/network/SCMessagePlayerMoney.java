package fr.aym.gtwnpc.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SCMessagePlayerMoney implements IMessage {
    private double money;

    public SCMessagePlayerMoney() {
    }

    public SCMessagePlayerMoney(double money) {
        this.money = money;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        money = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(money);
    }

    public static class Handler implements IMessageHandler<SCMessagePlayerMoney, IMessage> {
        @Override
        public IMessage onMessage(SCMessagePlayerMoney message, MessageContext ctx) {

            return null;
        }
    }
}

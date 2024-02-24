package fr.aym.gtwnpc.network;

import fr.aym.gtwnpc.item.ItemNodes;
import fr.aym.gtwnpc.path.NodeType;
import fr.dynamx.common.items.tools.ItemSlopes;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class CSMessageSetNodeMode implements IMessage {
    private int mode;

    public CSMessageSetNodeMode() {
    }

    public CSMessageSetNodeMode(int mode) {
        this.mode = mode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mode);
    }

    public static class Handler implements IMessageHandler<CSMessageSetNodeMode, IMessage> {
        @Override
        public IMessage onMessage(CSMessageSetNodeMode message, MessageContext ctx) {
            System.out.println("Received message" + message.mode);
            if (message.mode == -15815) {
                ItemStack s = ctx.getServerHandler().player.getHeldItemMainhand();
                if (s.getItem() instanceof ItemNodes) {
                    if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
                    if (s.getTagCompound().getInteger("mode") == 5) {
                        s.getTagCompound().setInteger("mode", 0);
                        ctx.getServerHandler().player.sendMessage(new TextComponentString("Set node mode to pedestrian"));
                    } else {
                        s.getTagCompound().setInteger("mode", s.getTagCompound().getInteger("mode") + 1);
                        ctx.getServerHandler().player.sendMessage(new TextComponentString("Set node mode to " +
                                NodeType.values()[s.getTagCompound().getInteger("mode")+1].name().toLowerCase()));
                    }
                }
            }
            return null;
        }
    }
}

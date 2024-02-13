package fr.aym.gtwnpc.block;

import fr.aym.gtwnpc.server.ServerEventHandler;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import lombok.Getter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ITickable;

import javax.annotation.Nullable;

public class TETrafficLight extends TEDynamXBlock implements ITickable {
    @Getter
    private byte lightState;
    private byte mode = 4;

    public TETrafficLight() {
    }

    public TETrafficLight(BlockObject<?> packInfo) {
        super(packInfo);
    }

    @Override
    public void update() {
        super.update();
        /*
         * Timings :
         * Red : 170 ticks
         * Orange : 30 ticks
         * Green : 100 ticks
         */
        if (!world.isRemote) {
            setLightState(ServerEventHandler.getTFStateByMode(mode));
        }
    }

    public byte switchMode() {
        setLightState((byte) 3);
        switch (mode) {
            case 0:
                mode = 2;
                break;
            case 2:
                mode = 1;
                break;
            case 1:
                mode = 3;
                break;
            case 4:
                mode = 0;
                break;
            case 3:
            default:
                mode = 4;
                break;
        }
        return mode;
    }

    public void setLightState(byte lightState) {
        byte old = this.lightState;
        this.lightState = lightState;
        if (this.lightState != old) {
            IBlockState st = world.getBlockState(getPos());
            this.world.notifyBlockUpdate(getPos(), st, st, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.lightState = compound.getByte("LightState");
        this.mode = compound.getByte("Mode");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setByte("LightState", this.lightState);
        compound.setByte("Mode", this.mode);
        return compound;
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        this.writeToNBT(nbttagcompound);
        return new SPacketUpdateTileEntity(getPos(), 0, nbttagcompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
        this.world.markBlockRangeForRenderUpdate(getPos(), getPos());
    }
}

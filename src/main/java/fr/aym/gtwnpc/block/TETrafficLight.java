package fr.aym.gtwnpc.block;

import fr.aym.gtwnpc.server.ServerEventHandler;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
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
    private boolean isPed;

    public TETrafficLight() {
    }

    public TETrafficLight(BlockObject<?> packInfo, boolean isPed) {
        super(packInfo);
        this.isPed = isPed;
    }

    @Override
    public void update() {
        //FIXME super.update();
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
        if(world == null || getLightsModule() == null)
            return;
        if (this.lightState != old) {
            IBlockState st = world.getBlockState(getPos());
            this.world.notifyBlockUpdate(getPos(), st, st, 3);
        }
        AbstractLightsModule lights = getLightsModule();
        switch (getLightState()) {
            case 2:
                lights.setLightOn("red", true);
                lights.setLightOn("orange", false);
                lights.setLightOn("green", false);
                break;
            case 0:
                lights.setLightOn("red", isPed && mode != 4);
                lights.setLightOn("orange", !isPed);
                lights.setLightOn("green", false);
                break;
            case 1:
                lights.setLightOn("red", false);
                lights.setLightOn("orange", false);
                lights.setLightOn("green", true);
                break;
            default:
                lights.setLightOn("red", false);
                lights.setLightOn("orange", false);
                lights.setLightOn("green", false);
                break;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        setLightState(compound.getByte("LightState"));
        this.mode = compound.getByte("Mode");
        this.isPed = compound.getBoolean("IsPed");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setByte("LightState", this.lightState);
        compound.setByte("Mode", this.mode);
        compound.setBoolean("IsPed", this.isPed);
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

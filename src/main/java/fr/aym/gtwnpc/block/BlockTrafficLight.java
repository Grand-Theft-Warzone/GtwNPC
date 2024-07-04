package fr.aym.gtwnpc.block;

import com.jme3.math.Vector3f;
import fr.aym.gtwnpc.utils.GtwNpcConstants;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.SimplePartLightSource;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.items.DynamXItemRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockTrafficLight extends DynamXBlock<BlockObject<?>> {
    private final boolean isPed;

    public BlockTrafficLight(boolean isPed, String name) {
        super(Material.IRON, GtwNpcConstants.ID, name, new ResourceLocation(GtwNpcConstants.ID, "models/obj/" + name + "/" + name + ".obj"));
        this.isPed = isPed;
        //FIXME blockObjectInfo.setItemScale(0.8f);
        blockObjectInfo.setTranslation(new Vector3f(0, -1.5f, 0));
        setCreativeTab(DynamXItemRegistry.objectTab);
        setLightLevel(0.5f);
        SimplePartLightSource redSource = new SimplePartLightSource((ISubInfoTypeOwner<ILightOwner<?>>) blockObjectInfo, "Light_Red");
        //TODO SET ID WITH NEW DYNAMX VERSION
        redSource.setLightId("red");
        redSource.setObjectName("red");
        redSource.setBaseMaterial("red_off");
        redSource.setTextures(new String[]{"red_off", "red_on"});
        redSource.setBlinkSequence(new int[]{-1, 20});
        redSource.appendTo(blockObjectInfo);
        if (!isPed) {
            SimplePartLightSource orangeSource = new SimplePartLightSource((ISubInfoTypeOwner<ILightOwner<?>>) blockObjectInfo, "Light_Orange");
            orangeSource.setLightId("orange");
            orangeSource.setObjectName("orange");
            orangeSource.setBaseMaterial("orange_off");
            orangeSource.setTextures(new String[]{"orange_off", "orange_on"});
            orangeSource.setBlinkSequence(new int[]{-1, 20});
            orangeSource.appendTo(blockObjectInfo);
        }
        SimplePartLightSource greenSource = new SimplePartLightSource((ISubInfoTypeOwner<ILightOwner<?>>) blockObjectInfo, "Light_Green");
        greenSource.setLightId("green");
        greenSource.setObjectName("green");
        greenSource.setBaseMaterial("green_off");
        greenSource.setTextures(new String[]{"green_off", "green_on"});
        greenSource.setBlinkSequence(new int[]{-1, 20});
        greenSource.appendTo(blockObjectInfo);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TETrafficLight(this.blockObjectInfo, isPed);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (stack.getItemDamage() == 0) {
            if (worldIn.isRemote)
                placer.sendMessage(new TextComponentString("Tip: right click on my foot with another traffic light to change mode !"));
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.isSneaking())
            return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
        if (!worldIn.isRemote && Block.getBlockFromItem(playerIn.getHeldItemMainhand().getItem()) instanceof BlockTrafficLight) {
            byte mode = ((TETrafficLight) worldIn.getTileEntity(pos)).switchMode();
            playerIn.sendMessage(new TextComponentString("Active mode: " + (mode == 0 ? "Short1" : mode == 2 ? "Short2" : mode == 1 ? "Long1" : mode == 3 ? "Long2" : mode == 4 ? "Off" : "Unknown (" + mode + ")")));
            return true;
        }
        return true;
    }
}

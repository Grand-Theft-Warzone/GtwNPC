package com.silvaniastudios.roads.blocks.paint;

import com.silvaniastudios.roads.RoadsConfig;
import com.silvaniastudios.roads.blocks.BlockBase;
import com.silvaniastudios.roads.blocks.FRBlocks;
import com.silvaniastudios.roads.blocks.NonPaintRoadTopBlock;
import com.silvaniastudios.roads.blocks.PaintColour;
import com.silvaniastudios.roads.blocks.decorative.CurbBlock;
import com.silvaniastudios.roads.blocks.diagonal.RoadBlockDiagonal;
import com.silvaniastudios.roads.items.FRItems;
import java.util.Arrays;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.Block.EnumOffsetType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PaintBlockBase extends BlockBase {
    private String categoryName;
    private int[] coreMetas;
    protected boolean[] dynamic;
    private PaintColour colour;

    public PaintBlockBase(String name, String catName, int[] coreMetas, boolean[] dynamic, PaintColour colour) {
        super(name, Material.PLANTS); // Modified from fureniku's code to let npcs walk on paint blocks
        this.categoryName = catName;
        this.coreMetas = coreMetas;
        this.setHardness(2.0F);
        this.dynamic = dynamic;
        this.colour = colour;
    }

    public PaintBlockBase(String name, String catName, int[] coreMetas, boolean dynamic, PaintColour colour) {
        this(name, catName, coreMetas, (boolean[])null, colour);
        if (coreMetas != null) {
            this.dynamic = new boolean[coreMetas.length];
            if (dynamic) {
                Arrays.fill(this.dynamic, true);
            }
        }

    }

    public PaintColour getColour() {
        return this.colour;
    }

    public String getCategory() {
        return this.categoryName;
    }

    public int[] getCoreMetas() {
        return this.coreMetas;
    }

    public boolean canConnect(int id) {
        return this.dynamic[id];
    }

    public String getIconName() {
        for(int i = 0; i < FRBlocks.col.size(); ++i) {
            if (this.name.contains(((PaintColour)FRBlocks.col.get(i)).getName() + "_")) {
                return this.name.replace(((PaintColour)FRBlocks.col.get(i)).getName() + "_", "");
            }

            if (this.name.contains("_" + ((PaintColour)FRBlocks.col.get(i)).getName())) {
                return this.name.replace("_" + ((PaintColour)FRBlocks.col.get(i)).getName(), "");
            }
        }

        return "";
    }

    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /** @deprecated */
    @Deprecated
    public boolean isBlockNormalCube(IBlockState state) {
        return false;
    }

    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return ItemStack.EMPTY.getItem();
    }

    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        int closest = 0;
        if (this.getCoreMetas() == null) {
            return new ItemStack(this, 1, 0);
        } else {
            for(int i = 0; i < this.getCoreMetas().length; ++i) {
                if (this.getCoreMetas()[i] <= this.getMetaFromState(state) && this.getCoreMetas()[i] > closest) {
                    closest = this.getCoreMetas()[i];
                }
            }

            return new ItemStack(this, 1, closest);
        }
    }

    @SideOnly(Side.CLIENT)
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for(int i = 0; i < this.getCoreMetas().length; ++i) {
            items.add(new ItemStack(this, 1, this.getCoreMetas()[i]));
        }

    }

    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        Item item = player.getHeldItem(hand).getItem();
        if (item.equals(FRItems.paint_scraper)) {
            world.setBlockToAir(pos);
        }

        return true;
    }

    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (world.getBlockState(pos.offset(EnumFacing.DOWN)).getBlock() instanceof BlockAir && RoadsConfig.general.breakPaintOnBlockBreak) {
            world.setBlockToAir(pos);
        }

    }

    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return new AxisAlignedBB(0.0, -1.0 + this.getBlockBelowHeight(source, pos), 0.0, 1.0, -1.0 + this.getBlockBelowHeight(source, pos) + 0.0625, 1.0);
    }

    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    public Block.EnumOffsetType getOffsetType() {
        return EnumOffsetType.XYZ;
    }

    public Vec3d getOffset(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        double offset = 1.0 - this.getBlockBelowHeight(worldIn, pos);
        return new Vec3d(0.0, -offset, 0.0);
    }

    public double getBlockBelowHeight(IBlockAccess worldIn, BlockPos pos) {
        IBlockState underState = worldIn.getBlockState(pos.offset(EnumFacing.DOWN));
        Block underBlock = underState.getBlock();
        double extraOffset = 0.0;
        if (underBlock instanceof RoadBlockDiagonal) {
            RoadBlockDiagonal rbd = (RoadBlockDiagonal)underBlock;
            return rbd.getBlockHeight(worldIn, rbd.getRoad(worldIn, pos.offset(EnumFacing.DOWN)), rbd.getRoadPos(worldIn, pos.offset(EnumFacing.DOWN)));
        } else {
            if (underBlock instanceof PaintBlockBase || underBlock instanceof NonPaintRoadTopBlock || underBlock instanceof CurbBlock) {
                extraOffset = 0.062;
            }

            return underBlock.getBoundingBox(underState, worldIn, pos.offset(EnumFacing.DOWN)).maxY - extraOffset;
        }
    }
}

package fr.aym.gtwnpc.entity.ai;

import fr.aym.gtwnpc.dynamx.GtwNpcModule;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.dynamx.common.entities.BaseVehicleEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class GEntityAIReturnToCar extends EntityAIBase {
    private final EntityGtwNpc tameable;
    @Getter
    @Setter
    private BaseVehicleEntity<?> targetVehicle;
    World world;
    private final double followSpeed;
    private final PathNavigate petPathfinder;
    private int timeToRecalcPath;
    private float oldWaterCost;

    public GEntityAIReturnToCar(EntityGtwNpc tameableIn, double followSpeedIn) {
        this.tameable = tameableIn;
        this.world = tameableIn.world;
        this.followSpeed = followSpeedIn;
        this.petPathfinder = tameableIn.getNavigator();
        this.setMutexBits(3);

        if (!(tameableIn.getNavigator() instanceof PathNavigateGround) && !(tameableIn.getNavigator() instanceof PathNavigateFlying)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
        }
    }

    public boolean shouldExecute() {
        return targetVehicle != null && !targetVehicle.isDead;
    }

    public boolean shouldContinueExecuting() {
        return !this.petPathfinder.noPath() && this.targetVehicle != null && !targetVehicle.isDead;
    }

    public void startExecuting() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.tameable.getPathPriority(PathNodeType.WATER);
        this.tameable.setPathPriority(PathNodeType.WATER, 0.0F);
    }

    public void resetTask() {
        this.petPathfinder.clearPath();
        this.tameable.setPathPriority(PathNodeType.WATER, this.oldWaterCost);
    }

    public void updateTask() {
        if (this.targetVehicle == null)
            return;

        this.tameable.getLookHelper().setLookPositionWithEntity(this.targetVehicle, 10.0F, (float) this.tameable.getVerticalFaceSpeed());

        if(tameable.getDistance(targetVehicle) < 5) {
            if(targetVehicle.hasModuleOfType(GtwNpcModule.class)) {
                if(targetVehicle.getModuleByType(GtwNpcModule.class).mountNpcPassenger(tameable)) {
                    tameable.setDead();
                }
            }
            setTargetVehicle(null);
            resetTask();
            return;
        }

        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;

            if (!this.petPathfinder.tryMoveToEntityLiving(this.targetVehicle, this.followSpeed)) {
                if (!this.tameable.getLeashed() && !this.tameable.isRiding()) {
                    if (this.tameable.getDistanceSq(this.targetVehicle) >= 144.0D) {
                        int i = MathHelper.floor(this.targetVehicle.posX) - 2;
                        int j = MathHelper.floor(this.targetVehicle.posZ) - 2;
                        int k = MathHelper.floor(this.targetVehicle.getEntityBoundingBox().minY);

                        for (int l = 0; l <= 4; ++l) {
                            for (int i1 = 0; i1 <= 4; ++i1) {
                                if ((l < 1 || i1 < 1 || l > 3 || i1 > 3) && this.isTeleportFriendlyBlock(i, j, k, l, i1)) {
                                    this.tameable.setLocationAndAngles((float) (i + l) + 0.5F, k, (float) (j + i1) + 0.5F, this.tameable.rotationYaw, this.tameable.rotationPitch);
                                    this.petPathfinder.clearPath();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean isTeleportFriendlyBlock(int x, int z, int y, int xOffset, int zOffset) {
        BlockPos blockpos = new BlockPos(x + xOffset, y - 1, z + zOffset);
        IBlockState iblockstate = this.world.getBlockState(blockpos);
        return iblockstate.getBlockFaceShape(this.world, blockpos, EnumFacing.DOWN) == BlockFaceShape.SOLID && iblockstate.canEntitySpawn(this.tameable) && this.world.isAirBlock(blockpos.up()) && this.world.isAirBlock(blockpos.up(2));
    }
}

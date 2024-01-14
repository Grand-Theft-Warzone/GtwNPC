package fr.aym.gtwnpc.common;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import fr.aym.gtwnpc.player.PlayerInformation;
import fr.aym.gtwnpc.player.PlayerManager;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import fr.aym.gtwnpc.utils.SpawningConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class NpcSpawningSystem {
    public static void tick(WorldServer world) {
        int i = 0;
        for (Entity e : world.loadedEntityList) {
            if (e instanceof EntityGtwNpc) {
                i++;
            }
        }
        if (i > GtwNpcsConfig.maxNpcs) {
            return;
        }
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        for (EntityPlayer player : world.playerEntities) {
            doSpawning(world, GtwNpcsConfig.citizenSpawningConfig, player, blockpos$mutableblockpos);
            PlayerInformation info = PlayerManager.getPlayerInformation(player.getUniqueID());
            if (info != null && info.getWantedLevel() > 0) {
                GtwNpcsConfig.policeSpawningConfig.setWantedLevel(info.getWantedLevel());
                doSpawning(world, GtwNpcsConfig.policeSpawningConfig, player, blockpos$mutableblockpos);
            }
        }
    }

    public static void doSpawning(WorldServer world, SpawningConfig config, EntityPlayer player, BlockPos.MutableBlockPos blockpos$mutableblockpos) {
        Random r = world.rand;
        if (r.nextInt(100) > config.getNpcSpawnChance()) {
            return;
        }
        Collection<PathNode> nodeList = PedestrianPathNodes.getInstance().getNodes();
        nodeList = nodeList.stream().filter(node -> node.getDistance(player.getPositionVector()) < 128 && node.getDistance(player.getPositionVector()) > 42).collect(Collectors.toList());
        if (nodeList.isEmpty()) {
            return;
        }
        PathNode node = nodeList.stream().skip(r.nextInt(nodeList.size())).findFirst().get();
        //  System.out.println("Spawning npc at " + node);
        List<? extends EntityLivingBase> nears = world.getEntitiesWithinAABB(config.getEntityClass(), node.getBoundingBox().grow(config.getNpcsLimitRadius()));
        if (nears.size() > config.getNpcsLimit()) {
            return;
        }
        int j = MathHelper.floor(node.getPosition().x / 16.0D);
        int k = MathHelper.floor(node.getPosition().z / 16.0D);
        BlockPos blockpos = getRandomChunkPosition(world, j, k, 4);
        IBlockState iblockstate = world.getBlockState(blockpos);
        if (!iblockstate.isNormalCube()) {
            int k1 = blockpos.getX();
            int l1 = blockpos.getY();
            int i2 = blockpos.getZ();
            int j2 = 0;
            for (int k2 = 0; k2 < 3; ++k2) {
                int l2 = k1;
                int i3 = l1;
                int j3 = i2;
                //int k3 = 6;
                IEntityLivingData ientitylivingdata = null;
                int l3 = MathHelper.ceil(Math.random() * 4.0D);
                for (int i4 = 0; i4 < l3; ++i4) {
                    l2 += world.rand.nextInt(6) - world.rand.nextInt(6);
                    //i3 += world.rand.nextInt(1) - world.rand.nextInt(1);
                    j3 += world.rand.nextInt(6) - world.rand.nextInt(6);
                    blockpos$mutableblockpos.setPos(l2, i3, j3);
                    float f = (float) l2 + 0.5F;
                    float f1 = (float) j3 + 0.5F;

                    if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(config.getEntityClass()), world, blockpos$mutableblockpos)) {
                        EntityLiving entityliving = config.getEntityFactory().apply(world);
                        entityliving.setLocationAndAngles(f, i3, f1, world.rand.nextFloat() * 360.0F, 0.0F);
                        if (entityliving.getCanSpawnHere() && entityliving.isNotColliding()) {
                            ientitylivingdata = entityliving.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);
                            if (entityliving.isNotColliding()) {
                                ++j2;
                                world.spawnEntity(entityliving);
                            } else {
                                entityliving.setDead();
                            }
                            if (j2 >= config.getNpcsSpawnLimit()) {
                                return;
                            }
                        }
                    } else {
                        // System.out.println("Failed due to location");
                    }
                }
            }
        } else {
            // System.out.println("Failed due to the block");
        }
    }

    private static BlockPos getRandomChunkPosition(World worldIn, int x, int z, int radius) {
        Chunk chunk = worldIn.getChunk(x, z);
        int i = x * 16 + worldIn.rand.nextInt(radius);
        int j = z * 16 + worldIn.rand.nextInt(radius);
        int k = MathHelper.roundUp(chunk.getHeight(new BlockPos(i, 0, j)) + 1, 16);
        int l = worldIn.rand.nextInt(k > 0 ? k : chunk.getTopFilledSegment() + 16 - 1);
        return new BlockPos(i, l, j);
    }
}

package fr.aym.gtwnpc.common;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class NpcSpawningSystem {
    public static void tick(World world) {
        Random r = world.rand;
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
            if (r.nextInt(20) > 0) {
                continue;
            }
            Collection<PathNode> nodeList = PedestrianPathNodes.getInstance().getNodes();
            nodeList = nodeList.stream().filter(node -> node.getDistance(player.getPositionVector()) < 60 && node.getDistance(player.getPositionVector()) > 24).collect(Collectors.toList());
            if (nodeList.isEmpty()) {
                continue;
            }
            PathNode node = nodeList.stream().skip(r.nextInt(nodeList.size())).findFirst().get();
            System.out.println("Spawning npc at " + node);
            List<EntityGtwNpc> nears = world.getEntitiesWithinAABB(EntityGtwNpc.class, node.getBoundingBox().grow(GtwNpcsConfig.npcsLimitRadius));
            if (nears.size() > GtwNpcsConfig.npcsLimit) {
                continue;
            }
            int j = MathHelper.floor(node.getPosition().x / 16.0D);
            int k = MathHelper.floor(node.getPosition().z / 16.0D);
            BlockPos blockpos = getRandomChunkPosition(world, j, k);
            int k1 = blockpos.getX();
            int l1 = blockpos.getY();
            int i2 = blockpos.getZ();
            IBlockState iblockstate = world.getBlockState(blockpos);

            if (!iblockstate.isNormalCube()) {
                int j2 = 0;

                for (int k2 = 0; k2 < 3; ++k2) {
                    int l2 = k1;
                    int i3 = l1;
                    int j3 = i2;
                    int k3 = 6;
                    IEntityLivingData ientitylivingdata = null;
                    int l3 = MathHelper.ceil(Math.random() * 4.0D);

                    for (int i4 = 0; i4 < l3; ++i4) {
                        l2 += world.rand.nextInt(6) - world.rand.nextInt(6);
                        i3 += world.rand.nextInt(1) - world.rand.nextInt(1);
                        j3 += world.rand.nextInt(6) - world.rand.nextInt(6);
                        blockpos$mutableblockpos.setPos(l2, i3, j3);
                        float f = (float) l2 + 0.5F;
                        float f1 = (float) j3 + 0.5F;

                        if (world.canCreatureTypeSpawnHere(enumcreaturetype, biome$spawnlistentry, blockpos$mutableblockpos) && canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(biome$spawnlistentry.entityClass), world, blockpos$mutableblockpos)) {
                            EntityLiving entityliving;

                            try {
                                entityliving = biome$spawnlistentry.newInstance(world);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                                return j4;
                            }

                            entityliving.setLocationAndAngles((double) f, (double) i3, (double) f1, world.rand.nextFloat() * 360.0F, 0.0F);

                            net.minecraftforge.fml.common.eventhandler.Event.Result canSpawn = net.minecraftforge.event.ForgeEventFactory.canEntitySpawn(entityliving, world, f, i3, f1, false);
                            if (canSpawn == net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW || (canSpawn == net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT && (entityliving.getCanSpawnHere() && entityliving.isNotColliding()))) {
                                if (!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entityliving, world, f, i3, f1))
                                    ientitylivingdata = entityliving.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);

                                if (entityliving.isNotColliding()) {
                                    ++j2;
                                    world.spawnEntity(entityliving);
                                } else {
                                    entityliving.setDead();
                                }

                                if (j2 >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(entityliving)) {
                                    continue label134;
                                }
                            }

                            j4 += j2;
                        }
                    }
                }
            }
        }
    }

    private static BlockPos getRandomChunkPosition(World worldIn, int x, int z) {
        Chunk chunk = worldIn.getChunk(x, z);
        int i = x * 16 + worldIn.rand.nextInt(16);
        int j = z * 16 + worldIn.rand.nextInt(16);
        int k = MathHelper.roundUp(chunk.getHeight(new BlockPos(i, 0, j)) + 1, 16);
        int l = worldIn.rand.nextInt(k > 0 ? k : chunk.getTopFilledSegment() + 16 - 1);
        return new BlockPos(i, l, j);
    }
}

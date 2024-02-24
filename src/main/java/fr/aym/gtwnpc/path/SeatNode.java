package fr.aym.gtwnpc.path;

import com.mia.craftstudio.CSModel;
import com.mia.craftstudio.libgdx.Vector3;
import com.mia.craftstudio.minecraft.CraftStudioModelWrapper;
import com.mia.props.common.entities.EntityChairMount;
import com.mia.props.common.entities.TileMountable;
import fr.aym.gtwnpc.GtwNpcMod;
import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import lombok.Getter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.vecmath.Vector3f;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class SeatNode extends PathNode {
    protected BlockPos seatPos;
    protected float seatRotation;

    public SeatNode() {
    }

    public SeatNode(Vector3f position, Set<PathNode> neighbors, BlockPos seatPos, float seatRotation) {
        super(position, neighbors, NodeType.PEDESTRIAN);
        this.seatPos = seatPos;
        this.seatRotation = seatRotation;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public Object[] getObjectsToSave() {
        if (neighborsIds != null) // Nodes not resolved yet
            return new Object[]{id, position.x, position.y, position.z, neighborsIds, seatPos, seatRotation};
        return new Object[]{id, position.x, position.y, position.z, neighbors.stream().map(PathNode::getId).collect(Collectors.toList()), seatPos, seatRotation};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        super.populateWithSavedObjects(objects);
        seatPos = (BlockPos) objects[5];
        seatRotation = (float) objects[6];
    }

    @Override
    public String toString() {
        return "SeatNode{" + super.toString() +
                ", seatPos=" + seatPos +
                ", seatRotation=" + seatRotation +
                '}';
    }

    @Override
    public boolean isIntermediateNode() {
        return false;
    }

    @Override
    public boolean onReached(World world, EntityGtwNpc npc) {
        if(npc.getNpcType() == SkinRepository.NpcType.POLICE)
            return true;
        if (!world.getEntitiesWithinAABB(EntityChairMount.class, new AxisAlignedBB(seatPos.getX(), seatPos.getY() - 1, seatPos.getZ(), seatPos.getX() + 1, seatPos.getY() + 1, seatPos.getZ() + 1)).isEmpty())
            return true;
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(seatPos);
            if(!(te instanceof TileMountable)) {
                GtwNpcMod.log.error("Seat node at " + seatPos + " is not a mountable tile entity ! Removing the node.");
                delete(PedestrianPathNodes.getInstance(), false);
                return true;
            }
            if (((TileMountable)te).getModelData().decocraftModelID == 0) {
                return true;
            }
            // Code extracted from TileMountable#onBlockActivated
            float offsetX = 0.5F;
            float offsetY = 0.5F;
            float offsetZ = 0.5F;
            CraftStudioModelWrapper.NodeHashCache nodeCache = ((TileMountable)te).getModelData().wrapper.nodeCache;
            Collection<CSModel.ModelNode> nodes = ((TileMountable)te).getModelData().csmodel.getNodes();
            for (CSModel.ModelNode node : nodes) {
                if (node.getName().startsWith("SittingNode")) {
                    CraftStudioModelWrapper.NodeWrapper nodeWrapper = nodeCache.get(node);
                    Vector3 nodeOffset = nodeWrapper.getExtend(((TileMountable) te).rotation)[0];
                    offsetX = nodeOffset.x;
                    offsetY = nodeOffset.y;
                    offsetZ = nodeOffset.z;
                    break;
                }
            }
            EntityChairMount mount = new EntityChairMount(world);
            mount.setPosition((float) seatPos.getX() + offsetX, (float) seatPos.getY() + offsetY - 0.5F, (float) seatPos.getZ() + offsetZ);
            world.spawnEntity(mount);
            npc.startRiding(mount);
            npc.setState("sitting");
            npc.prevRotationYaw = npc.rotationYaw = seatRotation;
        }
        return false;
    }
}

package fr.aym.gtwnpc.utils;

import fr.aym.gtwnpc.client.render.NodesRenderer;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;

public class GtwNpcsUtils {
    public static PathNode rayTracePathNode(Entity entity, float partialTicks) {
        Vec3d vec3d = entity.getPositionEyes(partialTicks);
        double d0 = NodesRenderer.MC.playerController.getBlockReachDistance();
        Vec3d vec3d1 = entity.getLook(1.0F);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
        Vec3d vec3d3 = null;
        double d2 = d0;
        if (NodesRenderer.MC.objectMouseOver != null) {
            d2 = NodesRenderer.MC.objectMouseOver.hitVec.distanceTo(vec3d);
        }
        Collection<PathNode> nodes = PedestrianPathNodes.getInstance().getNodesWithinAABB(entity.getEntityBoundingBox().expand(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).grow(1.0D, 1.0D, 1.0D));
        PathNode pointedNode = null;
        for (PathNode node : nodes) {
            AxisAlignedBB axisalignedbb = node.getBoundingBox();
            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

            if (axisalignedbb.contains(vec3d)) {
                if (d2 >= 0.0D) {
                    pointedNode = node;
                    vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                    d2 = 0.0D;
                }
            } else if (raytraceresult != null) {
                double d3 = vec3d.distanceTo(raytraceresult.hitVec);
                if (d3 < d2 || d2 == 0.0D) {
                    pointedNode = node;
                    vec3d3 = raytraceresult.hitVec;
                    d2 = d3;
                }
            }
        }
        if (pointedNode != null && vec3d.distanceTo(vec3d3) > 4.0D) {
            pointedNode = null;
        }
        return pointedNode;
    }
}

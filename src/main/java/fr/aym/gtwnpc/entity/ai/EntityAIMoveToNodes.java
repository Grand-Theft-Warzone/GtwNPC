package fr.aym.gtwnpc.entity.ai;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import net.minecraft.entity.ai.EntityAIBase;

import javax.vecmath.Vector3f;
import java.util.ArrayDeque;
import java.util.Queue;

public class EntityAIMoveToNodes extends EntityAIBase
{
    private final EntityGtwNpc entity;
    private final Queue<PathNode> path = new ArrayDeque<>();
    protected double x;
    protected double y;
    protected double z;
    protected final double speed;

    public EntityAIMoveToNodes(EntityGtwNpc creatureIn, double speedIn)
    {
        this(creatureIn, speedIn, 0.001f);
    }

    public EntityAIMoveToNodes(EntityGtwNpc creatureIn, double speedIn, float probability)
    {
        this.entity = creatureIn;
        this.speed = speedIn;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute()
    {
        // todo clean this
        //if (!this.mustUpdate)
        {
            /*if (this.entity.getIdleTime() >= 100)
            {
                return false;
            }*/

            if (!entity.getState().equals("wandering"))
            {
                return false;
            }
        }

        this.path.clear();
        //TODO MIN MAX VALUES TO SET
        PathNode target = PedestrianPathNodes.getInstance().selectRandomPathNode(entity.getPositionVector(), 10, 100);
        if(target == null) {
            System.out.println("No target");
            return false;
        }
        Queue<PathNode> path = PedestrianPathNodes.getInstance().createPathToNode(entity.getPositionVector(), target);
        if(path == null) {
            System.out.println("No path to " + target);
            return false;
        }
        this.path.addAll(path);

        target = path.poll();
        Vector3f vec3d = target == null ? null : target.getPosition();

        if (vec3d == null)
        {
            return false;
        }
        else
        {
            this.x = vec3d.x;
            this.y = vec3d.y;
            this.z = vec3d.z;
            return true;
        }
    }

    @Override
    public boolean shouldContinueExecuting()
    {
        if (!entity.getState().equals("wandering"))
        {
            return false;
        }
        if(entity.getNavigator().noPath())
        {
            if(path.size() > 0)
            {
                PathNode target = path.poll();
                Vector3f vec3d = target == null ? null : target.getPosition();
                if (vec3d == null)
                    return false;
                this.x = vec3d.x;
                this.y = vec3d.y;
                this.z = vec3d.z;
                this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void startExecuting()
    {
        this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
    }
}

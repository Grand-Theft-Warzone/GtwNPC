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

    public static PathNode BIG_TARGET;
    public static PathNode INTERMEDIATE_TARGET;

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
        PathNode target = PedestrianPathNodes.getInstance().selectRandomPathNode(entity.getPositionVector(), 20, 3000);
        BIG_TARGET = target;
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
        INTERMEDIATE_TARGET = target;
        Vector3f vec3d = target == null ? null : target.getPosition();

        if (vec3d == null)
        {
            return false;
        }
        else
        {
            this.x = vec3d.x;
            this.y = vec3d.y - 0.5f;
            this.z = vec3d.z;
            System.out.println("Launching to " + target + " at " + x + " " + y + " " + z);
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
        if(entity.getNavigator().getPath() != null && entity.getNavigator().getPath().isFinished())
        {
            if(path.size() > 0)
            {
                PathNode target = path.poll();
                INTERMEDIATE_TARGET = target;
                Vector3f vec3d = target == null ? null : target.getPosition();
                if (vec3d == null)
                    return false;
                this.x = vec3d.x;
                this.y = vec3d.y - 0.5f;
                this.z = vec3d.z;
                System.out.println("Continue to " + target + " at " + x + " " + y + " " + z);
                this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
                return true;
            }
            System.out.println("No path left");
            return false;
        }
        if(entity.getNavigator().noPath()) {
            System.out.println("No path vanilla");
            return false;
        }
        return true;
    }

    @Override
    public void startExecuting()
    {
        System.out.println("Start executing");
        this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
    }
}

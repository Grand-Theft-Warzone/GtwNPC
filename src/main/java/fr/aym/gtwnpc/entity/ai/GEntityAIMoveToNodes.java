package fr.aym.gtwnpc.entity.ai;

import fr.aym.gtwnpc.entity.EntityGtwNpc;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.PedestrianPathNodes;
import net.minecraft.entity.ai.EntityAIBase;

import javax.vecmath.Vector3f;
import java.util.ArrayDeque;
import java.util.Queue;

public class GEntityAIMoveToNodes extends EntityAIBase {
    private final EntityGtwNpc entity;
    private final Queue<PathNode> path = new ArrayDeque<>();
    protected double x;
    protected double y;
    protected double z;

    public GEntityAIMoveToNodes(EntityGtwNpc creatureIn) {
        this.entity = creatureIn;
        this.setMutexBits(1);
    }

    public static PathNode BIG_TARGET;
    public static PathNode INTERMEDIATE_TARGET;

    @Override
    public boolean shouldExecute() {
        // todo clean this
        //if (!this.mustUpdate)
        {
            /*if (this.entity.getIdleTime() >= 100)
            {
                return false;
            }*/

            if (!entity.getState().equals("wandering") && !entity.getState().equals("lost")) {
                return false;
            }
        }

        this.path.clear();
        //TODO MIN MAX VALUES TO SET
        PathNode target = PedestrianPathNodes.getInstance().selectRandomPathNode(entity.getPositionVector(), 20, 3000);
        BIG_TARGET = target;
        if (target == null) {
            //System.out.println("No target");
            return false;
        }
        Queue<PathNode> path = PedestrianPathNodes.getInstance().createPathToNode(entity.getPositionVector(), target);
        if (path == null) {
            //System.out.println("No path to " + target);
            return false;
        }
        this.path.addAll(path);

        target = this.path.peek();
        if (target.getDistance(entity.getPositionVector()) < 3) {
            //System.out.println("00 Intermediate joined !");
            this.path.remove();
            target = this.path.peek();
        }
        INTERMEDIATE_TARGET = target;
        Vector3f tare = target == null ? null : target.getPosition();
        if (tare == null) {
            return false;
        } else {
            double dx = tare.x - entity.posX;
            double dy = tare.y - entity.posY;
            double dz = tare.z - entity.posZ;
            double dist = target.getDistance(entity.getPositionVector());
            if (dist > 10) {
                double adx = Math.abs(dx);
                double ady = Math.abs(dy);
                double adz = Math.abs(dz);
                if (adx > adz && adx > ady) {
                    float red = 10 / (float) adx;
                    dx *= red;
                    dy *= red;
                    dz *= red;
                    dz = dz - 2 + entity.getRNG().nextFloat() * 4;
                } else if (ady > adz && ady > adx) {
                    float red = 10 / (float) ady;
                    dx *= red;
                    dy *= red;
                    dz *= red;
                } else if (adz > adx && adz > ady) {
                    float red = 10 / (float) adz;
                    dx *= red;
                    dx = dx - 2 + entity.getRNG().nextFloat() * 4;
                    dy *= red;
                    dz *= red;
                }
            }
            this.x = entity.posX + dx;
            this.y = entity.posY + dy;
            this.z = entity.posZ + dz;
            //System.out.println("Launching to " + target + " at " + x + " " + y + " " + z);
            return true;
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (!entity.getState().equals("wandering") && !entity.getState().equals("lost")) {
            return false;
        }
        PathNode target = path.peek();
        ////System.out.println("Intermediate dist " + target.getDistance(entity.getPositionVector()));
        if (target.getDistance(entity.getPositionVector()) < 1) {
            //System.out.println("Intermediate joined !");
            path.remove();
            if (path.isEmpty()) {
                //System.out.println("22 No path left");
                return false;
            }
            target = path.peek();
        }
        if (entity.getNavigator().noPath() || entity.getNavigator().getPath() != null && entity.getNavigator().getPath().isFinished()) {
            ////System.out.println("Indexes " + entity.getNavigator().getPath().getCurrentPathIndex() + " on " + entity.getNavigator().getPath());
            if (path.size() > 0) {
                ////System.out.println("Target " + entity.getNavigator().getPath().getTarget());
                //if(true)
                //  return true;
                INTERMEDIATE_TARGET = target;
                Vector3f tare = target == null ? null : target.getPosition();
                if (tare == null)
                    return false;
                double dx = tare.x - entity.posX;
                double dy = tare.y - entity.posY;
                double dz = tare.z - entity.posZ;
                double dist = target.getDistance(entity.getPositionVector());
                if (dist > 12) {
                    double adx = Math.abs(dx);
                    double ady = Math.abs(dy);
                    double adz = Math.abs(dz);
                    if (adx > adz && adx > ady) {
                        float red = 10 / (float) adx;
                        dx *= red;
                        dy *= red;
                        dz *= red;
                        dz = dz - 1.5 + entity.getRNG().nextFloat() * 3;
                    } else if (ady > adz && ady > adx) {
                        float red = 10 / (float) ady;
                        dx *= red;
                        dy *= red;
                        dz *= red;
                    } else if (adz > adx && adz > ady) {
                        float red = 10 / (float) adz;
                        dx *= red;
                        dx = dx - 1.5 + entity.getRNG().nextFloat() * 3;
                        dy *= red;
                        dz *= red;
                    }
                }
                //TODO IMPROVE FORMULA AND CHECK VALIDITY OF BLOCK
                this.x = entity.posX + dx;
                this.y = entity.posY + dy;
                this.z = entity.posZ + dz;
                //System.out.println("Continue to " + target + " at " + x + " " + y + " " + z);
                this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, entity.getAIMoveSpeed());
                if (entity.getNavigator().noPath()) {
                    //System.out.println("Move to " + x + " " + y + " " + z + " at speed " + speed +" : " + this.entity.getNavigator().getPath() +" ist " + target.getDistance(entity.getPositionVector()));
                    //System.out.println("No path vanilla");
                    entity.setState("lost");
                    return false;
                }
                ////System.out.println("Indexes " + entity.getNavigator().getPath().getCurrentPathIndex() + " on " + entity.getNavigator().getPath());
                return true;
            }
            //System.out.println("No path left");
            return false;
        }
        if (entity.getNavigator().noPath()) {
            System.out.println("00 No path vanilla");
            entity.setState("lost");
            return false;
        }
        return true;
    }

    @Override
    public void startExecuting() {
        //System.out.println("Start executing");
        this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, entity.getAIMoveSpeed());
        //System.out.println("Move to " + x + " " + y + " " + z + " at speed " + speed +" : " + this.entity.getNavigator().getPath());
    }
}

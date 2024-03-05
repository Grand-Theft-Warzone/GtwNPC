package fr.aym.gtwnpc.dynamx;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.entity.ai.GEntityAIMoveToNodes;
import fr.aym.gtwnpc.path.CarPathNodes;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.path.TrafficLightNode;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import fr.dynamx.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

public class AutopilotModule {
    //TODO CLEAN
    //TODO DIMS OF VEHICLE
    //TODO SAVE STATE (TARGET...) + ensure path still exists on load

    private final BaseVehicleEntity<?> entity;
    private final GtwNpcModule engineModule;

    private final Queue<PathNode> path = new ArrayDeque<>();
    private PathNode lastTargetNode;
    private String state;
    private boolean navigating;
    private Vector3f navigationTarget;
    private int cooldown = 5;

    protected boolean hasReachedStartPoint;
    protected List<PathNode> nodeBlacklist = new ArrayList<>();

    @Getter
    private float steerForce;

    @Getter
    private final ObstacleDetection obstacleDetection;
    @Getter
    @Setter
    private int forcedSteeringTime;
    @Getter
    @Setter
    private float forcedSteering;

    @Getter
    private int stolenTime;

    public AutopilotModule(BaseVehicleEntity<?> vehicleEntity, GtwNpcModule engineModule) {
        this.entity = vehicleEntity;
        this.engineModule = engineModule;
        this.obstacleDetection = new ObstacleDetection(entity, this);
        //System.out.println("Autopilot module ignited !");
    }

    public void setState(String state) {
        // System.out.println("State: " + state);
        this.state = state;
    }

    public void stealVehicle() {
        stolenTime = 1;
        stopNavigation(Integer.MAX_VALUE);
        setState("stolen");
    }

    protected void startNavigation() {
        //System.out.println("Start navigation");
        this.path.clear();
        //TODO MIN MAX VALUES TO SET
        com.jme3.math.Vector3f look = DynamXGeometry.rotateVectorByQuaternion(com.jme3.math.Vector3f.UNIT_Z, entity.physicsRotation);
        com.jme3.math.Vector3f pos = Vector3fPool.get(entity.physicsPosition);
        Predicate<PathNode> predicate = node -> {
           /* com.jme3.math.Vector3f dir = pos.subtractLocal(node.getPosition().x, node.getPosition().y, node.getPosition().z);
            dir = dir.normalize();
            float dot = dir.dot(look);
            //System.out.println("Dot: " + dot + " " + node + " " + look + " " + dir + " " + pos + " " + entity.getPositionVector() + " " + node.getPosition());
            return dot < 0.2f;*/
            return true;
        };
        PathNode target = CarPathNodes.getInstance().selectRandomPathNode(entity.world, entity.getPositionVector(), 40, 3000, predicate);
        if (target == null) {
            target = CarPathNodes.getInstance().selectRandomPathNode(entity.world, entity.getPositionVector(), 40, 3000, n -> true);
        }
        //CarPathNodes.getInstance().getNode(UUID.fromString("e75bb806-5fa7-4c3e-a78c-bb6ca06942a4"));
        GEntityAIMoveToNodes.BIG_TARGET = target;
        if (target == null) {
            //System.out.println("No target");
            setState("lost_no_target");
            stopNavigation(30 * 20);
            lastTargetNode = null;
            return;
        }
        PathNode start = lastTargetNode != null ? lastTargetNode : CarPathNodes.getInstance().findNearestNode(entity.getPositionVector(), nodeBlacklist);
        if (start == null) {
            //System.out.println("No start");
            setState("lost_no_start");
            stopNavigation(15 * 20);
            lastTargetNode = null;
            return;
        }
        Queue<PathNode> path = CarPathNodes.getInstance().createPathToNode(start, target);
        if (path == null) {
            //System.out.println("No path to " + target + " " + start + " " + nodeBlacklist);
            setState("lost_no_path");
            stopNavigation(5 * 20);
            lastTargetNode = null;
            return;
        }
        //TODO
        /*
        sync
        creative tab
         */
        this.path.addAll(path);

        target = this.path.peek();
        if (target.getDistance(entity.getPositionVector()) < 3) {
            //System.out.println("00 Intermediate joined !");
            lastTargetNode = target;
            this.path.remove();
            target = this.path.peek();
            nodeBlacklist.clear();
            hasReachedStartPoint = true;
        } else {
            lastTargetNode = null;
            hasReachedStartPoint = false;
        }
        GEntityAIMoveToNodes.INTERMEDIATE_TARGET = target;
        Vector3f tare = target == null ? null : target.getPosition();
        if (tare == null) {
            setState("reached_target_0");
            stopNavigation(1);
            return;
        }
        navigationTarget = tare;
        //System.out.println("Launching to " + target + " at " + tare);
        navigating = true;
    }

    public void readFromNBT(NBTTagCompound tag) {
        navigating = tag.getBoolean("navigating");
        //System.out.println("Loading nav " + navigating);
        if (navigating) {
            NBTTagList path = tag.getTagList("path", 8);
            //System.out.println("Read " + path);
            for (int i = 0; i < path.tagCount(); i++) {
                PathNode node = CarPathNodes.getInstance().getNode(java.util.UUID.fromString(path.getStringTagAt(i)));
                if (node != null) {
                    this.path.add(node);
                } else {
                    System.out.println("READ: Node not found: " + path.getStringTagAt(i));
                    stopNavigation(10 * 20);
                    setState("failed_nbt_load");
                    break;
                }
            }
        }
        stolenTime = tag.getInteger("stolenTime");
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("navigating", navigating);
        //System.out.println("Saving nav " + navigating + " " + path.size() + " " + path);
        if (navigating && !this.path.isEmpty()) {
            NBTTagList path = new NBTTagList();
            for (PathNode node : this.path) {
                path.appendTag(new NBTTagString(node.getId().toString()));
            }
            tag.setTag("path", path);
            // System.out.println("Saved " + path);
        }
        tag.setInteger("stolenTime", stolenTime);
    }

    protected void updateNavigation() {
        if (!navigating) {
            if (cooldown > 40)
                cooldown = 40;
            if (cooldown-- == 0) {
                startNavigation();
            }
            return;
        }
        if (path.isEmpty()) {
            setState("empty_path");
            stopNavigation(1);
            return;
        }
        PathNode target = path.peek();
        ////System.out.println("Intermediate dist " + target.getDistance(entity.getPositionVector()));
        if (target.getDistance(entity.getPositionVector()) < 4.5f) {
            //System.out.println("Intermediate joined !");
            if (!hasReachedStartPoint) {
                hasReachedStartPoint = true;
                nodeBlacklist.clear();
            }
            lastTargetNode = target;
            path.remove();
            //System.out.println("Go next node");
            if (path.isEmpty()) {
                //System.out.println("22 No path left");
                //target.onReached(entity.world, entity);
                setState("reached_target_1");
                stopNavigation(1);
                return;
            }
            target = path.peek();
        }
        GEntityAIMoveToNodes.INTERMEDIATE_TARGET = target;
        Vector3f tare = target == null ? null : target.getPosition();
        if (tare == null) {
            setState("reached_target_2");
            stopNavigation(40);
            return;
        }
        navigationTarget = tare;
        com.jme3.math.Vector3f entityPos = entity.physicsPosition;
        Quaternion entityRot = entity.physicsRotation;
        com.jme3.math.Vector3f dest = Vector3fPool.get(navigationTarget.x, navigationTarget.y, navigationTarget.z);
        com.jme3.math.Vector3f dir = dest.subtract(entityPos);
        dir = dir.normalize();
        // Compare the desired dir and the current dir of the vehicle
        // then steer the vehicle to the desired dir
        float angle = (float) ((float) Math.atan2(dir.z, dir.x) - Math.PI / 2);
        Matrix3f rot = new Matrix3f();
        entityRot.toRotationMatrix(rot);
        float yaw = (float) Math.atan2(rot.get(2, 0), rot.get(0, 0));
        float diff = angle - yaw;
        if (diff > Math.PI) {
            diff -= Math.PI * 2;
        } else if (diff < -Math.PI) {
            diff += Math.PI * 2;
        }

        int controls = 1; // engine started
        if (diff > 0.05f) {
            controls |= 16;
            if (diff > 1f)
                steerForce = -1;
            else
                steerForce = -diff;
        } else if (diff < -0.05f) {
            controls |= 8;
            if (diff < -1f)
                steerForce = 1;
            else
                steerForce = -diff;
        } else {
            steerForce = 0;
        }

        float speed = target.getNodeType().getMaxSpeed();
        if (Math.abs(steerForce) > 0.5f) {
            speed = 12;
        } else if (Math.abs(steerForce) > 0.25f) {
            speed = 20;
        } else if (Math.abs(steerForce) > 0.1f) {
            speed = 30;
        } else if (Math.abs(steerForce) > 0.06f) {
            speed = 40;
        }

        float nextAngle = 0;
        if (path.size() > 2) {
            PathNode nextNextNode = path.stream().skip(1).findFirst().orElse(null);
            if (nextNextNode != null) {
                nextAngle = getNextTurnAngle(target, nextNextNode);
            }
        } else {
            nextAngle = (float) (Math.PI / 3);
        }
        if (nextAngle >= Math.PI / 8) {
            if (nextAngle >= Math.PI / 4) {
                if (nextAngle >= Math.PI / 2) {
                    if (speed > 30 && target.getDistance(entity.getPositionVector()) < 25) {
                        speed = 30;
                    }
                    if (speed > 20 && target.getDistance(entity.getPositionVector()) < 16) {
                        speed = 20;
                    }
                    if (speed > 10 && target.getDistance(entity.getPositionVector()) < 8) {
                        speed = 10;
                    }
                } else {
                    if (speed > 35 && target.getDistance(entity.getPositionVector()) < 22) {
                        speed = 35;
                    }
                    if (speed > 25 && target.getDistance(entity.getPositionVector()) < 16) {
                        speed = 25;
                    }
                    if (speed > 15 && target.getDistance(entity.getPositionVector()) < 8) {
                        speed = 15;
                    }
                }
            } else {
                if (speed > 40 && target.getDistance(entity.getPositionVector()) < 16) {
                    speed = 40;
                }
                if (speed > 30 && target.getDistance(entity.getPositionVector()) < 8) {
                    speed = 30;
                }
            }
        }

        //  System.out.println("Target " + target);
        if (target instanceof TrafficLightNode) {
            if (!target.canPassThrough(entity)) {
                float dist = target.getDistance(entity.getPositionVector());
                //   System.out.println("Light IS RED ! " + dist);
                if (dist < 7.2f) {
                    speed = 0;
                } else if (dist < 10) {
                    speed = 2;
                } else if (dist < 16) {
                    speed = 10;
                } else if (dist < 25) {
                    speed = 20;
                } else {
                    speed = 30;
                }
            }
        }
        // System.out.println("Steer: " + Math.abs(steerForce));
        //System.out.println("Speed: " + speed + " for " + target.getDistance(entity.getPositionVector()) + " meters to target and angle " + nextAngle);
        setSpeedLimit(speed);
        BaseVehiclePhysicsHandler<?> phycites = entity.getPhysicsHandler();
        if (phycites == null && entity.getSynchronizer() instanceof SPPhysicsEntitySynchronizer<?>) {
            Entity e = ((SPPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).getOtherSideEntity();
            if (e instanceof BaseVehicleEntity<?>) {
                phycites = ((BaseVehicleEntity<?>) e).getPhysicsHandler();
            }
        }
        if (phycites != null) {
            //System.out.println("Cur speed: " + phycites.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) + " km/h");
            float mySpeed = entity.ticksExisted > 10 ? phycites.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) : 0;
            if (mySpeed > speed + 10 || speed == 0) {
                // System.out.println("Ma braker");
                if (mySpeed > 0.5f)
                    controls |= 4; // braking
                else
                    controls |= 32; // handbrake
            } else if (mySpeed < speed + 10) {
                controls |= 2; // forward
            }
        } else {
            System.out.println("ERROR - No physics handler");
        }
        //System.out.println("Controls: " + controls + " for " + diff + " current: " + yaw + " target: " + angle +" accel? " + (controls & 2));
        setControls(controls);
    }

    protected float getNextTurnAngle(PathNode nextNode, PathNode nextNextNode) {
        com.jme3.math.Vector3f entityPos = entity.physicsPosition;
        Quaternion entityRot = entity.physicsRotation;
        com.jme3.math.Vector3f dest = Vector3fPool.get(nextNode.getPosition().x, nextNode.getPosition().y, nextNode.getPosition().z);
        com.jme3.math.Vector3f dir = dest.subtract(entityPos);
        dir = dir.normalize();
        com.jme3.math.Vector3f nextDest = Vector3fPool.get(nextNextNode.getPosition().x, nextNextNode.getPosition().y, nextNextNode.getPosition().z);
        com.jme3.math.Vector3f nextDir = nextDest.subtract(dest);
        nextDir = nextDir.normalize();
        // angle between dir and nextDir
        float dot = dir.dot(nextDir);
        return FastMath.abs((float) Math.acos(dot));
    }

    protected void stopNavigation(int cooldown) {
        navigating = false;
        path.clear();
        this.cooldown = cooldown;
        navigationTarget = null;
        setControls(32); // handbrake
    }

    public void updateEntity() {
        if (entity.getSynchronizer().getSimulationHolder().ownsControls(entity.world.isRemote ? Side.CLIENT : Side.SERVER)) {
            if (stolenTime == 0) {
                updateNavigation();
                //stopNavigation();
                obstacleDetection.detectObstacles();
            } else {
                // stopNavigation();
            }
            if (entity.ticksExisted % 300 == 0 && entity.hasModuleOfType(AbstractLightsModule.LightsModule.class))
                entity.getModuleByType(AbstractLightsModule.LightsModule.class).setLightOn(3, !entity.world.isDaytime());
        }
        if (!entity.world.isRemote) {
            if (stolenTime > 0) {
                if (entity.getControllingPassenger() != null) {
                    stolenTime = 1;
                } else {
                    stolenTime++;
                    if (stolenTime > 20 * 60 * 5) {
                        System.out.println("Killing stolen entity");
                        entity.setDead();
                    }
                }
            }
        }
    }

    public void setSpeedLimit(float limit) {
        engineModule.setSpeedLimit(limit);
    }

    public int getControls() {
        return engineModule.getControls();
    }

    public void setControls(int controls) {
        engineModule.setControls(controls);
    }
}

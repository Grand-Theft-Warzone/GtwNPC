package fr.aym.gtwnpc.entity;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.entity.ai.EntityAIFollowPlayer;
import fr.aym.gtwnpc.entity.ai.EntityAIMoveToNodes;
import lombok.Getter;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EntityGtwNpc extends EntityCreature implements INpc {
    private static final DataParameter<String> STATE = EntityDataManager.createKey(EntityGtwNpc.class, DataSerializers.STRING);

    @Getter
    private EntityLivingBase entityToFollow;
    private EntityAIFollowPlayer followPlayerAI;
    @Getter
    private ResourceLocation skin;

    //todo
    // déplacement
    // réaction aux coups
    // skin

    public EntityGtwNpc(World worldIn) {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
        this.skin = SkinRepository.getRandomSkin(SkinRepository.SkinType.NPC, worldIn.rand);
    }

    @Override
    public boolean isWithinHomeDistanceFromPosition(BlockPos pos) {
        System.out.println("Is home : " + super.isWithinHomeDistanceFromPosition(pos));
        return super.isWithinHomeDistanceFromPosition(pos);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(STATE, "wandering");
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIPanic(this, 1.25D));
        //this.tasks.addTask(2, new EntityAIMoveIndoors(this));
        this.tasks.addTask(3, new EntityAIRestrictOpenDoor(this));
        this.tasks.addTask(4, new EntityAIOpenDoor(this, true));
        //this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.3D));
        //this.tasks.addTask(6, followPlayerAI = new EntityAIFollowPlayer(this, 1.0D, 10.0F, 2.0F));
        this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
        this.tasks.addTask(9, new EntityAIMoveToNodes(this, 0.6D));
        //this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(100);
    }

    public EntityLivingBase getEntityToFollow() {
        return dataManager.get(STATE).equals("following") ? entityToFollow : null;
    }

    public void setEntityToFollow(EntityLivingBase entityToFollow) {
        this.entityToFollow = entityToFollow;
        if(followPlayerAI != null)
            followPlayerAI.setOwner(null); //Reset entity to follow
    }

    public void setState(String state) {
        //System.out.println("Set state to " + state);
        dataManager.set(STATE, state);
        if (!state.equals("following"))
            setEntityToFollow(null);
    }

    public String getState() {
        return dataManager.get(STATE);
    }

    public void setAttribute(String attribute, String value) {
        System.out.println("Set attribute " + attribute + " to " + value);
        switch (attribute) {
            case "state":
                setState(value);
                break;
            case "speed":
                getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(Double.parseDouble(value));
                break;
        }
    }

    public String getAttribute(String attribute) {
        System.out.println("Get attribute " + attribute);
        switch (attribute) {
            case "state":
                return getState();
            case "speed":
                return String.valueOf(getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue());
        }
        return null;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setString("skin", skin.toString());
        compound.setString("state", getState());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        skin = new ResourceLocation(compound.getString("skin"));
        setState(compound.getString("state"));
    }
}

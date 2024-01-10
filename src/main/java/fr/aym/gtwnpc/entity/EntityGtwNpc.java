package fr.aym.gtwnpc.entity;

import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.entity.ai.EntityAIFollowPlayer;
import fr.aym.gtwnpc.entity.ai.EntityAIHurtByTarget;
import fr.aym.gtwnpc.entity.ai.EntityAIMoveToNodes;
import fr.aym.gtwnpc.entity.ai.EntityAIPanic;
import lombok.Getter;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityGtwNpc extends EntityCreature implements INpc {
    private static final DataParameter<String> STATE = EntityDataManager.createKey(EntityGtwNpc.class, DataSerializers.STRING);
    private static final DataParameter<Boolean> IS_FRIENDLY = EntityDataManager.createKey(EntityGtwNpc.class, DataSerializers.BOOLEAN);

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
        //System.out.println("Is home : " + super.isWithinHomeDistanceFromPosition(pos));
        return super.isWithinHomeDistanceFromPosition(pos);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(STATE, "wandering");
        this.dataManager.register(IS_FRIENDLY, rand.nextBoolean());
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIPanic(this, 0.8D));
        //this.tasks.addTask(2, new EntityAIMoveIndoors(this));
        //this.tasks.addTask(3, new EntityAIRestrictOpenDoor(this));
        this.tasks.addTask(3, new EntityAIOpenDoor(this, true));
        this.tasks.addTask(5, new EntityAIAttackMelee(this, 0.65D, true));
        //this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.3D));
        //this.tasks.addTask(6, followPlayerAI = new EntityAIFollowPlayer(this, 1.0D, 10.0F, 2.0F));
        this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
        this.tasks.addTask(9, new EntityAIMoveToNodes(this, 0.45D + rand.nextDouble() * 0.15));
        //this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(100);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_SPEED).setBaseValue(4);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20);
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        float f = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        int i = 0;

        if (entityIn instanceof EntityLivingBase) {
            f += EnchantmentHelper.getModifierForCreature(this.getHeldItemMainhand(), ((EntityLivingBase) entityIn).getCreatureAttribute());
            i += EnchantmentHelper.getKnockbackModifier(this);
        }

        boolean flag = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), f);

        if (flag) {
            if (i > 0 && entityIn instanceof EntityLivingBase) {
                ((EntityLivingBase) entityIn).knockBack(this, (float) i * 0.5F, (double) MathHelper.sin(this.rotationYaw * 0.017453292F), (double) (-MathHelper.cos(this.rotationYaw * 0.017453292F)));
                this.motionX *= 0.6D;
                this.motionZ *= 0.6D;
            }

            int j = EnchantmentHelper.getFireAspectModifier(this);

            if (j > 0) {
                entityIn.setFire(j * 4);
            }

            if (entityIn instanceof EntityPlayer) {
                EntityPlayer entityplayer = (EntityPlayer) entityIn;
                ItemStack itemstack = this.getHeldItemMainhand();
                ItemStack itemstack1 = entityplayer.isHandActive() ? entityplayer.getActiveItemStack() : ItemStack.EMPTY;

                if (!itemstack.isEmpty() && !itemstack1.isEmpty() && itemstack.getItem().canDisableShield(itemstack, itemstack1, entityplayer, this) && itemstack1.getItem().isShield(itemstack1, entityplayer)) {
                    float f1 = 0.25F + (float) EnchantmentHelper.getEfficiencyModifier(this) * 0.05F;

                    if (this.rand.nextFloat() < f1) {
                        entityplayer.getCooldownTracker().setCooldown(itemstack1.getItem(), 100);
                        this.world.setEntityState(entityplayer, (byte) 30);
                    }
                }
            }
            this.applyEnchantments(this, entityIn);
        }
        return flag;
    }

    public EntityLivingBase getEntityToFollow() {
        return dataManager.get(STATE).equals("following") ? entityToFollow : null;
    }

    public void setEntityToFollow(EntityLivingBase entityToFollow) {
        this.entityToFollow = entityToFollow;
        if (followPlayerAI != null)
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

    public void setFriendly(boolean friendly) {
        dataManager.set(IS_FRIENDLY, friendly);
    }

    public boolean isFriendly() {
        return dataManager.get(IS_FRIENDLY);
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
            case "friendly":
                setFriendly(Boolean.parseBoolean(value));
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
            case "friendly":
                return String.valueOf(isFriendly());
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

package fr.aym.gtwnpc.entity;

import com.modularwarfare.common.guns.ItemGun;
import fr.aym.gtwmap.api.GtwMapApi;
import fr.aym.gtwnpc.client.skin.SkinRepository;
import fr.aym.gtwnpc.entity.ai.*;
import fr.aym.gtwnpc.path.PathNode;
import fr.aym.gtwnpc.sqript.EventOnNpcKilled;
import fr.aym.gtwnpc.utils.GtwNpcsConfig;
import fr.aym.gtwnpc.utils.ShotHelper;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.nico.sqript.ScriptManager;
import lombok.Getter;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public class EntityGtwNpc extends EntityCreature implements INpc, IRangedAttackMob {
    public static Function<EntityGtwNpc, EntityAIBase> moveToNodesAiFactory;

    private static final DataParameter<Integer> NPC_TYPE = EntityDataManager.createKey(EntityGtwNpc.class, DataSerializers.VARINT);
    private static final DataParameter<String> STATE = EntityDataManager.createKey(EntityGtwNpc.class, DataSerializers.STRING);
    private static final DataParameter<Boolean> IS_FRIENDLY = EntityDataManager.createKey(EntityGtwNpc.class, DataSerializers.BOOLEAN);
    private static final DataParameter<String> SKIN = EntityDataManager.createKey(EntityGtwNpc.class, DataSerializers.STRING);
    private static final DataParameter<Boolean> SWINGING_ARMS = EntityDataManager.createKey(EntityGtwNpc.class, DataSerializers.BOOLEAN);
    public static PathNode BIG_TARGET;
    public static PathNode INTERMEDIATE_TARGET;

    protected EntityLivingBase entityToFollow;
    protected GEntityAIFollowPlayer followPlayerAI; //TODO
    protected GEntityAIPanic panicAI;
    protected final GEntityAIAttackMelee attackAI = new GEntityAIAttackMelee(this, GtwNpcsConfig.config.getAttackingMoveSpeed(), false);
    protected final GEntityAIAttackRanged rangedAttackAI = new GEntityAIAttackRanged(this, GtwNpcsConfig.config.getAttackingMoveSpeed(), 15.0F);
    protected GEntityAIPoliceTarget policeTargetAI;

    @Getter
    protected BaseVehicleEntity<?> ownerVehicle;
    @Getter
    protected GEntityAIReturnToCar returnToCarAI;

    @Getter
    private SkinRepository.NpcType npcType = SkinRepository.NpcType.NPC;

    private Entity ridingHack;

    public EntityGtwNpc(World worldIn) {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
        this.setCanPickUpLoot(true);
        this.setCombatTask();
    }

    public void setNpcType(SkinRepository.NpcType npcType) {
        this.npcType = npcType;
        dataManager.set(NPC_TYPE, npcType.ordinal());
        setSkin(SkinRepository.getRandomSkin(npcType, world.rand).toString());
    }

    public void setOwnerVehicle(BaseVehicleEntity<?> ownerVehicle) {
        this.ownerVehicle = ownerVehicle;
        if (ownerVehicle != null) {
            if (returnToCarAI == null)
                returnToCarAI = new GEntityAIReturnToCar(this, GtwNpcsConfig.config.getAttackingMoveSpeed());
            this.tasks.addTask(3, returnToCarAI);
        } else if (returnToCarAI != null) {
            this.tasks.removeTask(returnToCarAI);
            returnToCarAI = null;
        }
    }

    @Override
    public boolean isWithinHomeDistanceFromPosition(BlockPos pos) {
        //System.out.println("Is home : " + super.isWithinHomeDistanceFromPosition(pos));
        return super.isWithinHomeDistanceFromPosition(pos);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(NPC_TYPE, SkinRepository.NpcType.NPC.ordinal());
        this.dataManager.register(STATE, "wandering");
        this.dataManager.register(IS_FRIENDLY, rand.nextInt(100) < GtwNpcsConfig.config.getAttackBackChance());
        this.dataManager.register(SKIN, SkinRepository.getRandomSkin(SkinRepository.NpcType.NPC, world.rand).toString());
        this.dataManager.register(SWINGING_ARMS, Boolean.valueOf(false));
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, panicAI = new GEntityAIPanic(this, GtwNpcsConfig.config.getPanicMoveSpeed()));
        //this.tasks.addTask(2, new EntityAIMoveIndoors(this));
        //this.tasks.addTask(3, new EntityAIRestrictOpenDoor(this));
        this.tasks.addTask(3, new EntityAIOpenDoor(this, true));
        this.tasks.addTask(4, followPlayerAI = new GEntityAIFollowPlayer(this, 1.0D, 10.0F, 2.0F));
        //this.tasks.addTask(5, attackAI);
        //this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.3D));
        this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
        this.tasks.addTask(9, moveToNodesAiFactory.apply(this));
        //this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));

        this.targetTasks.addTask(1, new GEntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, policeTargetAI = new GEntityAIPoliceTarget(this));
    }

    public void setCombatTask() {
        if (this.world != null && !this.world.isRemote) {
            this.tasks.removeTask(this.attackAI);
            this.tasks.removeTask(this.rangedAttackAI);
            ItemStack itemstack = this.getHeldItemMainhand();

            if (itemstack.getItem() instanceof ItemGun) {
                this.tasks.addTask(5, this.rangedAttackAI);
                //System.out.println("Added ranged attack task");
            } else {
                this.tasks.addTask(5, this.attackAI);
                //System.out.println("Added melee attack task");
            }
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue((float) (GtwNpcsConfig.config.getMinNpcMoveSpeed() + rand.nextDouble() * (GtwNpcsConfig.config.getMaxNpcMoveSpeed() - GtwNpcsConfig.config.getMinNpcMoveSpeed())));
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(GtwNpcsConfig.config.getAttackDamage());
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_SPEED).setBaseValue(GtwNpcsConfig.config.getAttackSpeed());
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(GtwNpcsConfig.config.getNpcHealth());
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.getTrueSource() instanceof EntityGtwNpc)
            return false;
        if (source.isProjectile() && source.getTrueSource() instanceof EntityPlayer) {
            //assume it's a gun: all npcs near should panic
            List<EntityGtwNpc> npcsNear = world.getEntitiesWithinAABB(EntityGtwNpc.class, getEntityBoundingBox().grow(20));
            npcsNear.addAll(world.getEntitiesWithinAABB(EntityGtwNpc.class, source.getTrueSource().getEntityBoundingBox().grow(10)));
            for (EntityGtwNpc npc : npcsNear) {
                npc.setRevengeTarget((EntityLivingBase) source.getTrueSource());
                npc.setState("panic");
            }
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void setRevengeTarget(@Nullable EntityLivingBase livingBase) {
        if (livingBase instanceof EntityGtwNpc)
            return;
        super.setRevengeTarget(livingBase);
    }

    @Override
    protected boolean canDespawn() {
        return GtwNpcsConfig.config.getIdleTimeDespawn() != -1 && idleTime > GtwNpcsConfig.config.getIdleTimeDespawn();
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
                ((EntityLivingBase) entityIn).knockBack(this, (float) i * 0.5F, MathHelper.sin(this.rotationYaw * 0.017453292F), -MathHelper.cos(this.rotationYaw * 0.017453292F));
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

    @Override
    public void dismountEntity(Entity entityIn) {
        super.dismountEntity(entityIn);
        if (getState().equals("sitting"))
            setState("wandering");
    }

    public void setState(String state) {
        //System.out.println("Set state to " + state);
        if (getState().equals("sitting") && !state.equals("sitting")) {
            dataManager.set(STATE, state);
            dismountRidingEntity();
        } else {
            dataManager.set(STATE, state);
        }
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

    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        return super.getArmorInventoryList();
    }

    private ResourceLocation skin;

    public ResourceLocation getSkinRes() {
        if (skin == null || !skin.toString().equals(getSkin()))
            skin = new ResourceLocation(getSkin());
        return skin;
    }

    public String getSkin() {
        return dataManager.get(SKIN);
    }

    public void setSkin(String skin) {
        dataManager.set(SKIN, skin);
    }

    public void setAttribute(String attribute, String value) {
        //System.out.println("Set attribute " + attribute + " to " + value);
        switch (attribute) {
            case "type":
                setNpcType(SkinRepository.NpcType.valueOf(value));
                break;
            case "state":
                setState(value);
                break;
            case "move_speed":
                getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(Float.parseFloat(value));
                break;
            case "panic_move_speed":
                panicAI.setSpeed(Double.parseDouble(value));
                break;
            case "attacking_move_speed":
                attackAI.setSpeedTowardsTarget(Double.parseDouble(value));
                rangedAttackAI.setMoveSpeedAmp(Double.parseDouble(value));
                break;
            case "health":
                getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(Double.parseDouble(value));
                break;
            case "attack_damage":
                getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(Double.parseDouble(value));
                break;
            case "attack_speed":
                getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).setBaseValue(Double.parseDouble(value));
                break;
            case "friendly":
                setFriendly(Boolean.parseBoolean(value));
                break;
            default:
                throw new IllegalArgumentException("Unknown attribute " + attribute);
        }
    }

    public String getAttribute(String attribute) {
        //System.out.println("Get attribute " + attribute);
        switch (attribute) {
            case "type":
                return npcType.getName();
            case "state":
                return getState();
            case "move_speed":
                return String.valueOf(getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue());
            case "panic_move_speed":
                return String.valueOf(panicAI.getSpeed());
            case "attacking_move_speed":
                return String.valueOf(attackAI.getSpeedTowardsTarget());
            case "health":
                return String.valueOf(getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue());
            case "attack_damage":
                return String.valueOf(getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue());
            case "attack_speed":
                return String.valueOf(getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).getBaseValue());
            case "friendly":
                return String.valueOf(isFriendly());
            default:
                throw new IllegalArgumentException("Unknown attribute " + attribute);
        }
    }

    public float getMoveSpeed() {
        return (float) getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (getState().equals("sitting")) {
            if (getRidingEntity() == null || getRidingEntity().isDead || (getRidingEntity().ticksExisted > 20 * 60 * 2 && rand.nextInt(100) == 1))
                setState("wandering");
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setString("skin", getSkin());
        compound.setString("state", getState());
        compound.setString("npcType", npcType.name());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        setSkin(compound.getString("skin"));
        setState(compound.getString("state"));
        setNpcType(SkinRepository.NpcType.valueOf(compound.getString("npcType")));
        this.setCombatTask();
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {
        super.setItemStackToSlot(slotIn, stack);
        if (!this.world.isRemote && slotIn == EntityEquipmentSlot.MAINHAND) {
            this.setCombatTask();
        }
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
        setCombatTask();
        return super.onInitialSpawn(difficulty, livingdata);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        ItemStack held = getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemGun)) {
            //System.out.println("Not a gun");
            return;
        }
        double d0 = target.posX - this.posX;
        double d1 = target.getEntityBoundingBox().minY + (double) (target.height / 3.0F) - this.posY - (double) this.getEyeHeight() + 0.10000000149011612D;
        double d2 = target.posZ - this.posZ;
        double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
        float pitch = MathHelper.wrapDegrees((float) (MathHelper.atan2(-d1, d3) * (180D / Math.PI)));
        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(d2, d0) * (180D / Math.PI))) - 90.0F;
        ShotHelper.fireServer(this, pitch, yaw, world, held, (ItemGun) held.getItem());
    }

    public boolean isSwingingArms() {
        return this.dataManager.get(SWINGING_ARMS).booleanValue();
    }

    public void setSwingingArms(boolean swingingArms) {
        this.dataManager.set(SWINGING_ARMS, Boolean.valueOf(swingingArms));
    }

    public boolean isHoldingAGun() {
        return getHeldItemMainhand().getItem() instanceof ItemGun;
    }

    public int getCooldownPeriod() {
        return (int) (1.0D / this.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED).getAttributeValue() * 20.0D);
    }

    public void setRidingHack(Entity ridingHack) {
        this.ridingHack = ridingHack;
    }

    @Override
    public boolean isRiding() {
        return super.isRiding() || ridingHack != null;
    }

    @Nullable
    @Override
    public Entity getRidingEntity() {
        Entity e = super.getRidingEntity();
        return e == null ? ridingHack : e;
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (!world.isRemote && cause.getTrueSource() instanceof EntityPlayer) {
            //System.out.println("Npc killed");
            ScriptManager.callEvent(new EventOnNpcKilled(this, (EntityPlayer) cause.getTrueSource()));
        }
        if (world.isRemote) {
            GtwMapApi.removeTrackedObject(this);
        }
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (world.isRemote && key.equals(NPC_TYPE)) {
            npcType = SkinRepository.NpcType.values()[dataManager.get(NPC_TYPE)];
        }
    }
}

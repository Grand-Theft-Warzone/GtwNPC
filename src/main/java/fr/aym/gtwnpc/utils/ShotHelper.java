package fr.aym.gtwnpc.utils;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.WeaponFireEvent;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import com.modularwarfare.common.hitbox.hits.PlayerHit;
import com.modularwarfare.common.hitbox.maths.EnumHitboxType;
import fr.aym.gtwnpc.entity.EntityGtwNpc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ShotHelper {

    public static void fireServer(EntityGtwNpc entityPlayer, float rotationPitch, float rotationYaw, World world, ItemStack gunStack, ItemGun itemGun) {
        GunType gunType = itemGun.type;
        if (GunType.getAttachment(gunStack, com.modularwarfare.common.guns.AttachmentPresetEnum.Barrel) != null) {
            gunType.playSound(entityPlayer, WeaponSoundType.FireSuppressed, gunStack, null);
        } else if (GunType.isPackAPunched(gunStack)) {
            gunType.playSound(entityPlayer, WeaponSoundType.Punched, gunStack, null);
            gunType.playSound(entityPlayer, WeaponSoundType.Fire, gunStack, null);
        } else {
            gunType.playSound(entityPlayer, WeaponSoundType.Fire, gunStack, null);
        }
        List<Entity> entities = new ArrayList();
        int numBullets = gunType.numBullets;
        ItemBullet bulletItem = ItemGun.getUsedBullet(gunStack, gunType);
        if ((bulletItem != null) &&
                (bulletItem.type.isSlug))
            numBullets = 1;
        ArrayList<BulletHit> rayTraceList;
        boolean headshot;
        BulletHit rayTrace;
        WeaponFireEvent.Post postFireEvent;
        if (gunType.weaponType != WeaponType.Launcher) {
            rayTraceList = new ArrayList();
            for (int i = 0; i < numBullets; i++) {
                rayTrace = com.modularwarfare.utility.RayUtil.standardEntityRayTrace(net.minecraftforge.fml.relauncher.Side.SERVER, world, rotationPitch, rotationYaw, entityPlayer, gunType.weaponEffectiveRange, itemGun, GunType.isPackAPunched(gunStack));
                rayTraceList.add(rayTrace);
            }
            headshot = false;
            for (BulletHit rayTrace2 : rayTraceList) {
                if ((rayTrace2 instanceof PlayerHit)) {
                    if (!world.isRemote) {
                        EntityPlayer victim = ((PlayerHit) rayTrace2).getEntity();
                        if ((victim != null) &&
                                (!victim.isDead) && (victim.getHealth() > 0.0F)) {
                            entities.add(victim);
                            gunType.playSoundPos(victim.getPosition(), world, WeaponSoundType.Penetration);
                            headshot = ((PlayerHit) rayTrace2).hitbox.type.equals(EnumHitboxType.HEAD);
                            ModularWarfare.NETWORK.sendTo(new com.modularwarfare.common.network.PacketPlaySound(victim.getPosition(), "flyby", 1.0F, 1.0F), (EntityPlayerMP) victim);
                            if (ModConfig.INSTANCE.hud.snap_fade_hit) {
                                ModularWarfare.NETWORK.sendTo(new com.modularwarfare.common.network.PacketPlayerHit(), (EntityPlayerMP) victim);
                            }
                        }
                    }
                } else if ((!world.isRemote) &&
                        (rayTrace2.rayTraceResult != null)) {
                    if ((rayTrace2.rayTraceResult.entityHit instanceof com.modularwarfare.common.entity.grenades.EntityGrenade)) {
                        ((com.modularwarfare.common.entity.grenades.EntityGrenade) rayTrace2.rayTraceResult.entityHit).explode();
                    }
                    if ((rayTrace2.rayTraceResult.entityHit instanceof EntityLivingBase)) {
                        EntityLivingBase victim = (EntityLivingBase) rayTrace2.rayTraceResult.entityHit;
                        if (victim != null) {
                            entities.add(victim);
                            gunType.playSoundPos(victim.getPosition(), world, WeaponSoundType.Penetration);
                            headshot = (ItemGun.canEntityGetHeadshot(victim)) && (rayTrace2.rayTraceResult.hitVec.y >= victim.getPosition().getY() + victim.getEyeHeight() - 0.15F);
                        }
                    } else if (rayTrace2.rayTraceResult.hitVec != null) {
                        net.minecraft.util.math.BlockPos blockPos = rayTrace2.rayTraceResult.getBlockPos();
                        ItemGun.playImpactSound(world, blockPos, gunType);
                        gunType.playSoundPos(blockPos, world, WeaponSoundType.Crack, null, 1.0F);
                        ItemGun.doHit(rayTrace2.rayTraceResult, null);
                    }
                }
            }
            if ((entities != null) && (!entities.isEmpty())) {
                for (Entity target : entities) {
                    if ((target != null) && (target != entityPlayer)) {
                        float damage = gunType.gunDamage;
                        if (headshot) {
                            damage += gunType.gunDamageHeadshotBonus;
                        }
                        if ((target instanceof EntityLivingBase)) {
                            EntityLivingBase targetELB = (EntityLivingBase) target;
                            if ((bulletItem != null) &&
                                    (bulletItem.type != null)) {
                                damage *= bulletItem.type.bulletDamageFactor;
                                if ((bulletItem.type.bulletProperties != null) &&
                                        (!bulletItem.type.bulletProperties.isEmpty())) {
                                    BulletProperty bulletProperty = bulletItem.type.bulletProperties.get(targetELB.getName()) != null ? bulletItem.type.bulletProperties.get(targetELB.getName()) : bulletItem.type.bulletProperties.get("All");
                                    if (bulletProperty.potionEffects != null) {
                                        for (PotionEntry potionEntry : bulletProperty.potionEffects) {
                                            targetELB.addPotionEffect(new net.minecraft.potion.PotionEffect(potionEntry.potionEffect.getPotion(), potionEntry.duration, potionEntry.level));
                                        }
                                    }
                                }
                            }
                        }
                        /* todo if (((target instanceof EntityPlayer)) &&
                                (((PlayerHit) rayTraceList.get(0)).hitbox.type.equals(EnumHitboxType.BODY))) {
                            EntityPlayer player = (EntityPlayer) target;
                            if (player.hasCapability(com.modularwarfare.common.capability.extraslots.CapabilityExtra.CAPABILITY, null)) {
                                com.modularwarfare.common.capability.extraslots.IExtraItemHandler extraSlots = player.getCapability(com.modularwarfare.common.capability.extraslots.CapabilityExtra.CAPABILITY, null);
                                ItemStack plate = extraSlots.getStackInSlot(1);
                                if ((plate != null) &&
                                        ((plate.getItem() instanceof com.modularwarfare.common.armor.ItemSpecialArmor))) {
                                    com.modularwarfare.common.armor.ArmorType armorType = ((com.modularwarfare.common.armor.ItemSpecialArmor) plate.getItem()).type;
                                    damage = ((float) (damage - damage * armorType.defense));
                                }
                            }
                        }*/
                        if (!ModConfig.INSTANCE.shots.knockback_entity_damage) {
                            com.modularwarfare.utility.RayUtil.attackEntityWithoutKnockback(target, DamageSource.causeMobDamage(entityPlayer).setProjectile(), damage);
                        } else {
                            target.attackEntityFrom(DamageSource.causeMobDamage(entityPlayer).setProjectile(), damage);
                        }
                        target.hurtResistantTime = 0;
                    }
                }
            }
        } else {
                /*com.modularwarfare.common.entity.EntityExplosiveProjectile projectile = new com.modularwarfare.common.entity.EntityExplosiveProjectile(world, entityPlayer, 0.5F, 3.0F, 2.5F, bulletItem.type.internalName);
                world.spawnEntity(projectile);*/
            throw new IllegalArgumentException("Projectiles not supported. Contact Aym' for that.");
        }
    }

    /*
    public static void shoot(EntityGtwNpc npc, EntityPlayer target, ItemStack gunStack) {
        ItemGun itemGun = (ItemGun) gunStack.getItem();
        GunType gunType = itemGun.type;
        if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel) != null) {
            ItemAttachment barrelAttachment = (ItemAttachment) GunType.getAttachment(gunStack, AttachmentPresetEnum.Barrel).getItem();
            if (barrelAttachment.type.barrel.isSuppressor) {
                playClientSound(npc, WeaponSoundType.FireSuppressed);
            } else {
                playClientSound(npc, WeaponSoundType.Fire);
            }
        } else if (GunType.isPackAPunched(gunStack)) {
            playClientSound(npc, WeaponSoundType.Punched);
            playClientSound(npc, WeaponSoundType.Fire);
        } else {
            playClientSound(npc, WeaponSoundType.Fire);
        }
        if (gunType.weaponType == WeaponType.BoltSniper || gunType.weaponType == WeaponType.Shotgun) {
            playClientSound(npc, WeaponSoundType.Pump);
        }
        fireClientSide(npc, itemGun);
    }

    public static void fireClientSide(EntityGtwNpc entityPlayer, ItemGun itemGun) {
        if (entityPlayer.world.isRemote) {
            List<Entity> entities = new ArrayList();
            int numBullets = itemGun.type.numBullets;
            ItemBullet bulletItem = ItemGun.getUsedBullet(entityPlayer.getHeldItemMainhand(), itemGun.type);
            if (bulletItem != null && bulletItem.type.isSlug) {
                numBullets = 1;
            }

            ArrayList<BulletHit> rayTraceList = new ArrayList();

            for (int i = 0; i < numBullets; ++i) {
                BulletHit rayTrace = RayUtil.standardEntityRayTrace(Side.CLIENT, entityPlayer.world, entityPlayer.rotationPitch, entityPlayer.rotationYaw, entityPlayer, itemGun.type.weaponMaxRange, itemGun, false);
                rayTraceList.add(rayTrace);
            }

            ModularWarfare.NETWORK.sendToServer(new PacketExpShot(entityPlayer.getEntityId(), itemGun.type.internalName));
            boolean headshot = false;
            Iterator var11 = rayTraceList.iterator();

            while (var11.hasNext()) {
                BulletHit rayTrace = (BulletHit) var11.next();
                if (rayTrace instanceof OBBHit) {
                    EntityLivingBase victim = ((OBBHit) rayTrace).entity;
                    if (victim != null && !victim.isDead && victim.getHealth() > 0.0F) {
                        entities.add(victim);
                        ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(victim.getEntityId(), itemGun.type.internalName, ((OBBHit) rayTrace).box.name, itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z));
                    }
                } else if (rayTrace.rayTraceResult != null && rayTrace.rayTraceResult.hitVec != null) {
                    if (rayTrace.rayTraceResult.entityHit != null) {
                        ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(rayTrace.rayTraceResult.entityHit.getEntityId(), itemGun.type.internalName, "", itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z));
                    } else {
                        ModularWarfare.NETWORK.sendToServer(new PacketExpGunFire(-1, itemGun.type.internalName, "", itemGun.type.fireTickDelay, itemGun.type.recoilPitch, itemGun.type.recoilYaw, itemGun.type.recoilAimReducer, itemGun.type.bulletSpread, rayTrace.rayTraceResult.hitVec.x, rayTrace.rayTraceResult.hitVec.y, rayTrace.rayTraceResult.hitVec.z, rayTrace.rayTraceResult.sideHit));
                    }
                }
            }
        }

    }

    public static void playClientSound(EntityLiving player, WeaponSoundType weaponSoundType) {
        if (weaponSoundType != null && this.weaponSoundMap != null) {
            if (this.weaponSoundMap.containsKey(weaponSoundType)) {
                Iterator var3 = ((ArrayList) this.weaponSoundMap.get(weaponSoundType)).iterator();

                while (var3.hasNext()) {
                    SoundEntry soundEntry = (SoundEntry) var3.next();
                    Minecraft.getMinecraft().world.playSound(null, player.getPosition(), ClientProxy.modSounds.get(soundEntry.soundName), SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
            } else if (this.allowDefaultSounds && weaponSoundType.defaultSound != null) {
                Minecraft.getMinecraft().world.playSound(null, player.getPosition(), ClientProxy.modSounds.get(weaponSoundType.defaultSound), SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
    }*/
}

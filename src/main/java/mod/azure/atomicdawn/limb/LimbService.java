package mod.azure.atomicdawn.limb;

import mod.azure.azurelib.hitbox.AzHitboxes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.perks.PerkEffectType;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.vats.BodyPart;

public final class LimbService {

    private static final double HITBOX_REACH_SQR = 2.25;

    private LimbService() {}

    public static boolean enabled(LivingEntity entity) {
        var config = AtomicDawn.CONFIG.limbs;
        if (!config.LIMBS_ENABLED || entity instanceof PowerArmorEntity) {
            return false;
        }
        if (entity instanceof Player ? !config.PLAYER_LIMBS : !config.MOB_LIMBS) {
            return false;
        }
        return !LimbProfiles.immune(entity);
    }

    public static LimbState state(LivingEntity entity) {
        return entity.hasData(ADAttachments.LIMBS.get())
            ? entity.getData(ADAttachments.LIMBS.get())
            : LimbState.HEALTHY;
    }

    public static boolean crippled(LivingEntity entity, BodyPart part) {
        return state(entity).crippled(part);
    }

    public static int crippledLegs(LivingEntity entity) {
        var state = state(entity);
        return (state.crippled(BodyPart.LEFT_LEG) ? 1 : 0) + (state.crippled(BodyPart.RIGHT_LEG) ? 1 : 0);
    }

    public static int crippledArms(LivingEntity entity) {
        var state = state(entity);
        return (state.crippled(BodyPart.LEFT_ARM) ? 1 : 0) + (state.crippled(BodyPart.RIGHT_ARM) ? 1 : 0);
    }

    public static boolean limping(LivingEntity entity) {
        return crippledLegs(entity) > 0;
    }

    public static boolean legsGone(LivingEntity entity) {
        var profile = LimbProfiles.of(entity);
        var legs = (profile.has(BodyPart.LEFT_LEG) ? 1 : 0) + (profile.has(BodyPart.RIGHT_LEG) ? 1 : 0);
        return legs > 0 && crippledLegs(entity) >= legs;
    }

    public static void set(LivingEntity entity, LimbState next) {
        if (next.healthy()) {
            if (entity.hasData(ADAttachments.LIMBS.get())) {
                entity.removeData(ADAttachments.LIMBS.get());
            }
        } else {
            entity.setData(ADAttachments.LIMBS.get(), next);
        }
        LimbEffects.apply(entity);
    }

    public static float limbHealth(LivingEntity entity) {
        return Math.max(
            1F,
            entity.getMaxHealth() * (float) AtomicDawn.CONFIG.limbs.LIMB_HEALTH_FRACTION * LimbProfiles.of(entity)
                .health()
        );
    }

    public static void damage(LivingEntity entity, BodyPart requested, float amount, @Nullable Entity attacker) {
        if (amount <= 0F || entity.level().isClientSide() || !entity.isAlive() || !enabled(entity)) {
            return;
        }
        var part = LimbProfiles.present(entity, requested);
        if (part == null) {
            return;
        }
        if (entity instanceof Player player) {
            if (
                AtomicDawn.CONFIG.limbs.POWER_ARMOR_PROTECTS_LIMBS && player
                    .getVehicle() instanceof PowerArmorEntity
            ) {
                return;
            }
            amount *= 1F - Math.clamp(PerkHooks.value(player, PerkEffectType.LIMB_DAMAGE_REDUCTION), 0F, 1F);
        }
        var state = state(entity);
        var before = state.get(part);
        if (before <= 0F) {
            return;
        }
        var loss = amount * (float) AtomicDawn.CONFIG.limbs.LIMB_DAMAGE_MULTIPLIER / limbHealth(entity);
        if (loss <= 0F) {
            return;
        }
        var after = Math.max(0F, before - loss);
        set(entity, state.with(part, after, entity.level().getGameTime()));
        if (after <= 0F) {
            onCrippled(entity, part, attacker);
        }
    }

    public static void cripple(LivingEntity entity, BodyPart requested, @Nullable Entity attacker) {
        if (entity.level().isClientSide() || !enabled(entity)) {
            return;
        }
        var part = LimbProfiles.present(entity, requested);
        if (part == null || crippled(entity, part)) {
            return;
        }
        set(entity, state(entity).with(part, 0F, entity.level().getGameTime()));
        onCrippled(entity, part, attacker);
    }

    public static void restore(LivingEntity entity, float amount, boolean crippled) {
        var state = state(entity);
        if (state.healthy() || amount <= 0F) {
            return;
        }
        set(entity, state.restore(amount, crippled));
    }

    public static void healAll(LivingEntity entity) {
        if (!state(entity).healthy()) {
            set(entity, LimbState.HEALTHY);
        }
    }

    private static void onCrippled(LivingEntity entity, BodyPart part, @Nullable Entity attacker) {
        if (entity.level() instanceof ServerLevel level) {
            level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.SKELETON_HURT,
                entity.getSoundSource(),
                1F,
                0.55F
            );
        }
        var name = Component.translatable(part.translationKey());
        if (entity instanceof ServerPlayer player) {
            player.sendSystemMessage(
                Component.translatable("limb.atomicdawn.crippled", name).withStyle(ChatFormatting.RED),
                true
            );
        }
        if (attacker instanceof ServerPlayer player && attacker != entity) {
            player.sendSystemMessage(
                Component.translatable("limb.atomicdawn.target_crippled", entity.getDisplayName(), name)
                    .withStyle(ChatFormatting.YELLOW),
                true
            );
        }
    }

    public static @Nullable BodyPart resolve(LivingEntity target, Vec3 position) {
        BodyPart nearest = null;
        var best = HITBOX_REACH_SQR;
        for (var hit : AzHitboxes.getActiveHitboxes(target)) {
            var hitbox = hit.hitbox();
            var part = LimbProfiles.classify(target, hitbox.name(), hitbox.bone(), hitbox::hasTag);
            if (part == null) {
                continue;
            }
            var distance = hit.location().distanceToSqr(position);
            if (distance < best) {
                best = distance;
                nearest = part;
            }
        }
        if (nearest != null) {
            return LimbProfiles.present(target, nearest);
        }
        return LimbProfiles.present(target, geometric(target, position));
    }

    private static BodyPart geometric(LivingEntity target, Vec3 position) {
        var box = target.getBoundingBox();
        var height = Math.max(0.1, box.getYsize());
        var ratio = Mth.clamp((position.y - box.minY) / height, 0.0, 1.0);
        var yaw = target.yBodyRot * Mth.DEG_TO_RAD;
        var left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        var forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        var offset = position.subtract(box.getCenter());
        var lateral = offset.dot(left);
        var width = Math.max(box.getXsize(), box.getZsize());
        var profile = LimbProfiles.of(target);
        var quadruped = !profile.has(BodyPart.LEFT_ARM) && !profile.has(BodyPart.RIGHT_ARM)
            || target.getBbWidth() > target.getBbHeight();
        if (quadruped) {
            if (ratio > 0.5 && offset.dot(forward) > width * 0.25) {
                return BodyPart.HEAD;
            }
            if (ratio < 0.45) {
                return lateral >= 0 ? BodyPart.LEFT_LEG : BodyPart.RIGHT_LEG;
            }
            return BodyPart.TORSO;
        }
        if (ratio >= 0.8) {
            return BodyPart.HEAD;
        }
        if (ratio < 0.42) {
            return lateral >= 0 ? BodyPart.LEFT_LEG : BodyPart.RIGHT_LEG;
        }
        if (Math.abs(lateral) > width * 0.28) {
            return lateral >= 0 ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM;
        }
        return BodyPart.TORSO;
    }

    public static BodyPart randomPart(LivingEntity target, LivingEntity attacker, RandomSource random) {
        var box = target.getBoundingBox();
        var ratio = (attacker.getEyeY() - box.minY) / Math.max(0.1, box.getYsize());
        if (ratio < 0.4) {
            return random.nextBoolean() ? BodyPart.LEFT_LEG : BodyPart.RIGHT_LEG;
        }
        var roll = random.nextFloat();
        if (roll < 0.15F && ratio > 0.6) {
            return BodyPart.HEAD;
        }
        if (roll < 0.55F) {
            return BodyPart.TORSO;
        }
        if (roll < 0.775F) {
            return BodyPart.LEFT_ARM;
        }
        return BodyPart.RIGHT_ARM;
    }

    public static float spreadMultiplier(Player player) {
        return 1F + crippledArms(player) * (float) AtomicDawn.CONFIG.limbs.ARM_SPREAD_PENALTY;
    }

    public static float apRegenMultiplier(Player player) {
        return crippled(player, BodyPart.TORSO) ? (float) AtomicDawn.CONFIG.limbs.TORSO_AP_REGEN_MULTIPLIER : 1F;
    }

    public static float vatsHitMultiplier(Player player) {
        return crippled(player, BodyPart.HEAD) ? (float) AtomicDawn.CONFIG.limbs.HEAD_VATS_HIT_MULTIPLIER : 1F;
    }

    public static float damageTakenMultiplier(LivingEntity entity) {
        return crippled(entity, BodyPart.TORSO)
            ? (float) AtomicDawn.CONFIG.limbs.TORSO_DAMAGE_TAKEN_MULTIPLIER
            : 1F;
    }
}

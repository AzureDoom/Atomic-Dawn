package mod.azure.atomicdawn.limb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.vats.BodyPart;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class LimbEvents {

    private static final int REGEN_INTERVAL = 100;

    private LimbEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var multiplier = LimbService.damageTakenMultiplier(event.getEntity());
        if (multiplier != 1F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        var target = event.getEntity();
        var amount = event.getInflictedDamage();
        if (amount <= 0F || target.level().isClientSide() || !LimbService.enabled(target)) {
            return;
        }
        var source = event.getSource();
        var attacker = source.getEntity();
        var pending = LimbHits.current(target);
        if (pending != null) {
            var part = pending.part() != null ? pending.part() : LimbService.resolve(target, pending.position());
            if (part != null) {
                var multiplier = pending.part() != null
                    ? (float) AtomicDawn.CONFIG.limbs.VATS_LIMB_DAMAGE_MULTIPLIER
                    : 1F;
                LimbService.damage(target, part, amount * multiplier, attacker);
            }
            return;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            var fall = amount * (float) AtomicDawn.CONFIG.limbs.FALL_LIMB_DAMAGE_MULTIPLIER;
            LimbService.damage(target, BodyPart.LEFT_LEG, fall, null);
            LimbService.damage(target, BodyPart.RIGHT_LEG, fall, null);
            return;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            var blast = amount * (float) AtomicDawn.CONFIG.limbs.EXPLOSION_LIMB_DAMAGE_MULTIPLIER;
            for (var part : LimbProfiles.of(target).parts()) {
                LimbService.damage(target, part, blast, attacker);
            }
            return;
        }
        var direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile) {
            var part = LimbService.resolve(target, projectile.position());
            if (part != null) {
                LimbService.damage(target, part, amount, attacker);
            }
            return;
        }
        if (direct instanceof LivingEntity melee && direct == attacker && melee != target) {
            LimbService.damage(target, LimbService.randomPart(target, melee, target.getRandom()), amount, attacker);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !living.hasData(ADAttachments.LIMBS.get())) {
            return;
        }
        var config = AtomicDawn.CONFIG.limbs;
        if (
            living instanceof Player player && config.BOTH_LEGS_BLOCK_SPRINT && player.isSprinting() && LimbService
                .legsGone(player)
        ) {
            player.setSprinting(false);
        }
        if (!(living.level() instanceof ServerLevel level) || (living.tickCount + living.getId()) % 20 != 0) {
            return;
        }
        if (!LimbService.enabled(living)) {
            LimbService.healAll(living);
            return;
        }
        if (
            living instanceof Player player && config.HEAD_CRIPPLED_NAUSEA && LimbService.crippled(
                player,
                BodyPart.HEAD
            ) && !player.hasEffect(MobEffects.NAUSEA)
        ) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, false));
        }
        if ((living.tickCount + living.getId()) % REGEN_INTERVAL != 0) {
            return;
        }
        regenerate(living, level.getGameTime());
    }

    private static void regenerate(LivingEntity living, long now) {
        var config = AtomicDawn.CONFIG.limbs;
        var state = LimbService.state(living);
        if (state.healthy()) {
            LimbService.set(living, state);
            return;
        }
        if (now - state.lastHit() < config.LIMB_REGEN_DELAY_SECONDS * 20L) {
            return;
        }
        var player = living instanceof Player;
        var rate = (float) (player ? config.PLAYER_LIMB_REGEN_PER_SECOND : config.MOB_LIMB_REGEN_PER_SECOND);
        var crippled = player ? config.PLAYER_CRIPPLED_SELF_HEAL : config.MOB_CRIPPLED_SELF_HEAL;
        var regeneration = living.getEffect(MobEffects.REGENERATION);
        if (regeneration != null) {
            rate = Math.max(rate, 0.01F) * (2 + regeneration.getAmplifier());
            crippled = true;
        }
        if (rate <= 0F) {
            return;
        }
        var next = state.restore(rate * REGEN_INTERVAL / 20F, crippled);
        if (!next.sameConditions(state)) {
            LimbService.set(living, next);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (
            !event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living && living.hasData(
                ADAttachments.LIMBS.get()
            )
        ) {
            LimbEffects.apply(living);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        var player = event.getEntity();
        if (!player.level().isClientSide() && AtomicDawn.CONFIG.limbs.SLEEP_HEALS_LIMBS) {
            LimbService.healAll(player);
        }
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        LimbProfiles.invalidate();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LimbCommands.register(event.getDispatcher());
    }
}

package mod.azure.atomicdawn.ghoul;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.mixin.FoodDataAccessor;
import mod.azure.atomicdawn.perks.PerkEffectType;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.special.SpecialService;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.util.HealthClamp;

public final class GhoulService {

    public static final Identifier GLOW_HEALTH_MODIFIER = AtomicDawn.id("ghoul_glow_health");

    private static final float GLOW_HEALTH_STEPS = 10F;

    private static final float TRANSFORM_SATURATION = 5F;

    private static final float FULL_GLOW_TOLERANCE = 0.5F;

    private static final long GLOW_DECAY_GRACE_TICKS = 100L;

    private static final float GLOW_HEALTH_HYSTERESIS = 0.03F;

    private static final float[] WARNING_THRESHOLDS = { 40F, 20F, 0F };

    private static final Map<UUID, Runtime> RUNTIME = new ConcurrentHashMap<>();

    private static final class Runtime {

        float pendingGlow;

        float pendingGlowDecay;

        float pendingStabilityDecay;

        int warningLevel = -1;

        long lastExposureTick = Long.MIN_VALUE;
    }

    private GhoulService() {}

    public static boolean enabled() {
        return AtomicDawn.CONFIG.common.progression.ghoul.general.GHOUL_ENABLED;
    }

    public static GhoulState state(Player player) {
        return player.getData(ADAttachments.GHOUL.get());
    }

    public static GhoulAppearance appearance(Player player) {
        return player.getData(ADAttachments.GHOUL_APPEARANCE.get());
    }

    public static boolean isGhoul(Player player) {
        return enabled() && state(player).isGhoul();
    }

    public static PlayerForm form(Player player) {
        return isGhoul(player) ? PlayerForm.GHOUL : PlayerForm.HUMAN;
    }

    public static float maxStability() {
        return (float) Math.max(1.0, AtomicDawn.CONFIG.common.progression.ghoul.stability.MAXIMUM_STABILITY);
    }

    public static float maxGlow(Player player) {
        return (float) Math.max(
            1.0,
            AtomicDawn.CONFIG.common.progression.ghoul.glow.GLOW_MAXIMUM + GhoulPerks.value(
                player,
                PerkEffectType.GLOW_MAX
            )
        );
    }

    public static float glowFraction(Player player) {
        if (!isGhoul(player)) {
            return 0F;
        }
        return Math.clamp(state(player).glow() / maxGlow(player), 0F, 1F);
    }

    public static float stabilityFraction(Player player) {
        return Math.clamp(state(player).stability() / maxStability(), 0F, 1F);
    }

    public static FeralTier tier(Player player) {
        return isGhoul(player) ? FeralTier.of(state(player).stability(), maxStability()) : FeralTier.STABLE;
    }

    public static float conversion(Player player) {
        return (float) Math.max(
            0.0,
            AtomicDawn.CONFIG.common.progression.ghoul.glow.GLOW_CONVERSION + GhoulPerks.value(
                player,
                PerkEffectType.GLOW_CONVERSION
            )
        );
    }

    public static float rangedAccuracyMultiplier(Player player) {
        if (!isGhoul(player)) {
            return 1F;
        }
        return lerp(
            (float) AtomicDawn.CONFIG.common.progression.ghoul.feral.FERAL_RANGED_ACCURACY_MULTIPLIER,
            tier(player).rangedWeight
        );
    }

    public static float spreadMultiplier(Player player) {
        return 1F / Math.max(0.05F, rangedAccuracyMultiplier(player));
    }

    public static float vatsHitMultiplier(Player player) {
        if (!isGhoul(player)) {
            return 1F;
        }
        return lerp(
            (float) AtomicDawn.CONFIG.common.progression.ghoul.feral.FERAL_VATS_ACCURACY_MULTIPLIER,
            tier(player).vatsWeight
        );
    }

    public static float apRegenMultiplier(Player player) {
        if (!isGhoul(player)) {
            return 1F;
        }
        var feral = lerp(
            (float) AtomicDawn.CONFIG.common.progression.ghoul.feral.FERAL_AP_REGEN_MULTIPLIER,
            tier(player).apWeight
        );
        var glow = 1F + glowFraction(player) * Math.max(0F, GhoulPerks.value(player, PerkEffectType.GLOW_AP_REGEN));
        return Math.max(0F, feral * glow);
    }

    public static float meleeMultiplier(Player player) {
        if (!isGhoul(player)) {
            return 1F;
        }
        var bonus = (float) (AtomicDawn.CONFIG.common.progression.ghoul.feral.FERAL_MELEE_MULTIPLIER - 1.0) * tier(
            player
        ).meleeWeight;
        return Math.max(0F, 1F + bonus * (1F + Math.max(0F, GhoulPerks.value(player, PerkEffectType.FERAL_MELEE))));
    }

    public static float outgoingDamageMultiplier(Player player) {
        if (!isGhoul(player)) {
            return 1F;
        }
        return 1F + glowFraction(player) * Math.max(0F, GhoulPerks.value(player, PerkEffectType.GLOW_DAMAGE));
    }

    public static float incomingDamageMultiplier(Player player) {
        if (!isGhoul(player)) {
            return 1F;
        }
        var feral = 1F + (float) AtomicDawn.CONFIG.common.progression.ghoul.feral.FERAL_DAMAGE_TAKEN_BONUS * tier(
            player
        ).defenseWeight;
        var resist = Math.clamp(
            glowFraction(player) * GhoulPerks.value(player, PerkEffectType.GLOW_DAMAGE_RESISTANCE),
            0F,
            0.9F
        );
        return feral * (1F - resist);
    }

    public static float stabilizerAmount(Player player) {
        var boost = Math.max(0F, GhoulPerks.value(player, PerkEffectType.STABILIZER_BOOST));
        return (float) AtomicDawn.CONFIG.common.progression.ghoul.stability.STABILIZER_RESTORE * (1F + boost);
    }

    public static long cooldownMillis() {
        return Math.round(
            Math.max(0.0, AtomicDawn.CONFIG.common.progression.ghoul.general.FORM_CHANGE_COOLDOWN_HOURS) * 3_600_000.0
        );
    }

    public static long cooldownRemainingMillis(Player player, long now) {
        var cooldown = cooldownMillis();
        var last = state(player).lastFormChangeMillis();
        if (cooldown <= 0L || last <= 0L) {
            return 0L;
        }
        var elapsed = now - last;
        if (elapsed < 0L) {
            if (-elapsed > cooldown) {
                return 0L;
            }
            elapsed = 0L;
        }
        return Math.max(0L, cooldown - elapsed);
    }

    public static boolean dimensionBlocked(ServerPlayer player) {
        var dimension = player.level().dimension().identifier().toString();
        return Arrays.asList(AtomicDawn.CONFIG.common.progression.ghoul.general.BLOCKED_DIMENSIONS).contains(dimension);
    }

    public static TransformationResult canTransform(ServerPlayer player, TransformationContext context) {
        if (!enabled()) {
            return TransformationResult.DISABLED;
        }
        if (state(player).isGhoul()) {
            return TransformationResult.ALREADY_GHOUL;
        }
        if (context.admin()) {
            return TransformationResult.SUCCESS;
        }
        if (!AtomicDawn.CONFIG.common.progression.ghoul.general.ALLOW_TRANSFORMATION) {
            return TransformationResult.DISABLED;
        }
        if (
            SpecialStatResolver.progress(player)
                .level() < AtomicDawn.CONFIG.common.progression.ghoul.general.MINIMUM_LEVEL
        ) {
            return TransformationResult.LEVEL_TOO_LOW;
        }
        if (dimensionBlocked(player)) {
            return TransformationResult.DIMENSION_BLOCKED;
        }
        if (cooldownRemainingMillis(player, System.currentTimeMillis()) > 0L) {
            return TransformationResult.ON_COOLDOWN;
        }
        return TransformationResult.SUCCESS;
    }

    public static TransformationResult canRevert(ServerPlayer player, TransformationContext context) {
        if (!enabled()) {
            return TransformationResult.DISABLED;
        }
        if (!state(player).isGhoul()) {
            return TransformationResult.ALREADY_HUMAN;
        }
        if (context.admin()) {
            return TransformationResult.SUCCESS;
        }
        if (!AtomicDawn.CONFIG.common.progression.ghoul.general.ALLOW_REVERSION) {
            return TransformationResult.DISABLED;
        }
        if (dimensionBlocked(player)) {
            return TransformationResult.DIMENSION_BLOCKED;
        }
        if (cooldownRemainingMillis(player, System.currentTimeMillis()) > 0L) {
            return TransformationResult.ON_COOLDOWN;
        }
        return TransformationResult.SUCCESS;
    }

    public static TransformationResult transform(ServerPlayer player, TransformationContext context) {
        var check = canTransform(player, context);
        if (!check.success()) {
            return check;
        }
        if (!context.requirement().getAsBoolean()) {
            return TransformationResult.MISSING_REQUIREMENT;
        }
        var next = state(player).withForm(PlayerForm.GHOUL, System.currentTimeMillis(), maxStability());
        player.setData(ADAttachments.GHOUL.get(), next);
        RUNTIME.remove(player.getUUID());
        RadiationService.set(player, 0F);
        prepareHunger(player);
        reconcile(player);
        SpecialService.onFormChanged(player);
        feedback(player, PlayerForm.GHOUL);
        return TransformationResult.SUCCESS;
    }

    public static TransformationResult revert(ServerPlayer player, TransformationContext context) {
        var check = canRevert(player, context);
        if (!check.success()) {
            return check;
        }
        if (!context.requirement().getAsBoolean()) {
            return TransformationResult.MISSING_REQUIREMENT;
        }
        var next = state(player).withForm(PlayerForm.HUMAN, System.currentTimeMillis(), maxStability());
        player.setData(ADAttachments.GHOUL.get(), next);
        RUNTIME.remove(player.getUUID());
        reconcile(player);
        SpecialService.onFormChanged(player);
        feedback(player, PlayerForm.HUMAN);
        return TransformationResult.SUCCESS;
    }

    private static void prepareHunger(ServerPlayer player) {
        if (!AtomicDawn.CONFIG.common.progression.ghoul.general.DISABLE_EXHAUSTION_HUNGER_LOSS) {
            return;
        }
        var food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(Math.clamp(food.getSaturationLevel(), TRANSFORM_SATURATION, 20F));
        ((FoodDataAccessor) food).atomicdawn$setExhaustionLevel(0F);
    }

    public static void suppressExhaustion(ServerPlayer player) {
        if (!AtomicDawn.CONFIG.common.progression.ghoul.general.DISABLE_EXHAUSTION_HUNGER_LOSS || !isGhoul(player)) {
            return;
        }
        var food = player.getFoodData();
        if (food.getSaturationLevel() > 0F) {
            return;
        }
        var accessor = (FoodDataAccessor) food;
        if (accessor.atomicdawn$getExhaustionLevel() > 0F) {
            accessor.atomicdawn$setExhaustionLevel(0F);
        }
    }

    public static void acceptRadiation(ServerPlayer player, float postResistanceDose) {
        if (!isGhoul(player) || !(postResistanceDose > 0F) || !player.isAlive()) {
            return;
        }
        var benefit = postResistanceDose * conversion(player);
        if (benefit <= 0F) {
            return;
        }
        var runtime = runtime(player);
        runtime.lastExposureTick = player.level().getGameTime();
        var baseMax = player.getMaxHealth() - currentGlowHealth(player);
        var missing = Math.max(0F, baseMax - player.getHealth());
        var healed = Math.min(missing, benefit);
        if (healed > 0F) {
            player.heal(healed);
        }
        var rest = benefit - healed;
        if (rest > 0F) {
            runtime.pendingGlow += rest;
        }
    }

    public static boolean restoreStability(ServerPlayer player, float amount) {
        if (!isGhoul(player) || amount <= 0F) {
            return false;
        }
        var state = state(player);
        var next = Math.min(maxStability(), state.stability() + amount);
        if (next != state.stability()) {
            player.setData(ADAttachments.GHOUL.get(), state.withStability(next));
        }
        runtime(player).pendingStabilityDecay = 0F;
        return true;
    }

    public static void setGlow(ServerPlayer player, float glow) {
        var state = state(player);
        if (!state.isGhoul()) {
            return;
        }
        runtime(player).pendingGlow = 0F;
        player.setData(ADAttachments.GHOUL.get(), state.withGlow(Math.clamp(glow, 0F, maxGlow(player))));
        updateGlowHealth(player);
        syncAppearance(player);
    }

    public static void setStability(ServerPlayer player, float stability) {
        var state = state(player);
        if (!state.isGhoul()) {
            return;
        }
        player.setData(ADAttachments.GHOUL.get(), state.withStability(Math.clamp(stability, 0F, maxStability())));
    }

    public static void resetCooldown(ServerPlayer player) {
        player.setData(ADAttachments.GHOUL.get(), state(player).withLastFormChange(0L));
    }

    public static void setAppearance(ServerPlayer player, int variant, boolean visible) {
        if (variant < 0 || variant >= GhoulState.APPEARANCE_VARIANTS) {
            return;
        }
        var state = state(player);
        var nextVariant = AtomicDawn.CONFIG.common.progression.ghoul.general.ALLOW_APPEARANCE_VARIANTS ? variant : 0;
        if (nextVariant == state.appearanceVariant() && visible == state.cosmeticVisible()) {
            return;
        }
        player.setData(ADAttachments.GHOUL.get(), state.withAppearance(nextVariant, visible));
        syncAppearance(player);
    }

    public static void resync(ServerPlayer player) {
        var state = state(player);
        player.setData(ADAttachments.GHOUL.get(), state.withLastFormChange(state.lastFormChangeMillis()));
        var appearance = appearance(player);
        player.setData(
            ADAttachments.GHOUL_APPEARANCE.get(),
            new GhoulAppearance(
                appearance.ghoul(),
                appearance.variant(),
                appearance.visible(),
                appearance.fullGlow()
            )
        );
        reconcile(player);
    }

    public static void reconcile(ServerPlayer player) {
        var current = state(player);
        var clean = current.sanitized(current.isGhoul() ? maxGlow(player) : 0F, maxStability());
        if (!clean.equals(current)) {
            player.setData(ADAttachments.GHOUL.get(), clean);
        }
        if (!isGhoul(player)) {
            RUNTIME.remove(player.getUUID());
        }
        updateGlowHealth(player);
        syncAppearance(player);
    }

    public static void forget(UUID id) {
        RUNTIME.remove(id);
    }

    public static void tickSecond(ServerPlayer player) {
        if (!isGhoul(player)) {
            if (hasGlowHealth(player) || appearance(player).ghoul()) {
                reconcile(player);
            }
            return;
        }
        var runtime = runtime(player);
        var state = state(player);
        var paused = !player.isAlive() || player.isSpectator()
            || (player.isCreative() && AtomicDawn.CONFIG.common.progression.ghoul.stability.PAUSE_DECAY_IN_CREATIVE);
        var exposed = player.level().getGameTime() - runtime.lastExposureTick < GLOW_DECAY_GRACE_TICKS;
        if (!paused && !exposed) {
            runtime.pendingGlowDecay += (float) AtomicDawn.CONFIG.common.progression.ghoul.glow.GLOW_DECAY_PER_SECOND
                * PerkHooks.glowDecayMultiplier(player);
        }
        if (!paused) {
            var reduction = Math.clamp(GhoulPerks.value(player, PerkEffectType.FERAL_DECAY), 0F, 0.9F);
            runtime.pendingStabilityDecay +=
                (float) (AtomicDawn.CONFIG.common.progression.ghoul.stability.STABILITY_DECAY_PER_120_SECONDS / 120.0)
                    * (1F - reduction);
        }

        var maxGlow = maxGlow(player);
        var glow = state.glow();
        var glowDelta = runtime.pendingGlow;
        runtime.pendingGlow = 0F;
        if (runtime.pendingGlowDecay >= 1F) {
            glowDelta -= runtime.pendingGlowDecay;
            runtime.pendingGlowDecay = 0F;
        }
        var nextGlow = Math.clamp(glow + glowDelta, 0F, maxGlow);
        if (nextGlow <= 0F) {
            runtime.pendingGlowDecay = 0F;
        }

        var stability = state.stability();
        var nextStability = stability;
        if (runtime.pendingStabilityDecay >= 1F) {
            nextStability = Math.max(0F, stability - runtime.pendingStabilityDecay);
            runtime.pendingStabilityDecay = 0F;
        }
        nextStability = Math.min(nextStability, maxStability());

        if (nextGlow != glow || nextStability != stability) {
            player.setData(ADAttachments.GHOUL.get(), state.withGlow(nextGlow).withStability(nextStability));
        }
        updateGlowHealth(player);
        syncAppearance(player);
        warn(player, runtime, nextStability);
    }

    private static void warn(ServerPlayer player, Runtime runtime, float stability) {
        var percent = stability / maxStability() * 100F;
        var level = 0;
        for (var threshold : WARNING_THRESHOLDS) {
            if (percent <= threshold) {
                level++;
            }
        }
        var previous = runtime.warningLevel;
        runtime.warningLevel = level;
        if (
            previous < 0 || level <= previous
                || !AtomicDawn.CONFIG.common.progression.ghoul.stability.STABILITY_WARNINGS
        ) {
            return;
        }
        var color = level >= 3 ? ChatFormatting.RED : level == 2 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
        player.sendSystemMessage(
            Component.translatable("ghoul.atomicdawn.warning." + level, Math.round(stability)).withStyle(color),
            true
        );
        player.level()
            .playSound(
                null,
                player.blockPosition(),
                SoundEvents.ZOMBIE_AMBIENT,
                SoundSource.PLAYERS,
                0.6F,
                0.6F + 0.1F * level
            );
    }

    private static boolean hasGlowHealth(ServerPlayer player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        return attribute != null && attribute.getModifier(GLOW_HEALTH_MODIFIER) != null;
    }

    public static float glowHealthBonus(Player player) {
        if (!isGhoul(player)) {
            return 0F;
        }
        var steps = (float) Math.floor(glowFraction(player) * GLOW_HEALTH_STEPS) / GLOW_HEALTH_STEPS;
        return (float) Math.max(0.0, AtomicDawn.CONFIG.common.progression.ghoul.glow.GLOW_MAX_HEALTH_BONUS) * steps;
    }

    public static void updateGlowHealth(ServerPlayer player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        var target = glowHealthBonus(player);
        var existing = attribute.getModifier(GLOW_HEALTH_MODIFIER);
        var current = existing == null ? 0.0 : existing.amount();
        if (Math.abs(current - target) < 1.0E-4) {
            return;
        }
        var maxBonus = AtomicDawn.CONFIG.common.progression.ghoul.glow.GLOW_MAX_HEALTH_BONUS;
        if (target > 0F && target < current && maxBonus > 0.0) {
            var heldFraction = current / maxBonus;
            if (glowFraction(player) > heldFraction - GLOW_HEALTH_HYSTERESIS) {
                return;
            }
        }
        attribute.removeModifier(GLOW_HEALTH_MODIFIER);
        if (target > 0F) {
            attribute.addPermanentModifier(
                new AttributeModifier(GLOW_HEALTH_MODIFIER, target, AttributeModifier.Operation.ADD_VALUE)
            );
        }
        var max = player.getMaxHealth();
        if (!player.isAlive()) {
            return;
        }
        if (target > current) {
            player.setHealth(Math.min(max, player.getHealth() + (float) (target - current)));
        } else {
            HealthClamp.clampToMax(player);
        }
    }

    private static float currentGlowHealth(ServerPlayer player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return 0F;
        }
        var existing = attribute.getModifier(GLOW_HEALTH_MODIFIER);
        return existing == null ? 0F : (float) existing.amount();
    }

    public static void syncAppearance(ServerPlayer player) {
        var state = state(player);
        var ghoul = isGhoul(player);
        var desired = new GhoulAppearance(
            ghoul,
            state.appearanceVariant(),
            state.cosmeticVisible(),
            ghoul && state.glow() >= maxGlow(player) - FULL_GLOW_TOLERANCE
        );
        if (!desired.equals(appearance(player))) {
            player.setData(ADAttachments.GHOUL_APPEARANCE.get(), desired);
        }
    }

    private static void feedback(ServerPlayer player, PlayerForm form) {
        var key = form == PlayerForm.GHOUL ? "ghoul.atomicdawn.transformed" : "ghoul.atomicdawn.restored";
        player.sendSystemMessage(
            Component.translatable(key)
                .withStyle(form == PlayerForm.GHOUL ? ChatFormatting.GREEN : ChatFormatting.AQUA)
        );
        ServerLevel level = player.level();
        level.playSound(
            null,
            player.blockPosition(),
            form == PlayerForm.GHOUL ? SoundEvents.ZOMBIE_VILLAGER_CONVERTED : SoundEvents.ZOMBIE_VILLAGER_CURE,
            SoundSource.PLAYERS,
            0.8F,
            form == PlayerForm.GHOUL ? 0.7F : 1.2F
        );
        level.sendParticles(
            form == PlayerForm.GHOUL ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD,
            player.getX(),
            player.getY() + 1.0,
            player.getZ(),
            24,
            0.4,
            0.6,
            0.4,
            0.02
        );
    }

    private static Runtime runtime(ServerPlayer player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new Runtime());
    }

    private static float lerp(float to, float t) {
        return (float) 1.0 + (to - (float) 1.0) * Math.clamp(t, 0F, 1F);
    }
}

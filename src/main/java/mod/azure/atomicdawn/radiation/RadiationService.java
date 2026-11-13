package mod.azure.atomicdawn.radiation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.upgrade.ArmorEffectType;
import mod.azure.atomicdawn.armor.upgrade.ArmorEvents;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.mutation.MutationService;
import mod.azure.atomicdawn.pipboy.PipBoyService;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorEffectType;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgradeEvents;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.registry.ADMobEffects;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.util.HealthClamp;

public final class RadiationService {

    private static final net.minecraft.resources.Identifier HEALTH_MODIFIER = AtomicDawn.id("radiation");

    private RadiationService() {}

    public static float maxRads() {
        return (float) AtomicDawn.CONFIG.radiation.MAX_RADS;
    }

    public static float rads(Player player) {
        return player.getData(ADAttachments.RADIATION.get()).rads();
    }

    public static float resistanceMultiplier(LivingEntity entity) {
        var effect = entity.getEffect(ADMobEffects.RAD_RESISTANCE);
        return effect == null ? 1F : (float) Math.pow(0.5, effect.getAmplifier() + 1);
    }

    public static void expose(LivingEntity entity, float amount) {
        if (amount <= 0F || !(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
            return;
        }
        amount *= resistanceMultiplier(entity);
        if (entity instanceof Player player) {
            if (player.isSpectator() || (player.isCreative() && !GhoulService.isGhoul(player))) {
                return;
            }
            amount *= SpecialStatResolver.resolve(player).radiationMultiplier()
                * PowerArmorUpgradeEvents.reductionMultiplier(player, PowerArmorEffectType.RADIATION_RESISTANCE)
                * ArmorEvents.reductionMultiplier(player, ArmorEffectType.RADIATION_RESISTANCE);
            if (GhoulService.isGhoul(player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    GhoulService.acceptRadiation(serverPlayer, amount);
                    PipBoyService.recordRads(serverPlayer, amount);
                }
                return;
            }
            set(player, rads(player) + amount);
            if (player instanceof ServerPlayer serverPlayer) {
                PipBoyService.recordRads(serverPlayer, amount);
                MutationService.onExposure(serverPlayer, amount);
            }
            return;
        }
        if (entity instanceof PowerArmorEntity || entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
            return;
        }
        var damage = amount * (float) AtomicDawn.CONFIG.radiation.MOB_RADIATION_DAMAGE;
        if (damage > 0F) {
            entity.hurtServer(level, ADDamageTypes.radiation(level), damage);
        }
    }

    public static void remove(Player player, float amount) {
        set(player, rads(player) - amount);
    }

    public static void set(Player player, float rads) {
        var current = player.getData(ADAttachments.RADIATION.get());
        var next = current.withRads(GhoulService.isGhoul(player) ? 0F : rads, maxRads());
        if (Math.abs(next.rads() - current.rads()) < 1.0E-3F) {
            return;
        }
        player.setData(ADAttachments.RADIATION.get(), next);
        updateMaxHealth(player);
    }

    public static void updateMaxHealth(Player player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(HEALTH_MODIFIER);
        var fraction = rads(player) / maxRads() * AtomicDawn.CONFIG.radiation.RADS_MAX_HEALTH_LOSS;
        var unmodified = attribute.getValue();
        var loss = Math.min(unmodified - 2.0, unmodified * fraction);
        if (loss > 0.01) {
            attribute.addTransientModifier(
                new AttributeModifier(HEALTH_MODIFIER, -loss, AttributeModifier.Operation.ADD_VALUE)
            );
        }
        HealthClamp.clampToMax(player);
    }
}

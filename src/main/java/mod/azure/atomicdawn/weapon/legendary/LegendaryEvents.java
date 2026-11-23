package mod.azure.atomicdawn.weapon.legendary;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.weapon.action.WeaponActions;
import mod.azure.atomicdawn.weapon.legendary.behavior.DefenseBehavior;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class LegendaryEvents {

    private static final net.minecraft.resources.Identifier AIMING_SPEED = AtomicDawn.id("legendary_aiming_speed");

    private LegendaryEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (
            source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
                && source.is(DamageTypes.PLAYER_ATTACK)
        ) {
            var multiplier = 1F;
            for (var behavior : LegendaryHooks.held(attacker)) {
                multiplier *= behavior.bashMultiplier();
            }
            if (multiplier != 1F) {
                event.setAmount(event.getAmount() * multiplier);
            }
        }
        if (!(event.getEntity() instanceof ServerPlayer holder)) {
            return;
        }
        var behaviors = LegendaryHooks.held(holder);
        if (behaviors.isEmpty()) {
            return;
        }
        var original = event.getAmount();
        var multiplier = 1F;
        var reflect = 0F;
        for (var behavior : behaviors) {
            multiplier *= behavior.incomingDamageMultiplier(holder, source);
            reflect += behavior.reflectFraction(holder, source);
        }
        if (multiplier != 1F) {
            event.setAmount(original * multiplier);
        }
        if (
            reflect > 0F && DefenseBehavior.melee(source) && source.getEntity() instanceof LivingEntity attacker
                && attacker != holder && holder.level() instanceof ServerLevel level
        ) {
            attacker.hurtServer(level, level.damageSources().thorns(holder), original * reflect);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        var bonus = 0F;
        if (WeaponActions.shooter(player).aimingDownSights()) {
            for (var behavior : LegendaryHooks.held(player)) {
                bonus += behavior.aimingSpeedBonus();
            }
        }
        var existing = speed.getModifier(AIMING_SPEED);
        if (bonus <= 0F) {
            if (existing != null) {
                speed.removeModifier(AIMING_SPEED);
            }
            return;
        }
        if (existing == null || existing.amount() != bonus) {
            speed.removeModifier(AIMING_SPEED);
            speed.addTransientModifier(
                new AttributeModifier(AIMING_SPEED, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        }
    }
}

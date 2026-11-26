package mod.azure.atomicdawn.weapon.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.limb.LimbHits;
import mod.azure.atomicdawn.perks.PerkCombat;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.weapon.action.ShooterState;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;

public final class DamageApplier {

    private DamageApplier() {}

    public static float apply(
        ServerLevel level,
        ServerPlayer attacker,
        ItemStack weapon,
        WeaponDefinition definition,
        ShooterState shooter,
        Entity target,
        Vec3 hitPosition,
        float amount
    ) {
        var behaviors = new ArrayList<LegendaryBehavior>();
        for (var effect : ModificationResolver.legendary(level.registryAccess(), weapon)) {
            effect.value().behavior().ifPresent(behaviors::add);
        }

        var context = new LegendaryContext(level, attacker, weapon, target, hitPosition, shooter);
        var automatic = Weapons.state(weapon, definition).fireMode().repeatsWhileHeld();
        var melee = definition.isMelee() || definition.category() == WeaponCategory.MELEE
            || definition.category() == WeaponCategory.UNARMED;
        var category = definition.category();
        var vats = shooter.vatsActive();
        var perkBonus = SpecialStatResolver.resolve(attacker).damageBonus(category, automatic, melee);
        var total = amount * (float) AtomicDawn.CONFIG.common.combat.weapons.DAMAGE_MULTIPLIER * (1F + perkBonus)
            * PerkCombat.outgoingMultiplier(attacker, target, category, automatic, vats);
        var penetration = PerkCombat.armorPenetration(attacker, category, automatic);
        for (var behavior : behaviors) {
            total *= behavior.damageMultiplier(context);
            penetration = Math.max(penetration, behavior.armorPenetration());
        }
        if (total <= 0F) {
            return 0F;
        }
        for (var behavior : behaviors) {
            if (behavior.redirect(context, total)) {
                return 0F;
            }
        }

        var wasAlive = target instanceof LivingEntity living && living.isAlive();
        resetInvulnerability(target);
        PerkCombat.beginWeaponHit();
        LimbHits.begin(target, vats ? shooter.vatsPart() : null, hitPosition);
        try {
            hurt(level, attacker, definition, target, total, penetration);
        } finally {
            LimbHits.end();
            PerkCombat.endWeaponHit();
        }

        for (var behavior : behaviors) {
            behavior.onHit(context, total);
        }
        if (wasAlive && target instanceof LivingEntity living && living.isDeadOrDying()) {
            shooter.recordKill(level.getGameTime());
            for (var behavior : behaviors) {
                behavior.onKill(context, total);
            }
        }
        PerkCombat.onHit(level, attacker, target, category, automatic, vats, total, hitPosition);
        return total;
    }

    private static void hurt(
        ServerLevel level,
        ServerPlayer attacker,
        WeaponDefinition definition,
        Entity target,
        float total,
        float penetration
    ) {
        var type = ADDamageTypes.forTarget(definition.damage().type(), target);
        if (penetration > 0F) {
            var piercing = total * penetration;
            target.hurtServer(
                level,
                ADDamageTypes.source(level, type, attacker),
                total - piercing
            );
            resetInvulnerability(target);
            target.hurtServer(level, ADDamageTypes.source(level, ADDamageTypes.ARMOR_PIERCING, attacker), piercing);
        } else {
            target.hurtServer(level, ADDamageTypes.source(level, type, attacker), total);
        }
    }

    private static void resetInvulnerability(Entity target) {
        if (target instanceof LivingEntity living) {
            living.setInvulnerableTime(0);
        }
    }
}

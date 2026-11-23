package mod.azure.atomicdawn.weapon.action;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.upgrade.ArmorEvents;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.mutation.MutationService;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.VatsShotPayload;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.pipboy.PipBoyService;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorBonuses;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorEffectType;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgradeEvents;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.thirst.ThirstService;
import mod.azure.atomicdawn.vats.ActionPoints;
import mod.azure.atomicdawn.vats.BodyPart;
import mod.azure.atomicdawn.vats.PartPoint;
import mod.azure.atomicdawn.vats.VatsTargeting;
import mod.azure.atomicdawn.weapon.combat.DamageApplier;
import mod.azure.atomicdawn.weapon.combat.Muzzle;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public final class VatsActions {

    private VatsActions() {}

    public static ActionPoints actionPoints(Player player) {
        return player.getData(ADAttachments.ACTION_POINTS.get());
    }

    public static float maxActionPoints(Player player) {
        return (float) (AtomicDawn.CONFIG.vats.MAX_ACTION_POINTS + SpecialStatResolver.resolve(player).maxApBonus
            + PowerArmorBonuses.of(player).get(PowerArmorEffectType.MAX_AP) + MutationService.maxApBonus(player)
            + ArmorEvents.maxApBonus(player)) * ThirstService.apMultiplier(player);
    }

    public static float apCost(Player player, WeaponDefinition definition) {
        return definition.vats().apCost() * SpecialStatResolver.resolve(player).apCostMultiplier
            * PowerArmorUpgradeEvents.apCostMultiplier(player, definition.category())
            * ArmorEvents.apCostMultiplier(player);
    }

    public static float apCharge(Player player, WeaponDefinition definition) {
        return apCost(player, definition) * (1F - PerkHooks.vatsHealthShare(player));
    }

    public static float healthCharge(Player player, WeaponDefinition definition) {
        return apCost(player, definition) * PerkHooks.vatsHealthShare(player)
            * (float) AtomicDawn.CONFIG.perks.PERKS_VATS_HEALTH_PER_AP;
    }

    private static void payHealth(ServerPlayer player, WeaponDefinition definition) {
        var cost = healthCharge(player, definition);
        if (cost > 0F) {
            player.setHealth(Math.max(1F, player.getHealth() - cost));
        }
    }

    public static boolean spend(ServerPlayer player, float amount) {
        var ap = actionPoints(player);
        if (ap.points() < amount) {
            return false;
        }
        player.setData(ADAttachments.ACTION_POINTS.get(), ap.withPoints(ap.points() - amount, maxActionPoints(player)));
        WeaponActions.shooter(player).lastApSpendTick = player.level().getGameTime();
        return true;
    }

    public static void setState(ServerPlayer player, boolean active, int targetId, BodyPart part) {
        var shooter = WeaponActions.shooter(player);
        shooter.vatsActive = active;
        shooter.vatsTargetId = active ? targetId : -1;
        shooter.vatsPart = part;
        if (!active) {
            shooter.criticalArmed = false;
        }
    }

    public static void armCritical(ServerPlayer player) {
        var shooter = WeaponActions.shooter(player);
        if (actionPoints(player).criticalReady() || player.hasInfiniteMaterials()) {
            shooter.criticalArmed = !shooter.criticalArmed;
        }
    }

    public static void tick(ServerPlayer player, ShooterState shooter, long now) {
        if (now % 5 != 0 || shooter.vatsActive) {
            return;
        }
        if (now - shooter.lastApSpendTick < AtomicDawn.CONFIG.vats.AP_REGEN_DELAY_TICKS) {
            return;
        }
        var ap = actionPoints(player);
        var max = maxActionPoints(player);
        if (ap.points() < max) {
            var gain = (float) AtomicDawn.CONFIG.vats.AP_REGEN_PER_SECOND * 5 / 20F
                * SpecialStatResolver.resolve(player).apRegenMultiplier
                * GhoulService.apRegenMultiplier(player)
                * (1F + PowerArmorBonuses.of(player).get(PowerArmorEffectType.AP_REGEN)
                    + ArmorEvents.apRegenBonus(player));
            player.setData(ADAttachments.ACTION_POINTS.get(), ap.withPoints(ap.points() + gain, max));
        }
    }

    public record VatsShot(
        LivingEntity target,
        PartPoint part
    ) {}

    public static @Nullable VatsShot prepare(ServerPlayer player, ShooterState shooter, WeaponDefinition definition) {
        if (!shooter.vatsActive || shooter.vatsTargetId < 0) {
            return null;
        }
        var entity = player.level().getEntity(shooter.vatsTargetId);
        var cone = AtomicDawn.CONFIG.vats.VATS_TARGET_CONE + 15.0D;
        if (
            !(entity instanceof LivingEntity target) || !VatsTargeting.isValidTarget(player, target, definition, cone)
        ) {
            return null;
        }
        var part = VatsTargeting.parts(target).get(shooter.vatsPart);
        return part == null ? null : new VatsShot(target, part);
    }

    public static boolean canAfford(ServerPlayer player, WeaponDefinition definition) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        var health = healthCharge(player, definition);
        return actionPoints(player).points() >= apCharge(player, definition)
            && (health <= 0F || player.getHealth() > health + 1F);
    }

    public static Vec3 spendForProjectile(
        ServerPlayer player,
        WeaponDefinition definition,
        ShooterState shooter,
        VatsShot shot
    ) {
        if (!player.hasInfiniteMaterials()) {
            var ap = actionPoints(player);
            player.setData(
                ADAttachments.ACTION_POINTS.get(),
                ap.withPoints(ap.points() - apCharge(player, definition), maxActionPoints(player))
            );
            payHealth(player, definition);
        }
        shooter.lastApSpendTick = player.level().getGameTime();
        return shot.part().position();
    }

    public static void notEnoughAp(ServerPlayer player) {
        player.sendOverlayMessage(Component.translatable("message.atomicdawn.not_enough_ap"));
    }

    public static void resolve(
        ServerLevel level,
        ServerPlayer player,
        ItemStack weapon,
        WeaponDefinition definition,
        ShooterState shooter,
        VatsShot shot,
        float damageMultiplier
    ) {
        var now = level.getGameTime();
        var ap = actionPoints(player);
        if (!player.hasInfiniteMaterials()) {
            ap = ap.withPoints(ap.points() - apCharge(player, definition), maxActionPoints(player));
            payHealth(player, definition);
        }
        shooter.lastApSpendTick = now;

        var target = shot.target();
        var part = shot.part();
        var chance = VatsTargeting.hitChance(player, target, definition, part);
        var stats = SpecialStatResolver.resolve(player);
        var critical = shooter.criticalArmed && (ap.criticalReady() || player.hasInfiniteMaterials());
        var hit = critical || player.getRandom().nextFloat() < chance;

        if (critical) {
            shooter.criticalArmed = false;
            ap = ap.withCritical(0F);
        }

        if (hit) {
            var eye = player.getEyePosition();
            var amount = definition.damage().base()
                * Math.max(1, definition.damage().pellets())
                * (definition.isMelee() ? 1F : definition.damage().falloffMultiplier(eye.distanceTo(part.position())))
                * part.damageMultiplier()
                * damageMultiplier
                * (critical
                    ? definition.vats().critMultiplier() * (1F + stats.criticalDamageBonus
                        + MutationService.criticalDamageBonus(player) + ArmorEvents.criticalDamageBonus(player))
                    : 1F);
            DamageApplier.apply(level, player, weapon, definition, shooter, target, part.position(), amount);
            if (!critical) {
                var gain = (float) AtomicDawn.CONFIG.vats.CRITICAL_GAIN_PER_HIT * stats.criticalGainMultiplier;
                ap = ap.withCritical(ap.critical() + gain);
                if (stats.criticalProcChance > 0F && player.getRandom().nextFloat() < stats.criticalProcChance) {
                    ap = ap.withCritical(ActionPoints.CRITICAL_FULL);
                }
            }
            applyLimbEffect(target, part.part());
            if (critical) {
                level.sendParticles(
                    ParticleTypes.CRIT,
                    part.position().x,
                    part.position().y,
                    part.position().z,
                    16,
                    0.2,
                    0.2,
                    0.2,
                    0.3
                );
            }
        } else {
            var miss = part.position()
                .add(
                    player.getRandom().nextGaussian() * 0.4,
                    player.getRandom().nextGaussian() * 0.3,
                    player.getRandom().nextGaussian() * 0.4
                );
            level.sendParticles(ParticleTypes.SMOKE, miss.x, miss.y, miss.z, 3, 0.05, 0.05, 0.05, 0.01);
        }

        definition.tracer().ifPresent(tracer -> beam(level, player, part.position(), tracer, hit));
        player.setData(ADAttachments.ACTION_POINTS.get(), ap);
        PipBoyService.recordVats(player, hit, critical);
        ADNetwork.sendTo(player, new VatsShotPayload(hit, critical, part.part(), Math.round(chance * 100F)));
    }

    private static void applyLimbEffect(LivingEntity target, BodyPart part) {
        if (!AtomicDawn.CONFIG.vats.VATS_LIMB_EFFECTS || !target.isAlive()) {
            return;
        }
        if (part.isLeg()) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
        } else if (part.isArm()) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
        }
    }

    private static void beam(
        ServerLevel level,
        ServerPlayer player,
        Vec3 end,
        WeaponDefinition.Tracer tracer,
        boolean hit
    ) {
        var start = Muzzle.position(player);
        var target = hit ? end : end.add(0.3, 0.2, 0.3);
        var path = target.subtract(start);
        var points = (int) Math.min(48, Math.ceil(path.length() / 0.5));
        if (points <= 0) {
            return;
        }
        var particle = new DustParticleOptions(tracer.color(), tracer.size());
        for (var i = 0; i <= points; i++) {
            var p = start.add(path.scale(i / (double) points));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    static void reset(ShooterState shooter) {
        shooter.vatsTargetId = -1;
        shooter.criticalArmed = false;
    }
}

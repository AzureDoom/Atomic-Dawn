package mod.azure.atomicdawn.weapon.modification;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.weapon.definition.FireMode;
import mod.azure.atomicdawn.weapon.definition.ReloadType;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.legendary.LegendaryEffect;
import mod.azure.atomicdawn.weapon.legendary.WeaponLegendary;

public final class ModificationResolver {

    private ModificationResolver() {}

    public static WeaponMods installed(ItemStack stack, WeaponDefinition base) {
        var mods = stack.get(ADDataComponents.WEAPON_MODS.get());
        return mods != null ? mods : new WeaponMods(base.modifications().defaults());
    }

    public static List<WeaponModification> resolve(
        RegistryAccess access,
        ResourceKey<WeaponDefinition> weaponKey,
        WeaponDefinition base,
        WeaponMods mods
    ) {
        var registry = ADRegistryUtil.optionalRegistry(access, ADRegistries.WEAPON_MODIFICATION);
        var weapon = weaponHolder(access, weaponKey);
        if (registry.isEmpty() || weapon == null || mods.installed().isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<WeaponModification>(mods.installed().size());
        for (var slot : base.modifications().slots()) {
            mods.get(slot)
                .map(key -> registry.get().getValue(key))
                .filter(mod -> mod.slot() == slot && mod.weapons().contains(weapon))
                .ifPresent(result::add);
        }
        return result;
    }

    public static List<Holder.Reference<WeaponModification>> compatible(
        RegistryAccess access,
        ResourceKey<WeaponDefinition> weaponKey,
        ModSlot slot
    ) {
        var registry = ADRegistryUtil.optionalRegistry(access, ADRegistries.WEAPON_MODIFICATION);
        var weapon = weaponHolder(access, weaponKey);
        if (registry.isEmpty() || weapon == null) {
            return List.of();
        }
        return registry.get()
            .listElements()
            .filter(holder -> holder.value().slot() == slot && holder.value().weapons().contains(weapon))
            .sorted(Comparator.comparing(holder -> holder.key().identifier().toString()))
            .toList();
    }

    public static List<Holder.Reference<WeaponModification>> allCompatible(
        RegistryAccess access,
        ResourceKey<WeaponDefinition> weaponKey
    ) {
        var registry = ADRegistryUtil.optionalRegistry(access, ADRegistries.WEAPON_MODIFICATION);
        var weapon = weaponHolder(access, weaponKey);
        if (registry.isEmpty() || weapon == null) {
            return List.of();
        }
        return registry.get().listElements().filter(holder -> holder.value().weapons().contains(weapon)).toList();
    }

    public static Optional<Holder.Reference<WeaponModification>> lookup(
        RegistryAccess access,
        ResourceKey<WeaponModification> key
    ) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.WEAPON_MODIFICATION)
            .flatMap(registry -> registry.get(key));
    }

    public static Set<String> managedBones(RegistryAccess access, ResourceKey<WeaponDefinition> weaponKey) {
        var bones = new LinkedHashSet<String>();
        allCompatible(access, weaponKey).forEach(holder -> bones.addAll(holder.value().showBones()));
        return bones;
    }

    public static Set<String> visibleBones(List<WeaponModification> mods) {
        var bones = new LinkedHashSet<String>();
        mods.forEach(mod -> bones.addAll(mod.showBones()));
        return bones;
    }

    public static Optional<WeaponScope> scope(List<WeaponModification> mods) {
        Optional<WeaponScope> scope = Optional.empty();
        for (var mod : mods) {
            if (mod.scope().isPresent()) {
                scope = mod.scope();
            }
        }
        return scope;
    }

    public static Optional<Identifier> textureOverride(List<WeaponModification> mods) {
        Optional<Identifier> texture = Optional.empty();
        for (var mod : mods) {
            if (mod.texture().isPresent()) {
                texture = mod.texture();
            }
        }
        return texture;
    }

    public static WeaponLegendary legendaryKeys(ItemStack stack) {
        var legendary = stack.get(ADDataComponents.LEGENDARY.get());
        return legendary == null ? WeaponLegendary.NONE : legendary;
    }

    public static List<Holder.Reference<LegendaryEffect>> legendary(RegistryAccess access, ItemStack stack) {
        var keys = legendaryKeys(stack);
        if (keys.isEmpty()) {
            return List.of();
        }
        var registry = ADRegistryUtil.optionalRegistry(access, ADRegistries.LEGENDARY_EFFECT);
        return registry.map(
            legendaryEffects -> keys.effects()
                .stream()
                .map(legendaryEffects::get)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(holder -> holder.value().star()))
                .toList()
        ).orElseGet(List::of);
    }

    public static List<Holder.Reference<LegendaryEffect>> legendaryEffectsForStar(RegistryAccess access, int star) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.LEGENDARY_EFFECT)
            .map(
                registry -> registry.listElements()
                    .filter(holder -> holder.value().star() == star)
                    .sorted(Comparator.comparing(holder -> holder.key().identifier().toString()))
                    .toList()
            )
            .orElse(List.of());
    }

    public static WeaponDefinition effective(
        RegistryAccess access,
        ResourceKey<WeaponDefinition> weaponKey,
        WeaponDefinition base,
        WeaponMods mods,
        List<Holder.Reference<LegendaryEffect>> legendary
    ) {
        var resolvedMods = resolve(access, weaponKey, base, mods);
        if (resolvedMods.isEmpty() && legendary.isEmpty()) {
            return base;
        }
        var modifiers = new ArrayList<StatModifier>();
        List<FireMode> fireModes = null;
        for (var mod : resolvedMods) {
            modifiers.addAll(mod.modifiers());
            if (mod.fireModes().isPresent() && !mod.fireModes().get().isEmpty()) {
                fireModes = mod.fireModes().get();
            }
        }
        for (var effect : legendary) {
            modifiers.addAll(effect.value().modifiers());
        }
        var largeHuntingMagazine = weaponKey.identifier().toString().equals("atomicdawn:hunting_rifle")
            && mods.get(ModSlot.MAGAZINE)
                .filter(key -> key.identifier().toString().equals("atomicdawn:hunting_rifle/large_magazine"))
                .isPresent()
            && resolvedMods.stream().anyMatch(mod -> mod.slot() == ModSlot.MAGAZINE);
        return apply(base, modifiers, fireModes, largeHuntingMagazine ? ReloadType.MAGAZINE : null);
    }

    public static WeaponDefinition apply(WeaponDefinition base, List<WeaponModification> mods) {
        var modifiers = new ArrayList<StatModifier>();
        List<FireMode> fireModes = null;
        for (var mod : mods) {
            modifiers.addAll(mod.modifiers());
            if (mod.fireModes().isPresent() && !mod.fireModes().get().isEmpty()) {
                fireModes = mod.fireModes().get();
            }
        }
        return mods.isEmpty() ? base : apply(base, modifiers, fireModes);
    }

    public static WeaponDefinition apply(
        WeaponDefinition base,
        List<StatModifier> modifiers,
        @Nullable List<FireMode> fireModeOverride
    ) {
        return apply(base, modifiers, fireModeOverride, null);
    }

    private static WeaponDefinition apply(
        WeaponDefinition base,
        List<StatModifier> modifiers,
        @Nullable List<FireMode> fireModeOverride,
        @Nullable ReloadType reloadTypeOverride
    ) {
        var damage = base.damage();
        var firing = base.firing();
        var accuracy = base.accuracy();
        var recoil = base.recoil();
        var ammo = base.ammo();
        var reload = base.reload();
        var condition = base.condition();

        var usesAmmo = ammo.usesAmmo();
        var scaledReload = Math.max(1, round(WeaponStat.RELOAD_TICKS, reload.ticks(), modifiers));
        var reloadScale = scaledReload / (float) reload.ticks();

        return new WeaponDefinition(
            base.category(),
            new WeaponDefinition.Damage(
                nonNegative(WeaponStat.DAMAGE, damage.base(), modifiers),
                Math.max(1, round(WeaponStat.PELLETS, damage.pellets(), modifiers)),
                nonNegative(WeaponStat.HEADSHOT_MULTIPLIER, damage.headshotMultiplier(), modifiers),
                nonNegative(WeaponStat.FALLOFF_START, damage.falloffStart(), modifiers),
                Math.max(1F, StatModifier.apply(WeaponStat.RANGE, damage.range(), modifiers)),
                damage.minFalloffMultiplier(),
                damage.type()
            ),
            new WeaponDefinition.Firing(
                fireModeOverride != null ? fireModeOverride : firing.modes(),
                Math.max(1F, StatModifier.apply(WeaponStat.FIRE_INTERVAL, firing.intervalTicks(), modifiers)),
                new WeaponDefinition.Burst(
                    Math.clamp(round(WeaponStat.BURST_COUNT, firing.burst().count(), modifiers), 2, 16),
                    firing.burst().cooldownTicks()
                ),
                new WeaponDefinition.Charge(
                    Math.max(1, round(WeaponStat.CHARGE_TICKS, firing.charge().ticks(), modifiers)),
                    firing.charge().minCharge(),
                    firing.charge().minDamage(),
                    firing.charge().maxDamage()
                ),
                Math.max(0, round(WeaponStat.SPIN_UP_TICKS, firing.spinUpTicks(), modifiers))
            ),
            new WeaponDefinition.Accuracy(
                nonNegative(WeaponStat.BASE_SPREAD, accuracy.baseSpread(), modifiers),
                nonNegative(WeaponStat.ADS_SPREAD_MULTIPLIER, accuracy.adsMultiplier(), modifiers),
                accuracy.crouchMultiplier(),
                nonNegative(WeaponStat.MOVING_SPREAD_MULTIPLIER, accuracy.movingMultiplier(), modifiers),
                accuracy.airborneMultiplier(),
                nonNegative(WeaponStat.BLOOM_PER_SHOT, accuracy.bloomPerShot(), modifiers),
                nonNegative(WeaponStat.MAX_BLOOM, accuracy.maxBloom(), modifiers),
                accuracy.bloomRecoveryPerTick()
            ),
            new WeaponDefinition.Recoil(
                nonNegative(WeaponStat.RECOIL_VERTICAL, recoil.vertical(), modifiers),
                nonNegative(WeaponStat.RECOIL_HORIZONTAL, recoil.horizontal(), modifiers),
                recoil.adsMultiplier()
            ),
            usesAmmo
                ? new WeaponDefinition.Ammo(
                    ammo.item(),
                    Math.max(1, round(WeaponStat.MAGAZINE_SIZE, ammo.magazineSize(), modifiers)),
                    ammo.perShot()
                )
                : ammo,
            new WeaponDefinition.Reload(
                reloadTypeOverride != null ? reloadTypeOverride : reload.type(),
                scaledReload,
                reload.emptyTicks() > 0 ? Math.max(1, Math.round(reload.emptyTicks() * reloadScale)) : 0,
                Math.max(1, Math.round(reload.perRoundTicks() * reloadScale)),
                Math.max(0, Math.round(reload.endTicks() * reloadScale))
            ),
            Math.max(0, round(WeaponStat.DRAW_TICKS, base.drawTicks(), modifiers)),
            base.sounds(),
            base.modifications(),
            base.melee()
                .map(
                    melee -> new WeaponDefinition.Melee(
                        Math.max(0.5F, StatModifier.apply(WeaponStat.MELEE_REACH, melee.reach(), modifiers)),
                        nonNegative(WeaponStat.KNOCKBACK, melee.knockback(), modifiers),
                        melee.sweepRadius(),
                        melee.sweepDamageMultiplier()
                    )
                ),
            base.tracer(),
            condition.enabled()
                ? new WeaponDefinition.Condition(
                    Math.max(1, round(WeaponStat.CONDITION_MAX, condition.max(), modifiers)),
                    nonNegative(WeaponStat.CONDITION_LOSS, condition.lossPerUse(), modifiers),
                    condition.repairCost()
                )
                : condition,
            base.crafting(),
            new WeaponDefinition.Vats(
                nonNegative(WeaponStat.VATS_AP_COST, base.vats().apCost(), modifiers),
                Math.max(1F, StatModifier.apply(WeaponStat.CRIT_DAMAGE, base.vats().critMultiplier(), modifiers)),
                nonNegative(WeaponStat.VATS_HIT_CHANCE, base.vats().hitChanceMultiplier(), modifiers)
            ),
            base.projectile()
        );
    }

    public static float value(WeaponDefinition definition, WeaponStat stat) {
        return switch (stat) {
            case DAMAGE -> definition.damage().base();
            case PELLETS -> definition.damage().pellets();
            case HEADSHOT_MULTIPLIER -> definition.damage().headshotMultiplier();
            case FALLOFF_START -> definition.damage().falloffStart();
            case RANGE -> definition.damage().range();
            case FIRE_INTERVAL -> definition.firing().intervalTicks();
            case BASE_SPREAD -> definition.accuracy().baseSpread();
            case ADS_SPREAD_MULTIPLIER -> definition.accuracy().adsMultiplier();
            case MOVING_SPREAD_MULTIPLIER -> definition.accuracy().movingMultiplier();
            case BLOOM_PER_SHOT -> definition.accuracy().bloomPerShot();
            case MAX_BLOOM -> definition.accuracy().maxBloom();
            case RECOIL_VERTICAL -> definition.recoil().vertical();
            case RECOIL_HORIZONTAL -> definition.recoil().horizontal();
            case MAGAZINE_SIZE -> definition.ammo().magazineSize();
            case RELOAD_TICKS -> definition.reload().ticks();
            case DRAW_TICKS -> definition.drawTicks();
            case BURST_COUNT -> definition.firing().burst().count();
            case CHARGE_TICKS -> definition.firing().charge().ticks();
            case SPIN_UP_TICKS -> definition.firing().spinUpTicks();
            case MELEE_REACH -> definition.melee().map(WeaponDefinition.Melee::reach).orElse(0F);
            case KNOCKBACK -> definition.melee().map(WeaponDefinition.Melee::knockback).orElse(0F);
            case CONDITION_MAX -> definition.condition().max();
            case CONDITION_LOSS -> definition.condition().lossPerUse();
            case VATS_AP_COST -> definition.vats().apCost();
            case VATS_HIT_CHANCE -> definition.vats().hitChanceMultiplier();
            case CRIT_DAMAGE -> definition.vats().critMultiplier();
        };
    }

    private static float nonNegative(WeaponStat stat, float base, List<StatModifier> modifiers) {
        return Math.max(0F, StatModifier.apply(stat, base, modifiers));
    }

    private static int round(WeaponStat stat, int base, List<StatModifier> modifiers) {
        return Math.round(StatModifier.apply(stat, base, modifiers));
    }

    private static @Nullable Holder<WeaponDefinition> weaponHolder(
        HolderLookup.Provider access,
        ResourceKey<WeaponDefinition> weaponKey
    ) {
        return access.lookup(ADRegistries.WEAPON_DEFINITION)
            .flatMap(registry -> registry.get(weaponKey))
            .orElse(null);
    }
}

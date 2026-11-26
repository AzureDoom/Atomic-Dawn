package mod.azure.atomicdawn.powerarmor.upgrade;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;

public final class PowerArmorBonuses {

    public static final PowerArmorBonuses NONE = new PowerArmorBonuses();

    private record Cached(
        ItemStack[] pieces,
        PowerArmorBonuses bonuses
    ) {}

    private static final Map<PowerArmorEntity, Cached> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private final EnumMap<PowerArmorEffectType, Float> totals = new EnumMap<>(PowerArmorEffectType.class);

    private final EnumMap<HumanoidArm, EnumMap<PowerArmorEffectType, Float>> bracers = new EnumMap<>(HumanoidArm.class);

    private final List<PowerArmorEffect> targeted = new ArrayList<>();

    private final Set<Holder<MobEffect>> immunities = new HashSet<>();

    private PowerArmorBonuses() {
        for (var arm : HumanoidArm.values()) {
            bracers.put(arm, new EnumMap<>(PowerArmorEffectType.class));
        }
    }

    public static PowerArmorBonuses of(@Nullable Player player) {
        return player != null && player.getVehicle() instanceof PowerArmorEntity armor ? of(armor) : NONE;
    }

    public static PowerArmorBonuses of(PowerArmorEntity armor) {
        var pieces = new ItemStack[PowerArmorSlot.COUNT];
        for (var slot : PowerArmorSlot.all()) {
            pieces[slot.ordinal()] = armor.getPiece(slot);
        }
        var cached = CACHE.get(armor);
        if (cached != null && sameStacks(cached.pieces, pieces)) {
            return cached.bonuses;
        }
        var bonuses = compute(armor.level().registryAccess(), pieces);
        CACHE.put(armor, new Cached(pieces, bonuses));
        return bonuses;
    }

    private static boolean sameStacks(ItemStack[] a, ItemStack[] b) {
        for (var i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static PowerArmorBonuses compute(RegistryAccess access, ItemStack[] pieces) {
        var result = new PowerArmorBonuses();
        for (var slot : PowerArmorSlot.all()) {
            var stack = pieces[slot.ordinal()];
            if (
                stack.isEmpty() || !(stack.getItem() instanceof PowerArmorPieceItem) || PowerArmorPieceItem.isBroken(
                    stack
                )
            ) {
                continue;
            }
            var upgrades = PowerArmorUpgrades.of(stack);
            var allowed = AtomicDawn.CONFIG.common.equipment.powerArmor.maintenance.POWER_ARMOR_MODS_PER_PIECE;
            for (var id : upgrades.mods().values()) {

                if (allowed-- <= 0) {
                    break;
                }
                var mod = PowerArmorMod.get(access, id).orElse(null);
                if (mod == null) {
                    continue;
                }
                var arm = slot == PowerArmorSlot.LEFT_ARM
                    ? HumanoidArm.LEFT
                    : slot == PowerArmorSlot.RIGHT_ARM ? HumanoidArm.RIGHT : null;
                for (var effect : mod.value().effects()) {
                    if (arm != null) {
                        result.bracers.get(arm).merge(effect.type(), effect.amount(), Float::sum);
                    } else {
                        result.add(effect);
                    }
                }
            }
            for (var id : upgrades.legendary()) {
                PowerArmorLegendary.get(access, id)
                    .ifPresent(legendary -> legendary.value().effects().forEach(result::add));
            }
        }
        return result;
    }

    private void add(PowerArmorEffect effect) {
        switch (effect.type()) {
            case REDUCE_FROM -> targeted.add(effect);
            case IMMUNITY -> immunities.addAll(effect.effects());

            case HEADLAMP, HASTE -> totals.merge(effect.type(), effect.amount(), Math::max);
            default -> totals.merge(effect.type(), effect.amount(), Float::sum);
        }
    }

    public boolean isEmpty() {
        return totals.isEmpty() && targeted.isEmpty() && immunities.isEmpty()
            && bracers.values().stream().allMatch(Map::isEmpty);
    }

    public float get(PowerArmorEffectType type) {
        return totals.getOrDefault(type, 0F);
    }

    public boolean has(PowerArmorEffectType type) {
        return get(type) > 0F;
    }

    public float unarmed(Player pilot, PowerArmorEffectType type) {
        return bracers.get(pilot.getMainArm()).getOrDefault(type, 0F);
    }

    public float reductionFrom(@Nullable Entity attacker) {
        var total = 0F;
        for (var effect : targeted) {
            if (effect.targets(attacker)) {
                total += effect.amount();
            }
        }
        return total;
    }

    public boolean immuneTo(Holder<MobEffect> effect) {
        return immunities.contains(effect);
    }

    public Set<Holder<MobEffect>> immunities() {
        return immunities;
    }
}

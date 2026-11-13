package mod.azure.atomicdawn.armor.upgrade;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
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

import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.armor.ArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;

public final class ArmorBonuses {

    public static final ArmorBonuses NONE = new ArmorBonuses();

    private record Cached(
        ItemStack[] pieces,
        ArmorBonuses bonuses
    ) {}

    private static final Map<Player, Cached> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private final EnumMap<ArmorEffectType, Float> totals = new EnumMap<>(ArmorEffectType.class);

    private final List<ArmorEffect> targeted = new ArrayList<>();

    private final Set<Holder<MobEffect>> immunities = new HashSet<>();

    private float weight;

    private ArmorBonuses() {}

    public static ArmorBonuses of(@Nullable Player player) {
        if (player == null || player.getVehicle() instanceof PowerArmorEntity) {
            return NONE;
        }
        var pieces = new ItemStack[ArmorPiece.COUNT];
        for (var piece : ArmorPiece.all()) {
            pieces[piece.ordinal()] = player.getItemBySlot(piece.slot());
        }
        var cached = CACHE.get(player);
        if (cached != null && sameStacks(cached.pieces, pieces)) {
            return cached.bonuses;
        }
        var bonuses = compute(player.level().registryAccess(), pieces);
        CACHE.put(player, new Cached(pieces, bonuses));
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

    private static ArmorBonuses compute(RegistryAccess access, ItemStack[] pieces) {
        var result = new ArmorBonuses();
        for (var piece : ArmorPiece.all()) {
            var stack = pieces[piece.ordinal()];
            if (stack.isEmpty() || ArmorPiece.of(stack) != piece) {
                continue;
            }
            var upgrades = ArmorUpgrades.of(stack);
            var lightened = 0F;
            if (stack.getItem() instanceof ArmorPieceItem item) {
                for (var id : upgrades.mods()) {
                    var mod = ArmorMod.get(access, id).orElse(null);
                    if (mod == null || !mod.value().fits(item)) {
                        continue;
                    }
                    for (var effect : mod.value().effects()) {
                        lightened += result.add(effect);
                    }
                }
            }
            for (var id : upgrades.legendary()) {
                var legendary = ArmorLegendary.get(access, id).orElse(null);
                if (legendary == null) {
                    continue;
                }
                for (var effect : legendary.value().effects()) {
                    lightened += result.add(effect);
                }
            }
            if (stack.getItem() instanceof ArmorPieceItem item) {
                var base = item.set().weight().speedPenalty() * piece.weightShare();
                result.weight += base * (1F - Math.clamp(lightened, 0F, 1F));
            }
        }
        return result;
    }

    private float add(ArmorEffect effect) {
        switch (effect.type()) {
            case WEIGHT_REDUCTION -> {
                return effect.amount();
            }
            case REDUCE_FROM -> targeted.add(effect);
            case IMMUNITY -> immunities.addAll(effect.effects());
            case ITEM_MAGNET, HASTE, JET_PACK -> totals.merge(effect.type(), effect.amount(), Math::max);
            default -> totals.merge(effect.type(), effect.amount(), Float::sum);
        }
        return 0F;
    }

    public boolean isEmpty() {
        return totals.isEmpty() && targeted.isEmpty() && immunities.isEmpty() && weight <= 0F;
    }

    public float get(ArmorEffectType type) {
        return totals.getOrDefault(type, 0F);
    }

    public boolean has(ArmorEffectType type) {
        return get(type) > 0F;
    }

    public float weight() {
        return weight;
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

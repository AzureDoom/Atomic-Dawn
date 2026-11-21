package mod.azure.atomicdawn.perks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import mod.azure.atomicdawn.ammunition.AmmoInventory;
import mod.azure.atomicdawn.ammunition.AmmoItem;
import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.armor.ArmorPieceItem;
import mod.azure.atomicdawn.ghoul.FeralTier;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.special.SpecialStats;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.weapon.definition.WeaponCategory;

@SuppressWarnings("unused")
public final class PerkHooks {

    private PerkHooks() {}

    public static SpecialStats stats(Player player) {
        return SpecialStatResolver.resolve(player);
    }

    public static float value(Player player, PerkEffectType type) {
        return stats(player).extra(type);
    }

    public static int specialBonus(Player player, SpecialAttribute attribute) {
        if (attribute != SpecialAttribute.STRENGTH) {
            return 0;
        }
        var radicool = SpecialStatResolver.equippedValue(player, PerkEffectType.RADICOOL);
        if (radicool <= 0F || GhoulService.isGhoul(player)) {
            return 0;
        }
        var fraction = Math.clamp(RadiationService.rads(player) / Math.max(1F, RadiationService.maxRads()), 0F, 1F);
        return (int) Math.floor(radicool * fraction);
    }

    public static float classFreak(Player player) {
        return Math.clamp(SpecialStatResolver.equippedValue(player, PerkEffectType.CLASS_FREAK), 0F, 1F);
    }

    public static float strangeInNumbers(Player player) {
        return Math.max(0F, SpecialStatResolver.equippedValue(player, PerkEffectType.STRANGE_IN_NUMBERS));
    }

    public static float spreadMultiplier(Player player, WeaponCategory category) {
        return 1F - Math.clamp(stats(player).value(PerkEffectType.SPREAD, category), 0F, 0.75F);
    }

    public static float reloadMultiplier(Player player, WeaponCategory category, boolean automatic) {
        var bonus = Math.max(0F, stats(player).value(PerkEffectType.RELOAD_SPEED, category, automatic));
        return 1F / (1F + bonus);
    }

    public static int scaleTicks(int ticks, float multiplier) {
        return ticks <= 0 ? ticks : Math.max(1, Math.round(ticks * multiplier));
    }

    public static boolean saveAmmo(Player player, WeaponCategory category, boolean automatic) {
        var chance = Math.clamp(stats(player).value(PerkEffectType.AMMO_SAVE, category, automatic), 0F, 0.9F);
        return chance > 0F && player.getRandom().nextFloat() < chance;
    }

    public static float vatsHealthShare(Player player) {
        return Math.clamp(value(player, PerkEffectType.VATS_HEALTH_COST), 0F, 1F);
    }

    public static float powerArmorDrainMultiplier(Player rider) {
        var stats = stats(rider);
        var reduction = stats.extra(PerkEffectType.PA_CORE_DRAIN);
        if (rider.isSprinting()) {
            reduction += stats.extra(PerkEffectType.PA_SPRINT_DRAIN);
        }
        return 1F - Math.clamp(reduction, 0F, 0.9F);
    }

    public static float armorWeightMultiplier(Player player) {
        return 1F - Math.clamp(value(player, PerkEffectType.ARMOR_WEIGHT), 0F, 0.9F);
    }

    public static float armorConditionKeep(Player player) {
        return Math.clamp(value(player, PerkEffectType.ARMOR_CONDITION_LOSS), 0F, 0.9F);
    }

    public static float glowDecayMultiplier(Player player) {
        if (GhoulService.tier(player).ordinal() < FeralTier.NEAR_FERAL.ordinal()) {
            return 1F;
        }
        return 1F - Math.clamp(value(player, PerkEffectType.GLOW_COST), 0F, 0.9F);
    }

    public static int lockpickTier(Player player) {
        return Math.round(value(player, PerkEffectType.LOCKPICK_TIER));
    }

    public static int hackTier(Player player) {
        return Math.round(value(player, PerkEffectType.HACK_TIER));
    }

    public static void onCrafted(ServerPlayer player, ItemStack crafted) {
        if (crafted.isEmpty()) {
            return;
        }
        var result = crafted.copy();
        var stats = stats(player);
        var random = player.getRandom();
        if (result.getItem() instanceof AmmoItem ammo) {
            var bonus = stats.extra(PerkEffectType.CRAFT_AMMO_BONUS);
            if (bonus > 0F) {
                var rounds = result.getCount() * ammo.roundsPerItem() * bonus;
                var whole = (int) rounds + (random.nextFloat() < rounds - (int) rounds ? 1 : 0);
                AmmoInventory.giveRounds(player, ammo, whole);
            }
        }
        if (PerkCombat.roll(random, stats.extra(PerkEffectType.CRAFT_DUPLICATE))) {
            PlayerItems.give(player, result);
        }
    }

    public static boolean fullArmorSet(Player player) {
        Object set = null;
        for (var piece : ArmorPiece.all()) {
            if (!(player.getItemBySlot(piece.slot()).getItem() instanceof ArmorPieceItem item)) {
                return false;
            }
            if (set == null) {
                set = item.set();
            } else if (!set.equals(item.set())) {
                return false;
            }
        }
        return set != null;
    }
}

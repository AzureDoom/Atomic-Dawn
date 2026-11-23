package mod.azure.atomicdawn.weapon.legendary;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.party.PartyService;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;

public final class LegendaryHooks {

    public static final TagKey<EntityType<?>> HUMANS = entityTag("humans");

    public static final TagKey<EntityType<?>> GHOULS = entityTag("ghouls");

    public static final TagKey<Item> CURRENCY = TagKey.create(Registries.ITEM, AtomicDawn.id("currency"));

    private LegendaryHooks() {}

    private static TagKey<EntityType<?>> entityTag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, AtomicDawn.id(path));
    }

    public static List<LegendaryBehavior> behaviors(Level level, ItemStack stack) {
        if (stack.isEmpty() || !Weapons.isWeapon(stack)) {
            return List.of();
        }
        var effects = ModificationResolver.legendary(level.registryAccess(), stack);
        if (effects.isEmpty()) {
            return List.of();
        }
        var behaviors = new ArrayList<LegendaryBehavior>(effects.size());
        for (var effect : effects) {
            effect.value().behavior().ifPresent(behaviors::add);
        }
        return behaviors;
    }

    public static List<LegendaryBehavior> held(Player player) {
        return behaviors(player.level(), player.getMainHandItem());
    }

    public static int specialBonus(Player player, SpecialAttribute attribute) {
        var total = 0;
        for (var behavior : held(player)) {
            total += behavior.specialBonus(player, attribute);
        }
        return total;
    }

    public static float criticalGainMultiplier(Level level, ItemStack weapon) {
        var multiplier = 1F;
        for (var behavior : behaviors(level, weapon)) {
            multiplier *= behavior.criticalGainMultiplier();
        }
        return multiplier;
    }

    public static boolean matches(Entity entity, String target) {
        if (entity == null) {
            return false;
        }
        if (target.startsWith("#")) {
            var id = Identifier.tryParse(target.substring(1));
            return id != null && is(entity, TagKey.create(Registries.ENTITY_TYPE, id));
        }
        return switch (target) {
            case "player" -> entity instanceof Player;
            case "animal" -> entity instanceof Animal;
            case "human" -> entity instanceof Player player ? !GhoulService.isGhoul(player) : is(entity, HUMANS);
            case "ghoul" -> entity instanceof Player player ? GhoulService.isGhoul(player) : is(entity, GHOULS);
            default -> false;
        };
    }

    public static boolean is(Entity entity, TagKey<EntityType<?>> tag) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(tag);
    }

    public static boolean friendly(Player attacker, Entity target) {
        if (target == attacker || !(target instanceof LivingEntity)) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isOwnedBy(attacker)) {
            return true;
        }
        if (target instanceof Player other && PartyService.sameParty(attacker, other)) {
            return true;
        }
        return attacker.isAlliedTo(target);
    }

    public static int currencyHeld(Player player) {
        var inventory = player.getInventory();
        var total = 0;
        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(CURRENCY)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean isNight(Level level) {
        var time = level.getDefaultClockTime() % 24000L;
        return level.dimensionType().hasSkyLight() && time > 13000L && time < 23000L;
    }

    public static float nightness(Level level) {
        if (!level.dimensionType().hasSkyLight()) {
            return 0F;
        }
        var time = level.getDefaultClockTime() % 24000L;
        if (time <= 13000L || time >= 23000L) {
            return 0F;
        }
        return 1F - Math.abs(time - 18000L) / 5000F;
    }
}

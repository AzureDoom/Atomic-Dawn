package mod.azure.atomicdawn.weapon.instance;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.item.WeaponItem;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;
import mod.azure.atomicdawn.weapon.modification.WeaponMods;

@SuppressWarnings("unused")
public final class Weapons {

    private static final Set<WeaponItem> WARNED = ConcurrentHashMap.newKeySet();

    private Weapons() {}

    public static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof WeaponItem;
    }

    public static @Nullable WeaponDefinition definition(Level level, ItemStack stack) {
        var base = baseDefinition(level, stack);
        if (base == null) {
            return null;
        }
        var weapon = (WeaponItem) stack.getItem();
        var access = level.registryAccess();
        return ModificationResolver.effective(
            access,
            weapon.definitionKey(),
            base,
            ModificationResolver.installed(stack, base),
            ModificationResolver.legendary(access, stack)
        );
    }

    public static @Nullable WeaponDefinition baseDefinition(Level level, ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponItem weapon)) {
            return null;
        }
        var registry = level.registryAccess().lookup(ADRegistries.WEAPON_DEFINITION);
        if (registry.isEmpty()) {
            return null;
        }
        var definition = registry.get().getValue(weapon.definitionKey());
        if (definition == null) {
            warnMissing(weapon);
        }
        return definition;
    }

    public static Optional<Item> itemFor(ResourceKey<WeaponDefinition> key) {
        return ADItems.weapons().filter(item -> ((WeaponItem) item).definitionKey().equals(key)).findFirst();
    }

    public static ItemStack createNew(Level level, Item item) {
        var stack = new ItemStack(item);
        var definition = definition(level, stack);
        if (definition != null) {
            ensureState(stack, definition);
        }
        return stack;
    }

    public static WeaponState state(ItemStack stack, WeaponDefinition definition) {
        var state = stack.get(ADDataComponents.WEAPON_STATE.get());
        if (state == null) {
            return WeaponState.factoryNew(definition);
        }
        var mode = definition.firing().sanitize(state.fireMode());
        return mode == state.fireMode() ? state : state.withFireMode(mode);
    }

    public static WeaponCondition condition(ItemStack stack, WeaponDefinition definition) {
        var max = definition.condition().max();
        var condition = stack.get(ADDataComponents.WEAPON_CONDITION.get());
        if (condition == null) {
            return WeaponCondition.pristine(max);
        }
        return condition.withMax(max);
    }

    public static WeaponState ensureState(ItemStack stack, WeaponDefinition definition) {
        if (!stack.has(ADDataComponents.WEAPON_MODS.get())) {
            stack.set(ADDataComponents.WEAPON_MODS.get(), new WeaponMods(definition.modifications().defaults()));
        }
        var condition = condition(stack, definition);
        if (definition.condition().enabled() && !condition.equals(stack.get(ADDataComponents.WEAPON_CONDITION.get()))) {
            stack.set(ADDataComponents.WEAPON_CONDITION.get(), condition);
        }
        var state = state(stack, definition);
        if (!state.equals(stack.get(ADDataComponents.WEAPON_STATE.get()))) {
            stack.set(ADDataComponents.WEAPON_STATE.get(), state);
        }
        return state;
    }

    public static void setState(ItemStack stack, WeaponState state) {
        stack.set(ADDataComponents.WEAPON_STATE.get(), state);
    }

    public static void setCondition(ItemStack stack, WeaponCondition condition) {
        stack.set(ADDataComponents.WEAPON_CONDITION.get(), condition);
    }

    public static void wear(ItemStack stack, WeaponDefinition definition, RandomSource random) {
        wear(stack, definition, random, 1F);
    }

    public static void wear(ItemStack stack, WeaponDefinition definition, RandomSource random, float lossMultiplier) {
        var condition = condition(stack, definition);
        if (!definition.condition().enabled()) {
            return;
        }
        var loss = definition.condition().lossPerUse() * Math.max(0F, lossMultiplier);
        var whole = (int) loss;
        if (random.nextFloat() < loss - whole) {
            whole++;
        }
        if (whole > 0) {
            condition = condition.withCurrent(condition.current() - whole);
            setCondition(stack, condition);
        }
    }

    public static Optional<Holder.Reference<WeaponDefinition>> definitionHolder(
        Level level,
        ResourceKey<WeaponDefinition> key
    ) {
        return level.registryAccess().lookup(ADRegistries.WEAPON_DEFINITION).flatMap(registry -> registry.get(key));
    }

    private static void warnMissing(WeaponItem weapon) {
        if (WARNED.add(weapon)) {
            AtomicDawn.LOGGER.error(
                "No weapon definition '{}' is loaded; {} will not function. Check the data pack.",
                weapon.definitionKey().identifier(),
                weapon
            );
        }
    }
}

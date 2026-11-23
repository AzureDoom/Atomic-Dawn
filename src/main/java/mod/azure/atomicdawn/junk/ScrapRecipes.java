package mod.azure.atomicdawn.junk;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampMaterials;
import mod.azure.atomicdawn.registry.ADRegistries;

public final class ScrapRecipes {

    public static final TagKey<Item> JUNK = TagKey.create(Registries.ITEM, AtomicDawn.id("junk"));

    public static final TagKey<Item> COMPONENTS = TagKey.create(Registries.ITEM, AtomicDawn.id("scrap_components"));

    private static final Map<Item, Optional<List<ScrapRecipe.Output>>> CACHE = new ConcurrentHashMap<>();

    private ScrapRecipes() {}

    public static void invalidate() {
        CACHE.clear();
    }

    public static Optional<List<ScrapRecipe.Output>> of(RegistryAccess access, Item item) {
        return CACHE.computeIfAbsent(item, key -> lookup(access, key));
    }

    private static Optional<List<ScrapRecipe.Output>> lookup(RegistryAccess access, Item item) {
        var registry = access.lookup(ADRegistries.SCRAP_RECIPE);
        if (registry.isEmpty()) {
            return Optional.empty();
        }
        var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        ScrapRecipe best = null;
        for (var entry : registry.get().listElements().toList()) {
            var recipe = entry.value();
            if (recipe.items().contains(holder) && (best == null || recipe.priority() > best.priority())) {
                best = recipe;
            }
        }
        return best == null || best.results().isEmpty() ? Optional.empty() : Optional.of(best.results());
    }

    public static boolean plain(ItemStack stack) {
        return !stack.isEmpty() && stack.isComponentsPatchEmpty() && !stack.isDamageableItem();
    }

    public static boolean scrappable(RegistryAccess access, ItemStack stack) {
        return plain(stack) && stack.is(JUNK) && of(access, stack.getItem()).isPresent();
    }

    public static boolean storable(RegistryAccess access, ItemStack stack) {
        return plain(stack) && (stack.is(JUNK) || stack.is(COMPONENTS) || stack.is(CampMaterials.SCRAP) || of(
            access,
            stack.getItem()
        ).isPresent());
    }
}

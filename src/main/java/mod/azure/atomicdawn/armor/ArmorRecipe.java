package mod.azure.atomicdawn.armor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public record ArmorRecipe(
    Item result,
    int count,
    List<SizedIngredient> cost,
    int order
) {

    public enum Category implements StringRepresentable {

        VANILLA,
        LIGHT,
        STURDY,
        HEAVY;

        public String translationKey() {
            return "armor.atomicdawn.bench.category." + getSerializedName();
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<ArmorRecipe> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(ArmorRecipe::result),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(ArmorRecipe::count),
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("cost").forGetter(ArmorRecipe::cost),
            Codec.INT.optionalFieldOf("order", 0).forGetter(ArmorRecipe::order)
        ).apply(instance, ArmorRecipe::new)
    );

    private static final Comparator<Holder.Reference<ArmorRecipe>> ORDER =
        Comparator.comparing((Holder.Reference<ArmorRecipe> holder) -> holder.value().category())
            .thenComparingInt(holder -> holder.value().order())
            .thenComparing(holder -> holder.key().identifier().toString());

    public Category category() {
        if (result instanceof ArmorPieceItem item) {
            return switch (item.set().weight()) {
                case LIGHT -> Category.LIGHT;
                case STURDY -> Category.STURDY;
                case HEAVY -> Category.HEAVY;
            };
        }
        return Category.VANILLA;
    }

    public ItemStack resultStack() {
        return new ItemStack(result, count);
    }

    public static List<Holder.Reference<ArmorRecipe>> all(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.ARMOR_RECIPE)
            .map(registry -> registry.listElements().sorted(ORDER).toList())
            .orElse(List.of());
    }

    public static List<Holder.Reference<ArmorRecipe>> all(RegistryAccess access, Category category) {
        return all(access).stream().filter(holder -> holder.value().category() == category).toList();
    }

    public static Optional<ArmorRecipe> forResult(RegistryAccess access, Item item) {
        return all(access).stream().map(Holder.Reference::value).filter(recipe -> recipe.result() == item).findFirst();
    }
}

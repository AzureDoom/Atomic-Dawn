package mod.azure.atomicdawn.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public record TinkerRecipe(
    Category category,
    Item result,
    int count,
    List<SizedIngredient> cost,
    int order
) {

    public static final Codec<TinkerRecipe> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Category.CODEC.fieldOf("category").forGetter(TinkerRecipe::category),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(TinkerRecipe::result),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(TinkerRecipe::count),
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("cost").forGetter(TinkerRecipe::cost),
            Codec.INT.optionalFieldOf("order", 0).forGetter(TinkerRecipe::order)
        ).apply(instance, TinkerRecipe::new)
    );

    public ItemStack resultStack() {
        return new ItemStack(result, count);
    }

    public enum Category implements StringRepresentable {

        AMMO,
        REPAIR,
        LEGENDARY,
        CHEMS;

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

        public String translationKey() {
            return "screen.atomicdawn.tinkers_bench.tab." + getSerializedName();
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}

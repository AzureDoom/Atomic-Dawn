package mod.azure.atomicdawn.powerarmor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Comparator;
import java.util.List;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;

public record PowerArmorRecipe(
    Item result,
    int count,
    List<SizedIngredient> cost,
    int order
) {

    public static final Codec<PowerArmorRecipe> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(PowerArmorRecipe::result),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(PowerArmorRecipe::count),
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("cost").forGetter(PowerArmorRecipe::cost),
            Codec.INT.optionalFieldOf("order", 0).forGetter(PowerArmorRecipe::order)
        ).apply(instance, PowerArmorRecipe::new)
    );

    private static final Comparator<Holder.Reference<PowerArmorRecipe>> ORDER =
        Comparator.comparingInt((Holder.Reference<PowerArmorRecipe> holder) -> holder.value().order())
            .thenComparing(holder -> holder.key().identifier().toString());

    public ItemStack resultStack() {
        return new ItemStack(result, count);
    }

    public static List<Holder.Reference<PowerArmorRecipe>> all(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.POWER_ARMOR_RECIPE)
            .map(registry -> registry.listElements().sorted(ORDER).toList())
            .orElse(List.of());
    }
}

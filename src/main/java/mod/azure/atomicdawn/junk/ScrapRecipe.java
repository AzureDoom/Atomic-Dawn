package mod.azure.atomicdawn.junk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ScrapRecipe(
    HolderSet<Item> items,
    List<Output> results,
    int priority
) {

    public record Output(
        Item item,
        int count
    ) {

        public static final Codec<Output> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Output::item),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(Output::count)
            ).apply(instance, Output::new)
        );

        public ItemStack stack(long multiplier) {
            return new ItemStack(item, (int) Math.min(Integer.MAX_VALUE, count * multiplier));
        }
    }

    @SuppressWarnings("NullableProblems")
    public static final Codec<ScrapRecipe> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            RegistryCodecs.holderSet(Registries.ITEM).fieldOf("items").forGetter(ScrapRecipe::items),
            Output.CODEC.listOf().fieldOf("results").forGetter(ScrapRecipe::results),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(ScrapRecipe::priority)
        ).apply(instance, ScrapRecipe::new)
    );
}

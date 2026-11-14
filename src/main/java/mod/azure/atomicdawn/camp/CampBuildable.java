package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.registry.ADRegistries;

public record CampBuildable(
    Block block,
    Category category,
    List<SizedIngredient> cost,
    int budget,
    int limit,
    Optional<Identifier> plan,
    List<PlacementMode> modes,
    int order
) {

    public enum Category implements StringRepresentable {

        STRUCTURE,
        DOORS,
        LIGHTING,
        CRAFTING,
        STORAGE,
        FURNITURE,
        DECOR;

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

        public Component label() {
            return Component.translatable("camp.atomicdawn.category." + getSerializedName());
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<CampBuildable> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(CampBuildable::block),
            Category.CODEC.optionalFieldOf("category", Category.STRUCTURE).forGetter(CampBuildable::category),
            SizedIngredient.NESTED_CODEC.listOf().optionalFieldOf("cost", List.of()).forGetter(CampBuildable::cost),
            Codec.intRange(0, 10000).optionalFieldOf("budget", 2).forGetter(CampBuildable::budget),
            Codec.intRange(0, 10000).optionalFieldOf("limit", 0).forGetter(CampBuildable::limit),
            Identifier.CODEC.optionalFieldOf("plan").forGetter(CampBuildable::plan),
            PlacementMode.CODEC.listOf()
                .optionalFieldOf("modes", List.of(PlacementMode.COLLISION))
                .forGetter(CampBuildable::modes),
            Codec.INT.optionalFieldOf("order", 0).forGetter(CampBuildable::order)
        ).apply(instance, CampBuildable::new)
    );

    private static final Comparator<Holder.Reference<CampBuildable>> ORDER = Comparator
        .comparing((Holder.Reference<CampBuildable> holder) -> holder.value().category())
        .thenComparingInt(holder -> holder.value().order())
        .thenComparing(holder -> holder.key().identifier().toString());

    public CampBuildable {
        modes = modes.isEmpty() ? List.of(PlacementMode.COLLISION) : List.copyOf(modes);
    }

    public boolean placeable() {
        return block.asItem() instanceof BlockItem;
    }

    public ItemStack icon() {
        return new ItemStack(block.asItem());
    }

    public Component name() {
        return block.getName();
    }

    public PlacementMode defaultMode() {
        return modes.getFirst();
    }

    public static List<Holder.Reference<CampBuildable>> all(RegistryAccess access) {
        return access.lookup(ADRegistries.CAMP_BUILDABLE)
            .map(
                registry -> registry.listElements()
                    .filter(holder -> holder.value().placeable())
                    .sorted(ORDER)
                    .toList()
            )
            .orElse(List.of());
    }

    public static Optional<Holder.Reference<CampBuildable>> forBlock(RegistryAccess access, Block block) {
        return all(access).stream().filter(holder -> holder.value().block() == block).findFirst();
    }

    public static Optional<CampBuildable> get(RegistryAccess access, Identifier id) {
        return access.lookup(ADRegistries.CAMP_BUILDABLE)
            .flatMap(registry -> registry.get(ResourceKey.create(ADRegistries.CAMP_BUILDABLE, id)))
            .map(Holder.Reference::value)
            .filter(CampBuildable::placeable);
    }
}

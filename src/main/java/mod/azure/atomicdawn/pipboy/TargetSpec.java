package mod.azure.atomicdawn.pipboy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public record TargetSpec(
    boolean tag,
    Identifier id
) {

    public static final Codec<TargetSpec> CODEC = Codec.STRING.comapFlatMap(
        TargetSpec::parse,
        TargetSpec::toString
    );

    public static DataResult<TargetSpec> parse(String raw) {
        var tag = raw.startsWith("#");
        var id = Identifier.tryParse(tag ? raw.substring(1) : raw);
        return id == null
            ? DataResult.error(() -> "Invalid target '" + raw + "'")
            : DataResult.success(new TargetSpec(tag, id));
    }

    @Override
    public @NonNull String toString() {
        return (tag ? "#" : "") + id;
    }

    public boolean matchesEntity(EntityType<?> type) {
        if (tag) {
            return type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, id));
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(id);
    }

    public boolean matchesItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (tag) {
            return stack.is(TagKey.create(Registries.ITEM, id));
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    public boolean matchesBiome(Holder<Biome> biome) {
        if (tag) {
            return biome.is(TagKey.create(Registries.BIOME, id));
        }
        return biome.unwrapKey().map(key -> key.identifier().equals(id)).orElse(false);
    }

    public boolean matchesStructure(RegistryAccess access, Identifier structure) {
        if (!tag) {
            return id.equals(structure);
        }
        return access.lookup(Registries.STRUCTURE)
            .flatMap(registry -> registry.get(ResourceKey.create(Registries.STRUCTURE, structure)))
            .map(holder -> holder.is(TagKey.create(Registries.STRUCTURE, id)))
            .orElse(false);
    }

    public boolean matchesId(Identifier value) {
        return !tag && id.equals(value);
    }

    public Component entityName() {
        if (tag) {
            return PipBoyNames.tag("entity_type", id);
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id)
            .map(EntityType::getDescription)
            .orElseGet(() -> Component.literal(PipBoyNames.pretty(id.getPath())));
    }

    public Component itemName() {
        if (tag) {
            return PipBoyNames.tag("item", id);
        }
        return BuiltInRegistries.ITEM.getOptional(id)
            .map(item -> new ItemStack(item).getHoverName())
            .orElseGet(() -> Component.literal(PipBoyNames.pretty(id.getPath())));
    }

    public Component biomeName() {
        return tag ? PipBoyNames.tag("worldgen.biome", id) : PipBoyNames.biome(id);
    }

    public Component structureName() {
        return tag ? PipBoyNames.tag("worldgen.structure", id) : PipBoyNames.location(id);
    }
}

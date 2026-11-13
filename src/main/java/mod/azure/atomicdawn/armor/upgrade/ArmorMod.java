package mod.azure.atomicdawn.armor.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.armor.ArmorPieceItem;
import mod.azure.atomicdawn.registry.ADRegistries;

public record ArmorMod(
    ArmorModKind kind,
    List<ArmorPiece> pieces,
    List<String> sets,
    List<String> exclude,
    List<SizedIngredient> cost,
    List<ArmorEffect> effects,
    int order
) {

    public static final Codec<ArmorMod> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            ArmorModKind.CODEC.fieldOf("kind").forGetter(ArmorMod::kind),
            ArmorPiece.CODEC.listOf().optionalFieldOf("pieces", List.of()).forGetter(ArmorMod::pieces),
            Codec.STRING.listOf().optionalFieldOf("sets", List.of()).forGetter(ArmorMod::sets),
            Codec.STRING.listOf().optionalFieldOf("exclude", List.of()).forGetter(ArmorMod::exclude),
            SizedIngredient.NESTED_CODEC.listOf().optionalFieldOf("cost", List.of()).forGetter(ArmorMod::cost),
            ArmorEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ArmorMod::effects),
            Codec.INT.optionalFieldOf("order", 0).forGetter(ArmorMod::order)
        ).apply(instance, ArmorMod::new)
    );

    private static final Comparator<Holder.Reference<ArmorMod>> ORDER =
        Comparator.comparing((Holder.Reference<ArmorMod> holder) -> holder.value().kind())
            .thenComparingInt(holder -> holder.value().order())
            .thenComparing(holder -> holder.key().identifier().toString());

    public boolean fits(ArmorPieceItem item) {
        if (!pieces.isEmpty() && !pieces.contains(item.piece())) {
            return false;
        }
        var set = item.set();
        if (!sets.isEmpty() && sets.stream().noneMatch(set::matches)) {
            return false;
        }
        return exclude.stream().noneMatch(set::matches);
    }

    public static List<Holder.Reference<ArmorMod>> all(RegistryAccess access) {
        return access.lookup(ADRegistries.ARMOR_MOD)
            .map(registry -> registry.listElements().sorted(ORDER).toList())
            .orElse(List.of());
    }

    public static List<Holder.Reference<ArmorMod>> fitting(
        RegistryAccess access,
        ArmorPieceItem item,
        ArmorModKind kind
    ) {
        return all(access).stream()
            .filter(holder -> holder.value().kind() == kind && holder.value().fits(item))
            .toList();
    }

    public static Optional<Holder.Reference<ArmorMod>> get(RegistryAccess access, Identifier id) {
        return access.lookup(ADRegistries.ARMOR_MOD)
            .flatMap(registry -> registry.get(ADRegistries.key(ADRegistries.ARMOR_MOD, id)));
    }

    public static String translationKey(Identifier id) {
        return "armor_mod." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}

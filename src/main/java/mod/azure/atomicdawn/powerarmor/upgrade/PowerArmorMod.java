package mod.azure.atomicdawn.powerarmor.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.registry.ADRegistries;

public record PowerArmorMod(
    PowerArmorModSlot slot,
    String group,
    List<String> sets,
    List<SizedIngredient> cost,
    List<PowerArmorEffect> effects,
    int order
) {

    public static final Codec<PowerArmorMod> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            PowerArmorModSlot.CODEC.fieldOf("slot").forGetter(PowerArmorMod::slot),
            Codec.STRING.fieldOf("group").forGetter(PowerArmorMod::group),
            Codec.STRING.listOf().optionalFieldOf("sets", List.of()).forGetter(PowerArmorMod::sets),
            SizedIngredient.NESTED_CODEC.listOf().optionalFieldOf("cost", List.of()).forGetter(PowerArmorMod::cost),
            PowerArmorEffect.CODEC.listOf().fieldOf("effects").forGetter(PowerArmorMod::effects),
            Codec.INT.optionalFieldOf("order", 0).forGetter(PowerArmorMod::order)
        ).apply(instance, PowerArmorMod::new)
    );

    private static final Comparator<Holder.Reference<PowerArmorMod>> ORDER =
        Comparator.comparing((Holder.Reference<PowerArmorMod> holder) -> holder.value().group())
            .thenComparingInt(holder -> holder.value().order())
            .thenComparing(holder -> holder.key().identifier().toString());

    public boolean fits(PowerArmorPieceItem piece) {
        return slot == PowerArmorModSlot.of(piece.slot()) && (sets.isEmpty() || sets.contains(piece.set()));
    }

    public static List<Holder.Reference<PowerArmorMod>> all(RegistryAccess access) {
        return access.lookup(ADRegistries.POWER_ARMOR_MOD)
            .map(registry -> registry.listElements().sorted(ORDER).toList())
            .orElse(List.of());
    }

    public static Optional<Holder.Reference<PowerArmorMod>> get(RegistryAccess access, Identifier id) {
        return access.lookup(ADRegistries.POWER_ARMOR_MOD)
            .flatMap(
                registry -> registry.get(
                    ADRegistries.key(
                        ADRegistries.POWER_ARMOR_MOD,
                        id
                    )
                )
            );
    }

    public static String translationKey(Identifier id) {
        return "power_armor_mod." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}

package mod.azure.atomicdawn.armor.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.registry.ADDataComponents;

public record ArmorUpgrades(
    Optional<Identifier> material,
    Optional<Identifier> misc,
    List<Identifier> legendary
) {

    public static final ArmorUpgrades EMPTY = new ArmorUpgrades(Optional.empty(), Optional.empty(), List.of());

    public static final Codec<ArmorUpgrades> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("material").forGetter(ArmorUpgrades::material),
            Identifier.CODEC.optionalFieldOf("misc").forGetter(ArmorUpgrades::misc),
            Identifier.CODEC.listOf().optionalFieldOf("legendary", List.of()).forGetter(ArmorUpgrades::legendary)
        ).apply(instance, ArmorUpgrades::new)
    );

    public static final StreamCodec<ByteBuf, ArmorUpgrades> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(Identifier.STREAM_CODEC),
        ArmorUpgrades::material,
        ByteBufCodecs.optional(Identifier.STREAM_CODEC),
        ArmorUpgrades::misc,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(3)),
        ArmorUpgrades::legendary,
        ArmorUpgrades::new
    );

    public ArmorUpgrades {
        legendary = List.copyOf(legendary.size() > 3 ? legendary.subList(0, 3) : legendary);
    }

    public static ArmorUpgrades of(ItemStack stack) {
        var upgrades = stack.get(ADDataComponents.ARMOR_UPGRADES.get());
        return upgrades == null ? EMPTY : upgrades;
    }

    public static void set(ItemStack stack, ArmorUpgrades upgrades) {
        if (upgrades.isEmpty()) {
            stack.remove(ADDataComponents.ARMOR_UPGRADES.get());
        } else {
            stack.set(ADDataComponents.ARMOR_UPGRADES.get(), upgrades);
        }
    }

    public boolean isEmpty() {
        return material.isEmpty() && misc.isEmpty() && legendary.isEmpty();
    }

    public Optional<Identifier> mod(ArmorModKind kind) {
        return kind == ArmorModKind.MATERIAL ? material : misc;
    }

    public List<Identifier> mods() {
        var result = new ArrayList<Identifier>(2);
        material.ifPresent(result::add);
        misc.ifPresent(result::add);
        return result;
    }

    public boolean hasMod(Identifier id) {
        return material.filter(id::equals).isPresent() || misc.filter(id::equals).isPresent();
    }

    public ArmorUpgrades withMod(ArmorModKind kind, Identifier id) {
        return kind == ArmorModKind.MATERIAL
            ? new ArmorUpgrades(Optional.of(id), misc, legendary)
            : new ArmorUpgrades(material, Optional.of(id), legendary);
    }

    public ArmorUpgrades withoutMod(Identifier id) {
        return new ArmorUpgrades(
            material.filter(value -> !value.equals(id)),
            misc.filter(value -> !value.equals(id)),
            legendary
        );
    }

    public ArmorUpgrades withLegendary(RegistryAccess access, int star, Identifier effect) {
        var copy = new ArrayList<Identifier>();
        for (var id : legendary) {
            var existing = ArmorLegendary.get(access, id);
            if (existing.isEmpty() || existing.get().value().star() != star) {
                copy.add(id);
            }
        }
        copy.add(effect);
        return new ArmorUpgrades(material, misc, copy);
    }

    @Nullable
    public Identifier legendary(RegistryAccess access, int star) {
        for (var id : legendary) {
            var existing = ArmorLegendary.get(access, id);
            if (existing.isPresent() && existing.get().value().star() == star) {
                return id;
            }
        }
        return null;
    }
}

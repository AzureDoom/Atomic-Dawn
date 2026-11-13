package mod.azure.atomicdawn.powerarmor.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mod.azure.atomicdawn.registry.ADDataComponents;

public record PowerArmorUpgrades(
    Map<String, Identifier> mods,
    List<Identifier> legendary
) {

    public static final PowerArmorUpgrades EMPTY = new PowerArmorUpgrades(Map.of(), List.of());

    public static final Codec<PowerArmorUpgrades> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Identifier.CODEC)
                .optionalFieldOf("mods", Map.of())
                .forGetter(PowerArmorUpgrades::mods),
            Identifier.CODEC.listOf().optionalFieldOf("legendary", List.of()).forGetter(PowerArmorUpgrades::legendary)
        ).apply(instance, PowerArmorUpgrades::new)
    );

    public static final StreamCodec<ByteBuf, PowerArmorUpgrades> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, Identifier.STREAM_CODEC, 16),
        PowerArmorUpgrades::mods,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(3)),
        PowerArmorUpgrades::legendary,
        PowerArmorUpgrades::new
    );

    public PowerArmorUpgrades {
        mods = Map.copyOf(mods);
        legendary = List.copyOf(legendary.size() > 3 ? legendary.subList(0, 3) : legendary);
    }

    public static PowerArmorUpgrades of(ItemStack stack) {
        var upgrades = stack.get(ADDataComponents.POWER_ARMOR_UPGRADES.get());
        return upgrades == null ? EMPTY : upgrades;
    }

    public boolean isEmpty() {
        return mods.isEmpty() && legendary.isEmpty();
    }

    public PowerArmorUpgrades withMod(String group, Identifier mod) {
        var copy = new LinkedHashMap<>(mods);
        copy.put(group, mod);
        return new PowerArmorUpgrades(copy, legendary);
    }

    public PowerArmorUpgrades withoutMod(Identifier mod) {
        var copy = new LinkedHashMap<>(mods);
        copy.values().removeIf(mod::equals);
        return new PowerArmorUpgrades(copy, legendary);
    }

    public PowerArmorUpgrades withoutMods() {
        return new PowerArmorUpgrades(Map.of(), legendary);
    }

    public PowerArmorUpgrades withLegendary(RegistryAccess access, int star, Identifier effect) {
        var copy = new ArrayList<Identifier>();
        for (var id : legendary) {
            var existing = PowerArmorLegendary.get(access, id);
            if (existing.isEmpty() || existing.get().value().star() != star) {
                copy.add(id);
            }
        }
        copy.add(effect);
        return new PowerArmorUpgrades(mods, copy);
    }

    public Identifier legendary(RegistryAccess access, int star) {
        for (var id : legendary) {
            var existing = PowerArmorLegendary.get(access, id);
            if (existing.isPresent() && existing.get().value().star() == star) {
                return id;
            }
        }
        return null;
    }
}

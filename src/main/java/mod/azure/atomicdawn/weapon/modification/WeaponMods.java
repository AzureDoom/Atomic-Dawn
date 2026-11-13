package mod.azure.atomicdawn.weapon.modification;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import mod.azure.atomicdawn.registry.ADRegistries;

@SuppressWarnings({ "OptionalUsedAsFieldOrParameterType", "unused" })
public record WeaponMods(Map<ModSlot, ResourceKey<WeaponModification>> installed) {

    public static final WeaponMods NONE = new WeaponMods(Map.of());

    private static final Codec<ResourceKey<WeaponModification>> KEY_CODEC = ResourceKey.codec(
        ADRegistries.WEAPON_MODIFICATION
    );

    public static final Codec<WeaponMods> CODEC = Codec.unboundedMap(ModSlot.CODEC, KEY_CODEC)
        .xmap(WeaponMods::new, WeaponMods::installed);

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponMods> STREAM_CODEC = ByteBufCodecs
        .<RegistryFriendlyByteBuf, ModSlot, ResourceKey<WeaponModification>, Map<ModSlot, ResourceKey<WeaponModification>>>map(
            size -> new EnumMap<>(ModSlot.class),
            ModSlot.STREAM_CODEC,
            ResourceKey.streamCodec(ADRegistries.WEAPON_MODIFICATION)
        )
        .map(WeaponMods::new, WeaponMods::installed);

    public WeaponMods {
        installed = installed.isEmpty() ? Map.of() : Map.copyOf(installed);
    }

    public Optional<ResourceKey<WeaponModification>> get(ModSlot slot) {
        return Optional.ofNullable(installed.get(slot));
    }

    public WeaponMods with(ModSlot slot, Optional<ResourceKey<WeaponModification>> mod) {
        var copy = new EnumMap<ModSlot, ResourceKey<WeaponModification>>(ModSlot.class);
        copy.putAll(installed);
        mod.ifPresentOrElse(key -> copy.put(slot, key), () -> copy.remove(slot));
        return new WeaponMods(copy);
    }
}

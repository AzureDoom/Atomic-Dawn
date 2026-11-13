package mod.azure.atomicdawn.progression;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.weapon.modification.WeaponModification;

public record KnownMods(Set<ResourceKey<WeaponModification>> known) {

    public static final KnownMods EMPTY = new KnownMods(Set.of());

    public static final MapCodec<KnownMods> MAP_CODEC = ResourceKey.codec(ADRegistries.WEAPON_MODIFICATION)
        .listOf()
        .fieldOf("known")
        .xmap(list -> new KnownMods(Set.copyOf(list)), mods -> List.copyOf(mods.known()));

    public static final StreamCodec<ByteBuf, KnownMods> STREAM_CODEC = ResourceKey
        .streamCodec(ADRegistries.WEAPON_MODIFICATION)
        .apply(ByteBufCodecs.list())
        .map(list -> new KnownMods(Set.copyOf(list)), mods -> List.copyOf(mods.known()));

    public KnownMods {
        known = Set.copyOf(known);
    }

    public boolean knows(ResourceKey<WeaponModification> mod) {
        return known.contains(mod);
    }

    public KnownMods with(ResourceKey<WeaponModification> mod) {
        if (known.contains(mod)) {
            return this;
        }
        var copy = new HashSet<>(known);
        copy.add(mod);
        return new KnownMods(copy);
    }
}

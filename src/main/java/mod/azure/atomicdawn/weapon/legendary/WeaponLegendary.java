package mod.azure.atomicdawn.weapon.legendary;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.registry.ADRegistries;

public record WeaponLegendary(List<ResourceKey<LegendaryEffect>> effects) {

    public static final WeaponLegendary NONE = new WeaponLegendary(List.of());

    private static final Codec<ResourceKey<LegendaryEffect>> KEY_CODEC = ResourceKey.codec(
        ADRegistries.LEGENDARY_EFFECT
    );

    public static final Codec<WeaponLegendary> CODEC = KEY_CODEC.listOf()
        .xmap(WeaponLegendary::new, WeaponLegendary::effects);

    public static final StreamCodec<ByteBuf, WeaponLegendary> STREAM_CODEC = ResourceKey
        .streamCodec(ADRegistries.LEGENDARY_EFFECT)
        .apply(ByteBufCodecs.list(4))
        .map(WeaponLegendary::new, WeaponLegendary::effects);

    public WeaponLegendary {
        effects = List.copyOf(effects);
    }

    public boolean isEmpty() {
        return effects.isEmpty();
    }

    public WeaponLegendary with(int star, ResourceKey<LegendaryEffect> effect, List<Integer> starsOfExisting) {
        var list = new ArrayList<ResourceKey<LegendaryEffect>>();
        for (var i = 0; i < effects.size(); i++) {
            if (starsOfExisting.get(i) != star) {
                list.add(effects.get(i));
            }
        }
        list.add(effect);
        return new WeaponLegendary(list);
    }
}

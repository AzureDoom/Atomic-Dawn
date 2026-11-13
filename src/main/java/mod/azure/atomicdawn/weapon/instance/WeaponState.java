package mod.azure.atomicdawn.weapon.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import mod.azure.atomicdawn.weapon.definition.FireMode;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public record WeaponState(
    int loadedAmmo,
    FireMode fireMode
) {

    public static final Codec<WeaponState> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("loaded_ammo").forGetter(WeaponState::loadedAmmo),
            FireMode.CODEC.optionalFieldOf("fire_mode", FireMode.SEMI_AUTO).forGetter(WeaponState::fireMode)
        ).apply(instance, WeaponState::new)
    );

    public static final StreamCodec<ByteBuf, WeaponState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        WeaponState::loadedAmmo,
        FireMode.STREAM_CODEC,
        WeaponState::fireMode,
        WeaponState::new
    );

    public static WeaponState factoryNew(WeaponDefinition definition) {
        return new WeaponState(definition.ammo().magazineSize(), definition.firing().defaultMode());
    }

    public WeaponState withLoadedAmmo(int ammo) {
        return new WeaponState(Math.max(0, ammo), fireMode);
    }

    public WeaponState withFireMode(FireMode mode) {
        return new WeaponState(loadedAmmo, mode);
    }

    public boolean isEmpty() {
        return loadedAmmo <= 0;
    }
}

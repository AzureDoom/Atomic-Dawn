package mod.azure.atomicdawn.camp;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.helper.ADSerialization;

public final class CampNbt {

    public static final Codec<CompoundTag> CODEC = Codec.withAlternative(
        Codec.STRING.comapFlatMap(CampNbt::parse, CompoundTag::toString),
        CompoundTag.CODEC
    );

    private CampNbt() {}

    private static DataResult<CompoundTag> parse(String value) {
        try {
            return DataResult.success(ADSerialization.parseCompound(value));
        } catch (CommandSyntaxException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    public static Vec3 rotate(Vec3 value, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new Vec3(-value.z, value.y, value.x);
            case CLOCKWISE_180 -> new Vec3(-value.x, value.y, -value.z);
            case COUNTERCLOCKWISE_90 -> new Vec3(value.z, value.y, -value.x);
            default -> value;
        };
    }
}

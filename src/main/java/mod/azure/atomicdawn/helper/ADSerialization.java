package mod.azure.atomicdawn.helper;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

public final class ADSerialization {

    private ADSerialization() {}

    public static int getInt(ValueInput input, String key, int fallback) {
        return input.getIntOr(key, fallback);
    }

    public static long getLong(ValueInput input, String key, long fallback) {
        return input.getLongOr(key, fallback);
    }

    public static float getFloat(ValueInput input, String key, float fallback) {
        return input.getFloatOr(key, fallback);
    }

    public static double getDouble(ValueInput input, String key, double fallback) {
        return input.getDoubleOr(key, fallback);
    }

    public static boolean getBoolean(ValueInput input, String key, boolean fallback) {
        return input.getBooleanOr(key, fallback);
    }

    public static String getString(ValueInput input, String key, String fallback) {
        return input.getStringOr(key, fallback);
    }

    public static <T> Optional<T> read(ValueInput input, String key, Codec<T> codec) {
        return input.read(key, codec);
    }

    public static <T> void store(ValueOutput output, String key, Codec<T> codec, T value) {
        output.store(key, codec, value);
    }

    public static int getInt(CompoundTag tag, String key, int fallback) {
        return tag.getIntOr(key, fallback);
    }

    public static long getLong(CompoundTag tag, String key, long fallback) {
        return tag.getLongOr(key, fallback);
    }

    public static float getFloat(CompoundTag tag, String key, float fallback) {
        return tag.getFloatOr(key, fallback);
    }

    public static double getDouble(CompoundTag tag, String key, double fallback) {
        return tag.getDoubleOr(key, fallback);
    }

    public static boolean getBoolean(CompoundTag tag, String key, boolean fallback) {
        return tag.getBooleanOr(key, fallback);
    }

    public static String getString(CompoundTag tag, String key, String fallback) {
        return tag.getStringOr(key, fallback);
    }

    public static int[] getIntArray(CompoundTag tag, String key, int[] fallback) {
        return tag.getIntArray(key).orElse(fallback);
    }

    public static ListTag getList(CompoundTag tag, String key) {
        return tag.getListOrEmpty(key);
    }

    public static CompoundTag parseCompound(String value) throws CommandSyntaxException {
        return TagParser.parseCompoundFully(value);
    }

    public static CompoundTag saveEntity(Entity entity, HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    public static void loadEntity(Entity entity, CompoundTag data, HolderLookup.Provider registries) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, data));
    }

    public static CompoundTag saveBlockEntity(BlockEntity blockEntity, HolderLookup.Provider registries) {
        return blockEntity.saveWithoutMetadata(registries);
    }

    public static void loadBlockEntity(BlockEntity blockEntity, CompoundTag data, HolderLookup.Provider registries) {
        blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, data));
    }
}

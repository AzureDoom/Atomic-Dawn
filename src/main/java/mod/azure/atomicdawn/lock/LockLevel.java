package mod.azure.atomicdawn.lock;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum LockLevel implements StringRepresentable {

    NOVICE(14F, 46F, 0.10F, ChatFormatting.GREEN),
    ADVANCED(9F, 38F, 0.14F, ChatFormatting.YELLOW),
    EXPERT(5.5F, 30F, 0.19F, ChatFormatting.GOLD),
    MASTER(3F, 24F, 0.25F, ChatFormatting.RED);

    public static final Codec<LockLevel> CODEC = StringRepresentable.fromEnum(LockLevel::values);

    private static final LockLevel[] VALUES = values();

    private final float fullWidth;

    private final float partialWidth;

    private final float strainDamage;

    private final ChatFormatting color;

    LockLevel(float fullWidth, float partialWidth, float strainDamage, ChatFormatting color) {
        this.fullWidth = fullWidth;
        this.partialWidth = partialWidth;
        this.strainDamage = strainDamage;
        this.color = color;
    }

    public static LockLevel byIndex(int index) {
        return VALUES[Math.clamp(index, 0, VALUES.length - 1)];
    }

    public static @Nullable LockLevel byName(String name) {
        for (var level : VALUES) {
            if (level.getSerializedName().equalsIgnoreCase(name)) {
                return level;
            }
        }
        return null;
    }

    public int requiredTier() {
        return ordinal();
    }

    public float fullWidth() {
        return fullWidth;
    }

    public float partialWidth() {
        return partialWidth;
    }

    public float strainDamage() {
        return strainDamage;
    }

    public ChatFormatting color() {
        return color;
    }

    public String translationKey() {
        return "lock.atomicdawn.level." + getSerializedName();
    }

    public MutableComponent component() {
        return Component.translatable(translationKey()).withStyle(color);
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

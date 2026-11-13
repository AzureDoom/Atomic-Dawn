package mod.azure.atomicdawn.quest;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum QuestCategory implements StringRepresentable {

    MAIN,
    SIDE,
    MISC;

    public static final Codec<QuestCategory> CODEC = StringRepresentable.fromEnum(QuestCategory::values);

    public String translationKey() {
        return "quest_category.atomicdawn." + getSerializedName();
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

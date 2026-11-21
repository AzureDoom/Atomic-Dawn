package mod.azure.atomicdawn.quest;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum ObjectiveType implements StringRepresentable {

    KILL("kill", true),
    COLLECT("collect", true),
    DISCOVER("discover", true),
    VISIT_BIOME("visit_biome", true),
    REACH_LEVEL("reach_level", false),
    HOLOTAPE("holotape", true),
    STUDY("study", true),
    TALK("talk", true),
    ESCORT("escort", true),
    ACTION("action", true);

    public static final Codec<ObjectiveType> CODEC = StringRepresentable.fromEnum(ObjectiveType::values);

    private final String serializedName;

    private final boolean needsTarget;

    ObjectiveType(String serializedName, boolean needsTarget) {
        this.serializedName = serializedName;
        this.needsTarget = needsTarget;
    }

    public boolean needsTarget() {
        return needsTarget;
    }

    public String translationKey() {
        return "quest_objective.atomicdawn." + serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return serializedName;
    }
}

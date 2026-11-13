package mod.azure.atomicdawn.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Optional;

import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.TargetSpec;

public record QuestObjective(
    ObjectiveType type,
    Optional<TargetSpec> target,
    int count,
    Optional<String> description,
    Optional<Location> location
) {

    public record Location(
        BlockPos pos,
        int radius,
        Optional<Identifier> dimension
    ) {

        public static final Codec<Location> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Location::pos),
                Codec.intRange(1, 256).optionalFieldOf("radius", 8).forGetter(Location::radius),
                Identifier.CODEC.optionalFieldOf("dimension").forGetter(Location::dimension)
            ).apply(instance, Location::new)
        );
    }

    public static final Codec<QuestObjective> CODEC = RecordCodecBuilder.<QuestObjective>create(
        instance -> instance.group(
            ObjectiveType.CODEC.fieldOf("type").forGetter(QuestObjective::type),
            TargetSpec.CODEC.optionalFieldOf("target").forGetter(QuestObjective::target),
            Codec.intRange(1, 1_000_000).optionalFieldOf("count", 1).forGetter(QuestObjective::count),
            Codec.STRING.optionalFieldOf("description").forGetter(QuestObjective::description),
            Location.CODEC.optionalFieldOf("location").forGetter(QuestObjective::location)
        ).apply(instance, QuestObjective::new)
    ).validate(QuestObjective::validate);

    private static DataResult<QuestObjective> validate(QuestObjective objective) {
        if (objective.type == ObjectiveType.ESCORT && objective.location.isEmpty()) {
            return DataResult.error(() -> "Objective type 'escort' needs a location");
        }
        if (objective.type.needsTarget() && objective.target.isEmpty()) {
            return DataResult.error(() -> "Objective type '" + objective.type.getSerializedName() + "' needs a target");
        }
        return DataResult.success(objective);
    }

    public Component describe() {
        if (description.isPresent()) {
            return PipBoyNames.fromKey(description.get());
        }
        var targetName = target.map(spec -> switch (type) {
            case KILL, STUDY -> spec.entityName();
            case COLLECT -> spec.itemName();
            case DISCOVER -> spec.structureName();
            case VISIT_BIOME -> spec.biomeName();
            case HOLOTAPE -> PipBoyNames.holotape(spec.id());
            case REACH_LEVEL -> Component.empty();
            case TALK, ESCORT -> PipBoyNames.fromKey("npc." + spec.id().getNamespace() + "." + spec.id().getPath());
        }).orElse(Component.empty());
        return Component.translatable(type.translationKey(), targetName, count);
    }
}

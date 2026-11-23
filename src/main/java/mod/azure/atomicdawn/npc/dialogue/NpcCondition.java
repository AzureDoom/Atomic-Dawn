package mod.azure.atomicdawn.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.quest.QuestService;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialService;
import mod.azure.atomicdawn.special.SpecialStatResolver;
import mod.azure.atomicdawn.story.StoryService;

public record NpcCondition(
    Type type,
    Optional<Identifier> id,
    Optional<String> value,
    int amount,
    boolean invert
) {

    public enum Type implements StringRepresentable {

        QUEST_COMPLETED,
        QUEST_ACTIVE,
        QUEST_STARTED,
        LEVEL,
        HAS_ITEM,
        GHOUL,
        SPECIAL,
        PERK,
        REPUTATION,
        AFFINITY,
        FLAG,
        QUEST_OUTCOME,
        COMPANION;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<NpcCondition> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Type.CODEC.fieldOf("type").forGetter(NpcCondition::type),
            Identifier.CODEC.optionalFieldOf("id").forGetter(NpcCondition::id),
            Codec.STRING.optionalFieldOf("value").forGetter(NpcCondition::value),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(NpcCondition::amount),
            Codec.BOOL.optionalFieldOf("invert", false).forGetter(NpcCondition::invert)
        ).apply(instance, NpcCondition::new)
    );

    public boolean test(ServerPlayer player) {
        return test(player, null);
    }

    public boolean test(ServerPlayer player, @Nullable NpcEntity npc) {
        return evaluate(player, npc) != invert;
    }

    private boolean evaluate(ServerPlayer player, @Nullable NpcEntity npc) {
        var log = QuestService.log(player);
        return switch (type) {
            case QUEST_COMPLETED -> id.map(log::isCompleted).orElse(false);
            case QUEST_ACTIVE -> id.flatMap(log::get).map(progress -> !progress.completed()).orElse(false);
            case QUEST_STARTED -> id.map(log.quests()::containsKey).orElse(false);
            case LEVEL -> SpecialService.get(player).level() >= amount;
            case HAS_ITEM -> id.flatMap(BuiltInRegistries.ITEM::getOptional)
                .map(item -> player.getInventory().countItem(item) >= Math.max(1, amount))
                .orElse(false);
            case GHOUL -> GhoulService.isGhoul(player);
            case SPECIAL -> value.flatMap(NpcCondition::attribute)
                .map(stat -> SpecialStatResolver.resolve(player).effective(stat) >= amount)
                .orElse(false);
            case PERK -> id.map(perk -> SpecialService.get(player).equippedRank(perk) >= Math.max(1, amount))
                .orElse(false);
            case REPUTATION -> id.map(faction -> StoryService.reputation(player, faction) >= amount).orElse(false);
            case AFFINITY -> id.or(() -> npc == null ? Optional.empty() : npc.config().identifier())
                .map(target -> StoryService.affinity(player, target) >= amount)
                .orElse(false);
            case FLAG -> value.map(raw -> {
                var split = raw.indexOf('=');
                var key = split < 0 ? raw : raw.substring(0, split);
                var stored = StoryService.flag(player, key);
                return split < 0 ? stored.isPresent() : stored.map(raw.substring(split + 1)::equals).orElse(false);
            }).orElse(false);
            case QUEST_OUTCOME -> id.map(
                quest -> StoryService.outcome(player, quest)
                    .map(outcome -> value.map(outcome::equals).orElse(true))
                    .orElse(false)
            ).orElse(false);
            case COMPANION -> npc != null && npc.isOwnedBy(player) && id.map(
                target -> npc.config().identifier().map(target::equals).orElse(false)
            ).orElse(true);
        };
    }

    private static Optional<SpecialAttribute> attribute(String name) {
        for (var attribute : SpecialAttribute.values()) {
            if (attribute.getSerializedName().equalsIgnoreCase(name)) {
                return Optional.of(attribute);
            }
        }
        return Optional.empty();
    }

    public static boolean all(List<NpcCondition> conditions, ServerPlayer player) {
        return all(conditions, player, null);
    }

    public static boolean all(List<NpcCondition> conditions, ServerPlayer player, @Nullable NpcEntity npc) {
        for (var condition : conditions) {
            if (!condition.test(player, npc)) {
                return false;
            }
        }
        return true;
    }
}

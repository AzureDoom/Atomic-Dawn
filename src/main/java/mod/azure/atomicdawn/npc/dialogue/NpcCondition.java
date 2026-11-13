package mod.azure.atomicdawn.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.quest.QuestService;
import mod.azure.atomicdawn.special.SpecialService;

public record NpcCondition(
    Type type,
    Optional<Identifier> id,
    int amount,
    boolean invert
) {

    public enum Type implements StringRepresentable {

        QUEST_COMPLETED,
        QUEST_ACTIVE,
        QUEST_STARTED,
        LEVEL,
        HAS_ITEM,
        GHOUL;

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
            Codec.INT.optionalFieldOf("amount", 1).forGetter(NpcCondition::amount),
            Codec.BOOL.optionalFieldOf("invert", false).forGetter(NpcCondition::invert)
        ).apply(instance, NpcCondition::new)
    );

    public boolean test(ServerPlayer player) {
        return evaluate(player) != invert;
    }

    private boolean evaluate(ServerPlayer player) {
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
        };
    }

    public static boolean all(List<NpcCondition> conditions, ServerPlayer player) {
        for (var condition : conditions) {
            if (!condition.test(player)) {
                return false;
            }
        }
        return true;
    }
}

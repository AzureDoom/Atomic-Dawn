package mod.azure.atomicdawn.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

import mod.azure.atomicdawn.story.StoryService;

public record QuestOutcome(
    String description,
    QuestReward rewards,
    boolean replaceRewards,
    Map<Identifier, Integer> reputation,
    Map<Identifier, Integer> affinity,
    Map<String, String> flags
) {

    public static final Codec<QuestOutcome> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.STRING.optionalFieldOf("description", "").forGetter(QuestOutcome::description),
            QuestReward.CODEC.optionalFieldOf("rewards", QuestReward.NONE).forGetter(QuestOutcome::rewards),
            Codec.BOOL.optionalFieldOf("replace_rewards", false).forGetter(QuestOutcome::replaceRewards),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                .optionalFieldOf("reputation", Map.of())
                .forGetter(QuestOutcome::reputation),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                .optionalFieldOf("affinity", Map.of())
                .forGetter(QuestOutcome::affinity),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("flags", Map.of())
                .forGetter(QuestOutcome::flags)
        ).apply(instance, QuestOutcome::new)
    );

    public void apply(ServerPlayer player) {
        reputation.forEach((faction, amount) -> StoryService.addReputation(player, faction, amount));
        affinity.forEach((npc, amount) -> StoryService.addAffinity(player, npc, amount, null));
        flags.forEach((key, value) -> StoryService.setFlag(player, key, value));
    }
}

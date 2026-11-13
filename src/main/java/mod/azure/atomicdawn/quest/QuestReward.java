package mod.azure.atomicdawn.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record QuestReward(
    int xp,
    int perkPicks,
    List<ItemReward> items,
    List<Identifier> holotapes,
    List<Identifier> startQuests
) {

    public record ItemReward(
        Identifier item,
        int count
    ) {

        public static final Codec<ItemReward> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Identifier.CODEC.fieldOf("item").forGetter(ItemReward::item),
                Codec.intRange(1, 6400).optionalFieldOf("count", 1).forGetter(ItemReward::count)
            ).apply(instance, ItemReward::new)
        );
    }

    public static final QuestReward NONE = new QuestReward(0, 0, List.of(), List.of(), List.of());

    public static final Codec<QuestReward> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.intRange(0, 1_000_000).optionalFieldOf("xp", 0).forGetter(QuestReward::xp),
            Codec.intRange(0, 100).optionalFieldOf("perk_picks", 0).forGetter(QuestReward::perkPicks),
            ItemReward.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(QuestReward::items),
            Identifier.CODEC.listOf().optionalFieldOf("holotapes", List.of()).forGetter(QuestReward::holotapes),
            Identifier.CODEC.listOf().optionalFieldOf("start_quests", List.of()).forGetter(QuestReward::startQuests)
        ).apply(instance, QuestReward::new)
    );
}

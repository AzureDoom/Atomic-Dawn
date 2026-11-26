package mod.azure.atomicdawn.npc.dialogue;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.story.SkillCheck;

public record NpcDialogue(
    List<Entry> start,
    Map<String, Node> nodes
) {

    public record Entry(
        String node,
        List<NpcCondition> conditions
    ) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.STRING.fieldOf("node").forGetter(Entry::node),
                NpcCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Entry::conditions)
            ).apply(instance, Entry::new)
        );
    }

    public record Option(
        String text,
        Optional<String> next,
        List<NpcCondition> conditions,
        List<NpcAction> actions,
        Optional<SkillCheck> check
    ) {

        public static final Codec<Option> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.STRING.fieldOf("text").forGetter(Option::text),
                Codec.STRING.optionalFieldOf("next").forGetter(Option::next),
                NpcCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Option::conditions),
                NpcAction.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(Option::actions),
                SkillCheck.CODEC.optionalFieldOf("check").forGetter(Option::check)
            ).apply(instance, Option::new)
        );
    }

    public record Node(
        String text,
        List<Option> options,
        List<NpcAction> actions
    ) {

        public static final Codec<Node> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.STRING.fieldOf("text").forGetter(Node::text),
                Option.CODEC.listOf().optionalFieldOf("options", List.of()).forGetter(Node::options),
                NpcAction.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(Node::actions)
            ).apply(instance, Node::new)
        );
    }

    private static final Codec<List<Entry>> START_CODEC = Codec.either(Codec.STRING, Entry.CODEC.listOf())
        .xmap(
            either -> either.map(node -> List.of(new Entry(node, List.of())), list -> list),
            list -> list.size() == 1 && list.getFirst().conditions().isEmpty()
                ? Either.left(list.getFirst().node())
                : Either.right(list)
        );

    public static final Codec<NpcDialogue> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            START_CODEC.fieldOf("start").forGetter(NpcDialogue::start),
            Codec.unboundedMap(Codec.STRING, Node.CODEC).fieldOf("nodes").forGetter(NpcDialogue::nodes)
        ).apply(instance, NpcDialogue::new)
    );

    public Optional<String> startNode(ServerPlayer player) {
        for (var entry : start) {
            if (nodes.containsKey(entry.node()) && NpcCondition.all(entry.conditions(), player)) {
                return Optional.of(entry.node());
            }
        }
        return Optional.empty();
    }

    public static Optional<NpcDialogue> get(RegistryAccess access, Identifier id) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.NPC_DIALOGUE)
            .flatMap(registry -> registry.get(ResourceKey.create(ADRegistries.NPC_DIALOGUE, id)))
            .map(Holder::value);
    }

    public static List<Identifier> ids(RegistryAccess access) {
        return ADRegistryUtil.optionalRegistry(access, ADRegistries.NPC_DIALOGUE)
            .map(
                registry -> registry.listElementIds()
                    .map(ResourceKey::identifier)
                    .sorted(Comparator.comparing(Identifier::toString))
                    .toList()
            )
            .orElse(List.of());
    }
}

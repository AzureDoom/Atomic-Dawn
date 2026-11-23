package mod.azure.atomicdawn.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record CompanionProfile(
    Identifier npc,
    String name,
    List<Reaction> reactions,
    List<FactionReaction> factions,
    int leaveBelow
) {

    public record Reaction(
        Identifier action,
        int amount
    ) {

        public static final Codec<Reaction> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Identifier.CODEC.fieldOf("action").forGetter(Reaction::action),
                Codec.INT.fieldOf("amount").forGetter(Reaction::amount)
            ).apply(instance, Reaction::new)
        );
    }

    public record FactionReaction(
        Identifier faction,
        float perPoint
    ) {

        public static final Codec<FactionReaction> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Identifier.CODEC.fieldOf("faction").forGetter(FactionReaction::faction),
                Codec.FLOAT.fieldOf("per_point").forGetter(FactionReaction::perPoint)
            ).apply(instance, FactionReaction::new)
        );
    }

    public static final Codec<CompanionProfile> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Identifier.CODEC.fieldOf("npc").forGetter(CompanionProfile::npc),
            Codec.STRING.fieldOf("name").forGetter(CompanionProfile::name),
            Reaction.CODEC.listOf().optionalFieldOf("reactions", List.of()).forGetter(CompanionProfile::reactions),
            FactionReaction.CODEC.listOf()
                .optionalFieldOf("factions", List.of())
                .forGetter(CompanionProfile::factions),
            Codec.INT.optionalFieldOf("leave_below", -750).forGetter(CompanionProfile::leaveBelow)
        ).apply(instance, CompanionProfile::new)
    );

    public static final int MIN = -1000;

    public static final int MAX = 1000;

    public static String tier(int affinity) {
        if (affinity <= -750) {
            return "companion.atomicdawn.tier.hatred";
        }
        if (affinity <= -250) {
            return "companion.atomicdawn.tier.disliked";
        }
        if (affinity < 250) {
            return "companion.atomicdawn.tier.neutral";
        }
        if (affinity < 500) {
            return "companion.atomicdawn.tier.liked";
        }
        if (affinity < 750) {
            return "companion.atomicdawn.tier.admiration";
        }
        return "companion.atomicdawn.tier.idolized";
    }
}

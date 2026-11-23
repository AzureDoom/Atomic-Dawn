package mod.azure.atomicdawn.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public record Faction(
    String name,
    String description,
    int color,
    int min,
    int max,
    int start,
    List<Rank> ranks,
    List<Identifier> npcs,
    HolderSet<EntityType<?>> entities,
    List<Identifier> allies,
    List<Identifier> enemies,
    float spillover,
    int killPenalty,
    boolean hidden
) {

    public record Rank(
        int min,
        String name,
        boolean hostile
    ) {

        public static final Codec<Rank> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.INT.fieldOf("min").forGetter(Rank::min),
                Codec.STRING.fieldOf("name").forGetter(Rank::name),
                Codec.BOOL.optionalFieldOf("hostile", false).forGetter(Rank::hostile)
            ).apply(instance, Rank::new)
        );
    }

    public static final List<Rank> DEFAULT_RANKS = List.of(
        new Rank(Integer.MIN_VALUE, "faction.atomicdawn.rank.vilified", true),
        new Rank(-500, "faction.atomicdawn.rank.hated", true),
        new Rank(-150, "faction.atomicdawn.rank.disliked", false),
        new Rank(-50, "faction.atomicdawn.rank.neutral", false),
        new Rank(150, "faction.atomicdawn.rank.liked", false),
        new Rank(400, "faction.atomicdawn.rank.accepted", false),
        new Rank(750, "faction.atomicdawn.rank.idolized", false)
    );

    public static final Codec<Integer> COLOR_CODEC = Codec.STRING.comapFlatMap(
        value -> {
            try {
                return DataResult.success(Integer.parseUnsignedInt(value.replace("#", ""), 16) & 0xFFFFFF);
            } catch (NumberFormatException exception) {
                return DataResult.error(() -> "Invalid faction colour: " + value);
            }
        },
        value -> String.format(Locale.ROOT, "#%06X", value & 0xFFFFFF)
    );

    @SuppressWarnings("NullableProblems")
    public static final Codec<Faction> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Faction::name),
            Codec.STRING.optionalFieldOf("description", "").forGetter(Faction::description),
            COLOR_CODEC.optionalFieldOf("color", 0x8BE04A).forGetter(Faction::color),
            Codec.INT.optionalFieldOf("min", -1000).forGetter(Faction::min),
            Codec.INT.optionalFieldOf("max", 1000).forGetter(Faction::max),
            Codec.INT.optionalFieldOf("start", 0).forGetter(Faction::start),
            Rank.CODEC.listOf().optionalFieldOf("ranks", List.of()).forGetter(Faction::ranks),
            Identifier.CODEC.listOf().optionalFieldOf("npcs", List.of()).forGetter(Faction::npcs),
            RegistryCodecs.holderSet(Registries.ENTITY_TYPE)
                .optionalFieldOf("entities", HolderSet.empty())
                .forGetter(Faction::entities),
            Identifier.CODEC.listOf().optionalFieldOf("allies", List.of()).forGetter(Faction::allies),
            Identifier.CODEC.listOf().optionalFieldOf("enemies", List.of()).forGetter(Faction::enemies),
            Codec.floatRange(0F, 1F).optionalFieldOf("spillover", 0.5F).forGetter(Faction::spillover),
            Codec.INT.optionalFieldOf("kill_penalty", -25).forGetter(Faction::killPenalty),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Faction::hidden)
        ).apply(instance, Faction::new)
    );

    public int clamp(int value) {
        return Math.clamp(value, min, max);
    }

    public Rank rank(int value) {
        var list = ranks.isEmpty() ? DEFAULT_RANKS : ranks;
        var sorted = list.stream().sorted(Comparator.comparingInt(Rank::min)).toList();
        var current = sorted.getFirst();
        for (var rank : sorted) {
            if (value >= rank.min()) {
                current = rank;
            }
        }
        return current;
    }
}

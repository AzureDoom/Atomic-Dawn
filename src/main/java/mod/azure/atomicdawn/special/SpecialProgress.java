package mod.azure.atomicdawn.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SpecialProgress(
    int version,
    boolean initialized,
    int level,
    long xp,
    int earnedPoints,
    int perkPicks,
    Map<Identifier, Integer> owned,
    List<SpecialLoadout> loadouts,
    int active,
    int revision,
    LegendaryProgress legendary
) {

    public static final int LEGACY_VERSION = 1;

    public static final int CURRENT_VERSION = 2;

    public static final int MAX_LOADOUTS = 8;

    public static final int MAX_OWNED = 512;

    public static final SpecialProgress EMPTY = new SpecialProgress(
        CURRENT_VERSION,
        false,
        1,
        0L,
        0,
        0,
        Map.of(),
        List.of(SpecialLoadout.fresh(1)),
        0,
        0,
        LegendaryProgress.EMPTY
    );

    public static final MapCodec<SpecialProgress> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.INT.optionalFieldOf("version", LEGACY_VERSION).forGetter(SpecialProgress::version),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(SpecialProgress::initialized),
            Codec.intRange(1, SpecialXp.ABSOLUTE_MAX_LEVEL)
                .optionalFieldOf("level", 1)
                .forGetter(SpecialProgress::level),
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(SpecialProgress::xp),
            Codec.INT.optionalFieldOf("earned_points", 0).forGetter(SpecialProgress::earnedPoints),
            Codec.INT.optionalFieldOf("perk_picks", 0).forGetter(SpecialProgress::perkPicks),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT)
                .optionalFieldOf("owned", Map.of())
                .forGetter(SpecialProgress::owned),
            SpecialLoadout.CODEC.listOf()
                .optionalFieldOf("loadouts", List.of())
                .forGetter(SpecialProgress::loadouts),
            Codec.INT.optionalFieldOf("active", 0).forGetter(SpecialProgress::active),
            Codec.INT.optionalFieldOf("revision", 0).forGetter(SpecialProgress::revision),
            LegendaryProgress.CODEC.optionalFieldOf("legendary", LegendaryProgress.EMPTY)
                .forGetter(SpecialProgress::legendary)
        ).apply(instance, SpecialProgress::new)
    );

    private static final StreamCodec<ByteBuf, Map<Identifier, Integer>> OWNED_STREAM = ByteBufCodecs.map(
        HashMap::new,
        Identifier.STREAM_CODEC,
        ByteBufCodecs.VAR_INT,
        MAX_OWNED
    );

    private static final StreamCodec<ByteBuf, List<SpecialLoadout>> LOADOUTS_STREAM = SpecialLoadout.STREAM_CODEC
        .apply(ByteBufCodecs.list(MAX_LOADOUTS));

    public static final StreamCodec<ByteBuf, SpecialProgress> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> {
            ByteBufCodecs.VAR_INT.encode(buf, value.version());
            ByteBufCodecs.BOOL.encode(buf, value.initialized());
            ByteBufCodecs.VAR_INT.encode(buf, value.level());
            ByteBufCodecs.VAR_LONG.encode(buf, value.xp());
            ByteBufCodecs.VAR_INT.encode(buf, value.earnedPoints());
            ByteBufCodecs.VAR_INT.encode(buf, value.perkPicks());
            OWNED_STREAM.encode(buf, value.owned());
            LOADOUTS_STREAM.encode(buf, value.loadouts());
            ByteBufCodecs.VAR_INT.encode(buf, value.active());
            ByteBufCodecs.VAR_INT.encode(buf, value.revision());
            LegendaryProgress.STREAM_CODEC.encode(buf, value.legendary());
        },
        buf -> new SpecialProgress(
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_LONG.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            OWNED_STREAM.decode(buf),
            LOADOUTS_STREAM.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            LegendaryProgress.STREAM_CODEC.decode(buf)
        )
    );

    public SpecialProgress {
        level = Math.clamp(level, 1, SpecialXp.ABSOLUTE_MAX_LEVEL);
        xp = Math.max(0L, xp);
        earnedPoints = Math.max(0, earnedPoints);
        perkPicks = Math.max(0, perkPicks);
        var cleanOwned = new HashMap<Identifier, Integer>();
        owned.forEach((id, rank) -> {
            if (rank != null && rank > 0) {
                cleanOwned.put(id, rank);
            }
        });
        owned = Map.copyOf(cleanOwned);
        loadouts = loadouts.isEmpty() ? List.of(SpecialLoadout.fresh(1)) : List.copyOf(loadouts);
        active = Math.clamp(active, 0, loadouts.size() - 1);
        legendary = legendary == null ? LegendaryProgress.EMPTY : legendary;
    }

    public SpecialLoadout activeLoadout() {
        return loadouts.get(active);
    }

    public int ownedRank(Identifier perk) {
        return owned.getOrDefault(perk, 0);
    }

    public int equippedRank(Identifier perk) {
        return activeLoadout().equipped().getOrDefault(perk, 0);
    }

    public int base(SpecialAttribute attribute) {
        return activeLoadout().base(attribute);
    }

    public int unspentPoints(int startingPerAttribute) {
        return Math.max(0, startingPerAttribute * SpecialAttribute.COUNT + earnedPoints - activeLoadout().allocated());
    }

    private SpecialProgress next(
        boolean initialized,
        int level,
        long xp,
        int earnedPoints,
        int perkPicks,
        Map<Identifier, Integer> owned,
        List<SpecialLoadout> loadouts,
        int active
    ) {
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, loadouts, active, legendary);
    }

    private SpecialProgress next(
        boolean initialized,
        int level,
        long xp,
        int earnedPoints,
        int perkPicks,
        Map<Identifier, Integer> owned,
        List<SpecialLoadout> loadouts,
        int active,
        LegendaryProgress legendary
    ) {
        return new SpecialProgress(
            version,
            initialized,
            level,
            xp,
            earnedPoints,
            perkPicks,
            owned,
            loadouts,
            active,
            revision + 1,
            legendary
        );
    }

    public SpecialProgress withVersion(int value) {
        return new SpecialProgress(
            value,
            initialized,
            level,
            xp,
            earnedPoints,
            perkPicks,
            owned,
            loadouts,
            active,
            revision + 1,
            legendary
        );
    }

    public SpecialProgress withLegendary(LegendaryProgress value) {
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, loadouts, active, value);
    }

    public SpecialProgress withOwnedAndLegendary(Map<Identifier, Integer> owned, LegendaryProgress value) {
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, loadouts, active, value);
    }

    public SpecialProgress withInitialized(Map<Identifier, Integer> owned, List<SpecialLoadout> loadouts, int picks) {
        return next(true, level, xp, earnedPoints, picks, owned, loadouts, 0);
    }

    public SpecialProgress withXp(long xp, int level, int earnedPoints, int perkPicks) {
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, loadouts, active);
    }

    public SpecialProgress withPerkPicks(int perkPicks) {
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, loadouts, active);
    }

    public SpecialProgress withOwned(Map<Identifier, Integer> owned, int perkPicks) {
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, loadouts, active);
    }

    public SpecialProgress withActiveLoadout(SpecialLoadout loadout) {
        return withLoadout(active, loadout);
    }

    public SpecialProgress withLoadout(int index, SpecialLoadout loadout) {
        var copy = new ArrayList<>(loadouts);
        copy.set(index, loadout);
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, copy, active);
    }

    public SpecialProgress withLoadouts(List<SpecialLoadout> loadouts, int active) {
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, loadouts, active);
    }

    public SpecialProgress withActive(int index) {
        return next(initialized, level, xp, earnedPoints, perkPicks, owned, loadouts, index);
    }
}

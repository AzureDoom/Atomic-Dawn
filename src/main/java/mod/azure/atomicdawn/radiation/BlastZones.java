package mod.azure.atomicdawn.radiation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public record BlastZones(List<BlastZone> zones) {

    public static final int MAX_ZONES = 64;

    public static final BlastZones EMPTY = new BlastZones(List.of());

    public static final MapCodec<BlastZones> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            BlastZone.CODEC.listOf().optionalFieldOf("zones", List.of()).forGetter(BlastZones::zones)
        ).apply(instance, BlastZones::new)
    );

    public BlastZones {
        zones = List.copyOf(zones);
    }

    public BlastZones with(BlastZone zone) {
        var list = new ArrayList<>(zones);
        list.add(zone);
        while (list.size() > MAX_ZONES) {
            list.removeFirst();
        }
        return new BlastZones(list);
    }

    public BlastZones pruned(long now) {
        if (zones.stream().noneMatch(zone -> zone.expired(now))) {
            return this;
        }
        return new BlastZones(zones.stream().filter(zone -> !zone.expired(now)).toList());
    }
}

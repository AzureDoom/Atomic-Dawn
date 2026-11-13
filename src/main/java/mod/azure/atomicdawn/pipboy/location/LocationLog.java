package mod.azure.atomicdawn.pipboy.location;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record LocationLog(
    List<DiscoveredLocation> discovered,
    List<Waypoint> waypoints
) {

    public static final int MAX_DISCOVERED = 1024;

    public static final int MAX_WAYPOINTS = 256;

    public static final LocationLog EMPTY = new LocationLog(List.of(), List.of());

    public static final MapCodec<LocationLog> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            DiscoveredLocation.CODEC.listOf()
                .optionalFieldOf("discovered", List.of())
                .forGetter(LocationLog::discovered),
            Waypoint.CODEC.listOf().optionalFieldOf("waypoints", List.of()).forGetter(LocationLog::waypoints)
        ).apply(instance, LocationLog::new)
    );

    public static final StreamCodec<ByteBuf, LocationLog> STREAM_CODEC = StreamCodec.composite(
        DiscoveredLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_DISCOVERED)),
        LocationLog::discovered,
        Waypoint.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_WAYPOINTS)),
        LocationLog::waypoints,
        LocationLog::new
    );

    public LocationLog {
        discovered = List.copyOf(
            discovered.size() > MAX_DISCOVERED ? discovered.subList(0, MAX_DISCOVERED) : discovered
        );
        waypoints = List.copyOf(waypoints.size() > MAX_WAYPOINTS ? waypoints.subList(0, MAX_WAYPOINTS) : waypoints);
    }

    public LocationLog withDiscovered(DiscoveredLocation location) {
        var copy = new ArrayList<>(discovered);
        copy.add(location);
        return new LocationLog(copy, waypoints);
    }

    public LocationLog withWaypoints(List<Waypoint> value) {
        return new LocationLog(discovered, value);
    }
}

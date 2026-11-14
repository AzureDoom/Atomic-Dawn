package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public record CampAccess(
    Map<UUID, String> trusted,
    boolean trustedBuild,
    boolean trustedStorage,
    boolean partyBuild,
    boolean publicStations,
    boolean mapIcon
) {

    public static final int MAX_TRUSTED = 16;

    public static final CampAccess DEFAULT = new CampAccess(Map.of(), true, false, false, true, true);

    public static final Codec<CampAccess> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING)
                .optionalFieldOf("trusted", Map.of())
                .forGetter(CampAccess::trusted),
            Codec.BOOL.optionalFieldOf("trusted_build", true).forGetter(CampAccess::trustedBuild),
            Codec.BOOL.optionalFieldOf("trusted_storage", false).forGetter(CampAccess::trustedStorage),
            Codec.BOOL.optionalFieldOf("party_build", false).forGetter(CampAccess::partyBuild),
            Codec.BOOL.optionalFieldOf("public_stations", true).forGetter(CampAccess::publicStations),
            Codec.BOOL.optionalFieldOf("map_icon", true).forGetter(CampAccess::mapIcon)
        ).apply(instance, CampAccess::new)
    );

    public enum Flag {

        TRUSTED_BUILD,
        TRUSTED_STORAGE,
        PARTY_BUILD,
        PUBLIC_STATIONS,
        MAP_ICON;

        private static final Flag[] VALUES = values();

        public static Flag byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }

        public String translationKey() {
            return "camp.atomicdawn.access." + name().toLowerCase(Locale.ROOT);
        }
    }

    public CampAccess {
        trusted = Map.copyOf(trusted);
    }

    public boolean get(Flag flag) {
        return switch (flag) {
            case TRUSTED_BUILD -> trustedBuild;
            case TRUSTED_STORAGE -> trustedStorage;
            case PARTY_BUILD -> partyBuild;
            case PUBLIC_STATIONS -> publicStations;
            case MAP_ICON -> mapIcon;
        };
    }

    public CampAccess toggled(Flag flag) {
        return new CampAccess(
            trusted,
            flag == Flag.TRUSTED_BUILD ? !trustedBuild : trustedBuild,
            flag == Flag.TRUSTED_STORAGE ? !trustedStorage : trustedStorage,
            flag == Flag.PARTY_BUILD ? !partyBuild : partyBuild,
            flag == Flag.PUBLIC_STATIONS ? !publicStations : publicStations,
            flag == Flag.MAP_ICON ? !mapIcon : mapIcon
        );
    }

    public CampAccess withTrusted(UUID id, String name) {
        var copy = new HashMap<>(trusted);
        copy.put(id, name);
        return new CampAccess(copy, trustedBuild, trustedStorage, partyBuild, publicStations, mapIcon);
    }

    public CampAccess withoutTrusted(UUID id) {
        var copy = new HashMap<>(trusted);
        copy.remove(id);
        return new CampAccess(copy, trustedBuild, trustedStorage, partyBuild, publicStations, mapIcon);
    }
}

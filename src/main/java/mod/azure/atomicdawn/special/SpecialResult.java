package mod.azure.atomicdawn.special;

import java.util.Locale;

public enum SpecialResult {

    OK(true),
    NO_CHANGE(false),
    STALE(false),
    RATE_LIMITED(false),
    DISABLED(false),
    INVALID(false),
    NO_POINTS(false),
    ATTRIBUTE_CAP(false),
    NO_PERK_PICKS(false),
    UNKNOWN_PERK(false),
    MAX_RANK(false),
    LEVEL_TOO_LOW(false),
    NOT_OWNED(false),
    NO_CAPACITY(false),
    EXCLUSIVE(false),
    IN_COMBAT(false),
    NEED_STATION(false),
    CARDS_UNEQUIPPED(true),
    WRONG_FORM(false),
    PROTECTED(false),
    NO_COINS(false),
    NO_SLOT(false);

    private static final SpecialResult[] VALUES = values();

    private final boolean success;

    SpecialResult(boolean success) {
        this.success = success;
    }

    public static SpecialResult byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    public boolean success() {
        return success;
    }

    public String translationKey() {
        return "special.atomicdawn.result." + name().toLowerCase(Locale.ROOT);
    }
}

package mod.azure.atomicdawn.ghoul;

import java.util.Locale;

public enum TransformationResult {

    SUCCESS(true),
    DISABLED(false),
    ALREADY_GHOUL(false),
    ALREADY_HUMAN(false),
    ON_COOLDOWN(false),
    LEVEL_TOO_LOW(false),
    DIMENSION_BLOCKED(false),
    MISSING_REQUIREMENT(false),
    EXPIRED(false);

    private final boolean success;

    TransformationResult(boolean success) {
        this.success = success;
    }

    public boolean success() {
        return success;
    }

    public String translationKey() {
        return "ghoul.atomicdawn.result." + name().toLowerCase(Locale.ROOT);
    }
}

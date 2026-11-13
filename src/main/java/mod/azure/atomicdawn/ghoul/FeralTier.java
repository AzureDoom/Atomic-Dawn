package mod.azure.atomicdawn.ghoul;

import java.util.Locale;

public enum FeralTier {

    STABLE(80F, 0F, 0F, 0F, 0F, 0F),
    CONTROLLED(60F, 0F, 0F, 0F, 0F, 0F),
    UNSTEADY(40F, 0.2F, 0F, 0F, 0F, 0F),
    AGITATED(20F, 0.35F, 0.4F, 0.5F, 0F, 0F),
    NEAR_FERAL(0F, 0.65F, 0.65F, 0.75F, 0.5F, 0.5F),
    FERAL(Float.NEGATIVE_INFINITY, 1F, 1F, 1F, 1F, 1F);

    private final float minimumPercent;

    public final float rangedWeight;

    public final float vatsWeight;

    public final float apWeight;

    public final float defenseWeight;

    public final float meleeWeight;

    FeralTier(
        float minimumPercent,
        float rangedWeight,
        float vatsWeight,
        float apWeight,
        float defenseWeight,
        float meleeWeight
    ) {
        this.minimumPercent = minimumPercent;
        this.rangedWeight = rangedWeight;
        this.vatsWeight = vatsWeight;
        this.apWeight = apWeight;
        this.defenseWeight = defenseWeight;
        this.meleeWeight = meleeWeight;
    }

    public static FeralTier of(float stability, float maxStability) {
        if (stability <= 0F) {
            return FERAL;
        }
        var percent = stability / Math.max(1F, maxStability) * 100F;
        for (var tier : values()) {
            if (tier != FERAL && percent >= tier.minimumPercent) {
                return tier;
            }
        }
        return FERAL;
    }

    public boolean penalized() {
        return rangedWeight > 0F || vatsWeight > 0F || apWeight > 0F || defenseWeight > 0F || meleeWeight > 0F;
    }

    public String translationKey() {
        return "ghoul.atomicdawn.tier." + name().toLowerCase(Locale.ROOT);
    }
}

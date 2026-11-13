package mod.azure.atomicdawn.special;

public final class SpecialXp {

    public static final int ABSOLUTE_MAX_LEVEL = 10_000;

    private SpecialXp() {}

    public static long requiredXp(int level) {
        var l = Math.max(1, level) - 1L;
        return Math.round(100.0 + 25.0 * l + 5.0 * l * l);
    }

    public static long totalForLevel(int level) {
        var total = 0L;
        for (var l = 1; l < Math.min(level, ABSOLUTE_MAX_LEVEL); l++) {
            total += requiredXp(l);
        }
        return total;
    }

    public static int levelFor(long xp, int maxLevel) {
        var cap = maxLevel <= 0 ? ABSOLUTE_MAX_LEVEL : Math.min(maxLevel, ABSOLUTE_MAX_LEVEL);
        var level = 1;
        var needed = 0L;
        while (level < cap) {
            needed += requiredXp(level);
            if (xp < needed) {
                break;
            }
            level++;
        }
        return level;
    }

    public static float fraction(long xp, int level) {
        var start = totalForLevel(level);
        var span = requiredXp(level);
        return span <= 0 ? 1F : Math.clamp((xp - start) / (float) span, 0F, 1F);
    }
}

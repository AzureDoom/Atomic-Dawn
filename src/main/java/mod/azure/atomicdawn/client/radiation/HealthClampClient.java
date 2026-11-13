package mod.azure.atomicdawn.client.radiation;

import net.minecraft.client.Minecraft;

public final class HealthClampClient {

    private static final float TOLERANCE = 0.01F;

    private static final long EXPIRY_TICKS = 40L;

    private static float expected = Float.NaN;

    private static long expiresAt;

    private HealthClampClient() {}

    public static void expect(float health) {
        var level = Minecraft.getInstance().level;
        expected = health;
        expiresAt = level == null ? 0L : level.getGameTime() + EXPIRY_TICKS;
    }

    public static boolean consume(float health) {
        if (Float.isNaN(expected)) {
            return false;
        }
        var level = Minecraft.getInstance().level;
        if (level == null || level.getGameTime() > expiresAt) {
            expected = Float.NaN;
            return false;
        }
        if (Math.abs(health - expected) > TOLERANCE) {
            return false;
        }
        expected = Float.NaN;
        return true;
    }
}

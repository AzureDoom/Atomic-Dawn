package mod.azure.atomicdawn.client;

import net.minecraft.client.Minecraft;

import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

@SuppressWarnings("unused")
public final class ClientWeaponState {

    private static double nextShotTime = Double.NEGATIVE_INFINITY;

    private static double lastShotTime = Double.NEGATIVE_INFINITY;

    private static float bloom;

    private static double bufferedShotUntil = Double.NEGATIVE_INFINITY;

    private static double drawReadyTime = Double.NEGATIVE_INFINITY;

    private static double reloadStartTime = -1;

    private static int reloadDurationTicks;

    private static boolean triggerDown;

    private static double triggerPressTime;

    private static int burstKicksRemaining;

    private ClientWeaponState() {}

    public static double now() {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            return 0;
        }
        return level.getGameTime() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public static boolean canShootAt(double now, boolean interruptReload) {
        return (!isReloading() || interruptReload) && now >= nextShotTime && now >= drawReadyTime;
    }

    public static boolean canShootAt(double now) {
        return canShootAt(now, false);
    }

    public static void bufferShot(double now) {
        bufferedShotUntil = now + 4;
    }

    public static boolean consumeBufferedShot(double now) {
        if (now <= bufferedShotUntil) {
            bufferedShotUntil = Double.NEGATIVE_INFINITY;
            return true;
        }
        return false;
    }

    public static void onShotRequested(double now, WeaponDefinition definition, boolean predictedHit) {
        nextShotTime = Math.max(nextShotTime, now) + definition.firing().intervalTicks();
        if (isReloading()) {
            onReloadStatus(0);
        }
        if (predictedHit) {
            lastShotTime = now;
            var accuracy = definition.accuracy();
            bloom = Math.min(accuracy.maxBloom(), bloom + accuracy.bloomPerShot());
        }
    }

    public static void startBurst(WeaponDefinition definition) {
        burstKicksRemaining = definition.firing().burst().count() - 1;
    }

    public static boolean pollBurstKick(double now, WeaponDefinition definition) {
        if (burstKicksRemaining <= 0 || now < nextShotTime) {
            return false;
        }
        burstKicksRemaining--;
        onShotRequested(now, definition, true);
        if (burstKicksRemaining == 0) {
            nextShotTime += definition.firing().burst().cooldownTicks();
        }
        return true;
    }

    public static void onDraw(double now, WeaponDefinition definition) {
        drawReadyTime = now + definition.drawTicks();
        bufferedShotUntil = Double.NEGATIVE_INFINITY;
        reloadDurationTicks = 0;
        reloadStartTime = -1;
        triggerDown = false;
        burstKicksRemaining = 0;
    }

    public static void tickBloom(double now, WeaponDefinition.Accuracy accuracy) {
        if (bloom > 0F && now - lastShotTime > 2) {
            bloom = Math.max(0F, bloom - accuracy.bloomRecoveryPerTick());
        }
    }

    public static float bloom() {
        return bloom;
    }

    public static boolean isTriggerDown() {
        return triggerDown;
    }

    public static void pressTrigger(double now) {
        triggerDown = true;
        triggerPressTime = now;
    }

    public static double releaseTrigger(double now) {
        triggerDown = false;
        return now - triggerPressTime;
    }

    public static double triggerHeldTicks(double now) {
        return triggerDown ? now - triggerPressTime : 0;
    }

    public static boolean isReloading() {
        return reloadDurationTicks > 0;
    }

    public static float reloadProgress() {
        if (!isReloading()) {
            return -1F;
        }
        var elapsed = now() - reloadStartTime;
        return (float) Math.clamp(elapsed / reloadDurationTicks, 0.0, 1.0);
    }

    public static void onReloadStatus(int durationTicks) {
        reloadDurationTicks = Math.max(0, durationTicks);
        reloadStartTime = durationTicks > 0 ? now() : -1;
        if (durationTicks == 0) {
            nextShotTime = Math.min(nextShotTime, now());
        }
    }

    public static void reset() {
        nextShotTime = Double.NEGATIVE_INFINITY;
        lastShotTime = Double.NEGATIVE_INFINITY;
        bufferedShotUntil = Double.NEGATIVE_INFINITY;
        drawReadyTime = Double.NEGATIVE_INFINITY;
        bloom = 0F;
        reloadStartTime = -1;
        reloadDurationTicks = 0;
        triggerDown = false;
        burstKicksRemaining = 0;
    }
}

package mod.azure.atomicdawn.weapon.action;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import mod.azure.atomicdawn.vats.BodyPart;

@SuppressWarnings("unused")
public final class ShooterState {

    double nextShotTick = Double.NEGATIVE_INFINITY;

    long lastShotTick = Long.MIN_VALUE / 2;

    long nextDryFireTick;

    long nextBrokenMessageTick;

    float bloom;

    boolean triggerHeld;

    long triggerPressTick;

    int burstRemaining;

    int lastSelectedSlot = -1;

    @Nullable
    Item lastHeldItem;

    long drawReadyTick;

    boolean aiming;

    ReloadPhase reloadPhase = ReloadPhase.IDLE;

    long reloadNextTick = -1;

    int reloadSlot = -1;

    @Nullable
    Item reloadItem;

    boolean vatsActive;

    int vatsTargetId = -1;

    BodyPart vatsPart = BodyPart.TORSO;

    boolean criticalArmed;

    long lastApSpendTick = Long.MIN_VALUE / 2;

    @Nullable
    private UUID furiousTarget;

    private int furiousStreak;

    enum ReloadPhase {
        IDLE,
        MAGAZINE,
        START,
        INSERTING,
        END,
        FINISH
    }

    public boolean isReloading() {
        return reloadPhase != ReloadPhase.IDLE;
    }

    void startReload(ReloadPhase phase, long nextTick, int slot, Item item) {
        this.reloadPhase = phase;
        this.reloadNextTick = nextTick;
        this.reloadSlot = slot;
        this.reloadItem = item;
    }

    void clearReload() {
        this.reloadPhase = ReloadPhase.IDLE;
        this.reloadNextTick = -1;
        this.reloadSlot = -1;
        this.reloadItem = null;
    }

    public float bloom() {
        return bloom;
    }

    public boolean vatsActive() {
        return vatsActive;
    }

    public int furiousStreak(UUID target) {
        return target.equals(furiousTarget) ? furiousStreak : 0;
    }

    public void recordFuriousHit(UUID target) {
        if (target.equals(furiousTarget)) {
            furiousStreak++;
        } else {
            furiousTarget = target;
            furiousStreak = 1;
        }
    }
}

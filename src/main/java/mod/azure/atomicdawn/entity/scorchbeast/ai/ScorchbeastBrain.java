package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.azurecortex.runtime.CortexRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastFlight;

public final class ScorchbeastBrain {

    public static final String GLOBAL_COOLDOWN = "atomicdawn:scorchbeast/global";

    public static final String SONIC_COOLDOWN = "atomicdawn:scorchbeast/sonic";

    public static final String DUST_COOLDOWN = "atomicdawn:scorchbeast/dust";

    public static final String SWEEP_COOLDOWN = "atomicdawn:scorchbeast/sweep";

    public static final String LUNGE_COOLDOWN = "atomicdawn:scorchbeast/lunge";

    public static final String SHOCKWAVE_COOLDOWN = "atomicdawn:scorchbeast/shockwave";

    public static final String TAKEOFF_COOLDOWN = "atomicdawn:scorchbeast/takeoff";

    public static final String LAND_COOLDOWN = "atomicdawn:scorchbeast/land";

    private static final int SENSOR_INTERVAL = 10;

    private static final int LOSE_SIGHT_TICKS = 240;

    private final ScorchbeastEntity beast;

    private final CortexRuntime<ScorchbeastEntity, Void> runtime;

    private long lastSeenTick;

    public ScorchbeastBrain(ScorchbeastEntity beast) {
        this.beast = beast;
        this.runtime = new CortexRuntime<>(beast, null, ScorchbeastTree.create());
    }

    public Blackboard blackboard() {
        return runtime.getBlackboard();
    }

    public CooldownTracker cooldowns() {
        return runtime.getCooldowns();
    }

    public void tick() {
        sense(beast.level().getGameTime());
        runtime.tick();
    }

    @Nullable
    public LivingEntity target() {
        LivingEntity target = blackboard().get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    public void onFlightChanged(ScorchbeastFlight next) {
        var config = AtomicDawn.CONFIG.scorchbeast;
        var random = beast.getRandom();
        if (next == ScorchbeastFlight.FLYING) {
            cooldowns().set(LAND_COOLDOWN, config.MIN_AIR_SECONDS * 20 + random.nextInt(200));
        } else if (next == ScorchbeastFlight.GROUNDED) {
            cooldowns().set(TAKEOFF_COOLDOWN, config.MIN_GROUND_SECONDS * 20 + random.nextInt(160));
            blackboard().remove(ScorchbeastBlackboardKeys.GROUND_STUCK);
        }
    }

    private void sense(long now) {
        var blackboard = blackboard();
        var config = AtomicDawn.CONFIG.scorchbeast;
        var target = target();
        if (target == null && blackboard.has(CommonBlackboardKeys.TARGET)) {
            clearTarget();
        } else if (target != null && (!ScorchbeastEntity.validTarget(target) || !withinLeash(target.position()))) {
            clearTarget();
            target = null;
        }
        if (target != null) {
            var range = config.DETECTION_RANGE * 1.5;
            var visible = beast.getSensing().hasLineOfSight(target) && beast.distanceToSqr(target) <= range * range;
            blackboard.set(ScorchbeastBlackboardKeys.TARGET_VISIBLE, visible);
            if (visible) {
                lastSeenTick = now;
                blackboard.set(CommonBlackboardKeys.LAST_SEEN_POS, target.blockPosition());
            }
            blackboard.set(CommonBlackboardKeys.LAST_KNOWN_TARGET_POS, target.blockPosition());
            if (!visible && now - lastSeenTick > LOSE_SIGHT_TICKS) {
                clearTarget();
                target = null;
            }
        }
        if ((now + beast.getId()) % SENSOR_INTERVAL == 0 && (target == null || !visible(blackboard))) {
            var found = scan();
            if (found != null && found != target) {
                acquire(found, now);
            }
        }
    }

    @Nullable
    private LivingEntity scan() {
        var range = AtomicDawn.CONFIG.scorchbeast.DETECTION_RANGE;
        return beast.level()
            .getEntitiesOfClass(
                LivingEntity.class,
                beast.getBoundingBox().inflate(range),
                candidate -> candidate != beast && ScorchbeastEntity.validTarget(candidate) && withinLeash(
                    candidate.position()
                ) && beast.distanceToSqr(candidate) <= range * range && beast.getSensing().hasLineOfSight(candidate)
            )
            .stream()
            .min(Comparator.comparingDouble(beast::distanceToSqr))
            .orElse(null);
    }

    private static boolean visible(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(ScorchbeastBlackboardKeys.TARGET_VISIBLE));
    }

    private void acquire(LivingEntity target, long now) {
        var blackboard = blackboard();
        var fresh = target() == null;
        blackboard.set(CommonBlackboardKeys.TARGET, target);
        blackboard.set(ScorchbeastBlackboardKeys.TARGET_VISIBLE, true);
        lastSeenTick = now;
        if (fresh) {
            var random = beast.getRandom();
            cooldowns().set(SONIC_COOLDOWN, 40 + random.nextInt(40));
            cooldowns().set(DUST_COOLDOWN, 160 + random.nextInt(120));
        }
    }

    public void onHurt(LivingEntity attacker, long now) {
        if (!ScorchbeastEntity.validTarget(attacker)) {
            return;
        }
        var current = target();
        if (current == null || !visible(blackboard())) {
            acquire(attacker, now);
        }
    }

    private void clearTarget() {
        var blackboard = blackboard();
        blackboard.remove(CommonBlackboardKeys.TARGET);
        blackboard.remove(ScorchbeastBlackboardKeys.GROUND_STUCK);
        blackboard.set(ScorchbeastBlackboardKeys.TARGET_VISIBLE, false);
    }

    private boolean withinLeash(Vec3 pos) {
        var home = beast.home();
        if (home == null) {
            return true;
        }
        var leash = (double) AtomicDawn.CONFIG.scorchbeast.LEASH_RANGE;
        return pos.distanceToSqr(Vec3.atCenterOf(home)) <= leash * leash;
    }
}

package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CortexRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedHiveMind;
import mod.azure.atomicdawn.npc.NpcEntity;

public final class ScorchedBrain {

    public static final TagKey<EntityType<?>> ALLIES = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("scorched_allies")
    );

    public static final TagKey<EntityType<?>> EXTRA_TARGETS = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("scorched_targets")
    );

    public static final String MELEE_COOLDOWN = "atomicdawn:scorched/melee";

    public static final String FIRE_COOLDOWN = "atomicdawn:scorched/fire";

    public static final String ALERT_COOLDOWN = "atomicdawn:scorched/alert";

    public static final String PATROL_COOLDOWN = "atomicdawn:scorched/patrol";

    private static final int SENSOR_INTERVAL = 10;

    private static final int RECEIVE_THROTTLE = 20;

    private static final int LOSE_SIGHT_TICKS = 60;

    private static final int LOSE_SHARED_TICKS = 200;

    private final ScorchedEntity scorched;

    private final CortexRuntime<ScorchedEntity, Void> runtime;

    private long lastSeenTick;

    private long lastBroadcastTick = Long.MIN_VALUE / 2;

    @Nullable
    private LivingEntity lastBroadcastTarget;

    private long lastAcceptTick = Long.MIN_VALUE / 2;

    private long lastActiveTick;

    private int dormancyDelay;

    public ScorchedBrain(ScorchedEntity scorched) {
        this.scorched = scorched;
        this.runtime = new CortexRuntime<>(scorched, null, ScorchedTree.create());
        rollDormancyDelay();
    }

    public Blackboard blackboard() {
        return runtime.getBlackboard();
    }

    public void tick() {
        var now = scorched.level().getGameTime();
        sense(now);
        runtime.tick();
    }

    @Nullable
    public LivingEntity target() {
        LivingEntity target = blackboard().get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    private void sense(long now) {
        var blackboard = blackboard();
        var config = AtomicDawn.CONFIG.scorched;
        blackboard.set(ScorchedBlackboardKeys.DORMANT, scorched.isDormant());
        var home = scorched.home();
        if (home != null) {
            blackboard.set(ScorchedBlackboardKeys.FISSURE_ORIGIN, home);
        }
        var until = blackboard.get(ScorchedBlackboardKeys.INVESTIGATE_UNTIL);
        if (until != null && now >= until) {
            blackboard.remove(ScorchedBlackboardKeys.INVESTIGATE_UNTIL);
            blackboard.remove(ScorchedBlackboardKeys.LAST_KNOWN_POS);
            blackboard.set(ScorchedBlackboardKeys.RETURNING, true);
        }
        var target = target();
        if (target == null && blackboard.has(CommonBlackboardKeys.TARGET)) {
            clearTarget();
        } else if (target != null && (!validTarget(target) || !withinLeash(target))) {
            if (withinLeash(target)) {
                clearTarget();
            } else {
                abandon();
            }
            target = null;
        }
        if (target != null) {
            var range = config.DETECTION_RANGE * 1.5;
            var visible = scorched.getSensing().hasLineOfSight(target) && scorched.distanceToSqr(target) <= range
                * range;
            blackboard.set(ScorchedBlackboardKeys.TARGET_VISIBLE, visible);
            var shared = Boolean.TRUE.equals(blackboard.get(ScorchedBlackboardKeys.SHARED_TARGET));
            if (visible) {
                lastSeenTick = now;
                blackboard.set(ScorchedBlackboardKeys.SHARED_TARGET, false);
                blackboard.set(ScorchedBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
                blackboard.set(CommonBlackboardKeys.LAST_SEEN_POS, target.blockPosition());
                blackboard.set(CommonBlackboardKeys.LAST_SEEN_VELOCITY, target.getDeltaMovement());
                blackboard.set(CommonBlackboardKeys.LAST_SEEN_TICK, (int) now);
                ScorchedHiveMind.broadcast(scorched, target, false);
            } else if (shared) {
                blackboard.set(ScorchedBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
            }
            blackboard.set(CommonBlackboardKeys.LAST_KNOWN_TARGET_POS, target.blockPosition());
            if (!visible && now - lastSeenTick > (shared ? LOSE_SHARED_TICKS : LOSE_SIGHT_TICKS)) {
                loseTarget(now);
                target = null;
            }
        }
        if (
            target != null || blackboard.has(ScorchedBlackboardKeys.INVESTIGATE_UNTIL) || Boolean.TRUE.equals(
                blackboard.get(ScorchedBlackboardKeys.RETURNING)
            )
        ) {
            lastActiveTick = now;
        }
        if ((now + scorched.getId()) % SENSOR_INTERVAL == 0 && (target == null || !visible(blackboard))) {
            var found = scan();
            if (found != null && found != target) {
                acquire(found, false, now);
            }
        }
    }

    @Nullable
    private LivingEntity scan() {
        var config = AtomicDawn.CONFIG.scorched;
        var range = scorched.isDormant() ? config.DORMANT_DETECTION_RANGE : config.DETECTION_RANGE;
        return scorched.level()
            .getEntitiesOfClass(
                LivingEntity.class,
                scorched.getBoundingBox().inflate(range),
                candidate -> candidate != scorched && validTarget(candidate) && withinLeash(candidate) && scorched
                    .distanceToSqr(candidate) <= range * range && scorched.getSensing().hasLineOfSight(candidate)
            )
            .stream()
            .min(Comparator.comparingDouble(scorched::distanceToSqr))
            .orElse(null);
    }

    private static boolean visible(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(ScorchedBlackboardKeys.TARGET_VISIBLE));
    }

    private void acquire(LivingEntity target, boolean shared, long now) {
        var blackboard = blackboard();
        blackboard.set(CommonBlackboardKeys.TARGET, target);
        blackboard.set(ScorchedBlackboardKeys.SHARED_TARGET, shared);
        blackboard.set(ScorchedBlackboardKeys.TARGET_VISIBLE, !shared);
        blackboard.set(ScorchedBlackboardKeys.NEW_TARGET, !shared);
        blackboard.set(ScorchedBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
        blackboard.remove(ScorchedBlackboardKeys.INVESTIGATE_UNTIL);
        blackboard.set(ScorchedBlackboardKeys.RETURNING, false);
        if (!shared) {
            blackboard.remove(ScorchedBlackboardKeys.ALERT_ORIGIN);
        }
        lastSeenTick = now;
        lastActiveTick = now;
        if (scorched.isDormant()) {
            scorched.requestWake();
        }
        if (!shared) {
            ScorchedHiveMind.broadcast(scorched, target, true);
        }
    }

    public void acceptShared(LivingEntity target, BlockPos origin, long now) {
        if (!scorched.isAlive() || !validTarget(target) || !withinLeash(target)) {
            return;
        }
        var blackboard = blackboard();
        var current = target();
        if (current == target) {
            if (!visible(blackboard)) {
                lastSeenTick = Math.max(lastSeenTick, now - LOSE_SIGHT_TICKS / 2);
                blackboard.set(ScorchedBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
            }
            return;
        }
        if ((current != null && visible(blackboard)) || now - lastAcceptTick < RECEIVE_THROTTLE) {
            return;
        }
        lastAcceptTick = now;
        acquire(target, true, now);
        blackboard.set(ScorchedBlackboardKeys.ALERT_ORIGIN, origin);
        runtime.getCooldowns().set(ALERT_COOLDOWN, 100 + scorched.getRandom().nextInt(100));
    }

    public void onHurt(LivingEntity attacker, long now) {
        if (!validTarget(attacker)) {
            if (scorched.isDormant()) {
                scorched.requestWake();
            }
            return;
        }
        var current = target();
        if (current == attacker) {
            ScorchedHiveMind.broadcast(scorched, attacker, true);
        } else if (
            current == null || !visible(blackboard()) || scorched.distanceToSqr(attacker) < scorched
                .distanceToSqr(current)
        ) {
            acquire(attacker, false, now);
        }
        if (scorched.isDormant()) {
            scorched.requestWake();
        }
    }

    public boolean tryBroadcast(LivingEntity target, long now, int interval) {
        var wait = target == lastBroadcastTarget ? interval : Math.min(interval, 10);
        if (now - lastBroadcastTick < wait) {
            return false;
        }
        lastBroadcastTick = now;
        lastBroadcastTarget = target;
        return true;
    }

    private void loseTarget(long now) {
        var blackboard = blackboard();
        var last = blackboard.get(ScorchedBlackboardKeys.LAST_KNOWN_POS);
        clearTarget();
        if (last != null) {
            blackboard.set(
                ScorchedBlackboardKeys.INVESTIGATE_UNTIL,
                now + AtomicDawn.CONFIG.scorched.INVESTIGATE_SECONDS * 20L
            );
        } else {
            blackboard.set(ScorchedBlackboardKeys.RETURNING, true);
        }
    }

    private void abandon() {
        var blackboard = blackboard();
        clearTarget();
        blackboard.remove(ScorchedBlackboardKeys.INVESTIGATE_UNTIL);
        blackboard.remove(ScorchedBlackboardKeys.LAST_KNOWN_POS);
        blackboard.set(ScorchedBlackboardKeys.RETURNING, true);
    }

    private void clearTarget() {
        var blackboard = blackboard();
        blackboard.remove(CommonBlackboardKeys.TARGET);
        blackboard.remove(ScorchedBlackboardKeys.SHARED_TARGET);
        blackboard.remove(ScorchedBlackboardKeys.NEW_TARGET);
        blackboard.remove(ScorchedBlackboardKeys.ALERT_ORIGIN);
        blackboard.set(ScorchedBlackboardKeys.TARGET_VISIBLE, false);
    }

    private boolean withinLeash(LivingEntity target) {
        var home = scorched.home();
        if (home == null) {
            return true;
        }
        var leash = (double) AtomicDawn.CONFIG.scorched.LEASH_RANGE;
        return scorched.distanceToSqr(Vec3.atCenterOf(home)) <= leash * leash && target.distanceToSqr(
            Vec3.atCenterOf(home)
        ) <= leash * leash;
    }

    public void onWake(long now) {
        lastActiveTick = now;
        rollDormancyDelay();
    }

    public boolean readyForDormancy(long now) {
        return now - lastActiveTick >= dormancyDelay;
    }

    public void markActive(long now) {
        lastActiveTick = now;
    }

    private void rollDormancyDelay() {
        var config = AtomicDawn.CONFIG.scorched;
        var min = Math.min(config.DORMANCY_MIN_SECONDS, config.DORMANCY_MAX_SECONDS);
        var max = Math.max(config.DORMANCY_MIN_SECONDS, config.DORMANCY_MAX_SECONDS);
        dormancyDelay = (min + scorched.getRandom().nextInt(max - min + 1)) * 20;
    }

    public static boolean validTarget(LivingEntity candidate) {
        if (!candidate.isAlive() || candidate instanceof ScorchedEntity || candidate.isSpectator()) {
            return false;
        }
        var holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(candidate.getType());
        if (holder.is(ALLIES)) {
            return false;
        }
        if (candidate instanceof Player player) {
            return !player.isCreative();
        }
        if (candidate instanceof NpcEntity npc) {
            return npc.canFight();
        }
        if (candidate instanceof AbstractVillager) {
            return AtomicDawn.CONFIG.scorched.ATTACK_VILLAGERS;
        }
        return candidate instanceof AbstractGolem || holder.is(EXTRA_TARGETS);
    }
}

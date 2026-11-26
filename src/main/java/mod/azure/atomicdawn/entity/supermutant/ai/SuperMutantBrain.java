package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CortexRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantPack;
import mod.azure.atomicdawn.npc.NpcEntity;

public final class SuperMutantBrain {

    public static final TagKey<EntityType<?>> ALLIES = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("super_mutant_allies")
    );

    public static final TagKey<EntityType<?>> TARGETS = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("super_mutant_targets")
    );

    public static final String MELEE_COOLDOWN = "atomicdawn:super_mutant/melee";

    public static final String SLAM_COOLDOWN = "atomicdawn:super_mutant/slam";

    public static final String FIRE_COOLDOWN = "atomicdawn:super_mutant/fire";

    public static final String ALERT_COOLDOWN = "atomicdawn:super_mutant/alert";

    public static final String TAUNT_COOLDOWN = "atomicdawn:super_mutant/taunt";

    public static final String PATROL_COOLDOWN = "atomicdawn:super_mutant/patrol";

    private static final int SENSOR_INTERVAL = 10;

    private static final int RECEIVE_THROTTLE = 20;

    private static final int LOSE_SIGHT_TICKS = 120;

    private static final int LOSE_SHARED_TICKS = 240;

    private static final int CALL_INTERVAL = 60;

    private static final double PREDICTION_TICKS = 25.0;

    private static final double MAX_PREDICTION = 10.0;

    private final SuperMutantEntity mutant;

    private final CortexRuntime<SuperMutantEntity, Void> runtime;

    private long lastSeenTick;

    private long lastCallTick = Long.MIN_VALUE / 2;

    @Nullable
    private LivingEntity lastCallTarget;

    private long lastAcceptTick = Long.MIN_VALUE / 2;

    public SuperMutantBrain(SuperMutantEntity mutant) {
        this.mutant = mutant;
        this.runtime = new CortexRuntime<>(mutant, null, SuperMutantTree.create());
    }

    public Blackboard blackboard() {
        return runtime.getBlackboard();
    }

    public void tick() {
        sense(mutant.level().getGameTime());
        runtime.tick();
    }

    @Nullable
    public LivingEntity target() {
        LivingEntity target = blackboard().get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    private void sense(long now) {
        var blackboard = blackboard();
        var config = AtomicDawn.CONFIG.server.entities.mutants.superMutants.behavior;
        var until = blackboard.get(SuperMutantBlackboardKeys.INVESTIGATE_UNTIL);
        if (until != null && now >= until) {
            blackboard.remove(SuperMutantBlackboardKeys.INVESTIGATE_UNTIL);
            blackboard.remove(SuperMutantBlackboardKeys.LAST_KNOWN_POS);
            blackboard.set(SuperMutantBlackboardKeys.RETURNING, true);
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
            var visible = mutant.getSensing().hasLineOfSight(target) && mutant.distanceToSqr(target) <= range * range;
            blackboard.set(SuperMutantBlackboardKeys.TARGET_VISIBLE, visible);
            var shared = shared(blackboard);
            if (visible) {
                lastSeenTick = now;
                blackboard.set(SuperMutantBlackboardKeys.SHARED_TARGET, false);
                blackboard.set(SuperMutantBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
                blackboard.set(CommonBlackboardKeys.LAST_SEEN_POS, target.blockPosition());
                blackboard.set(CommonBlackboardKeys.LAST_SEEN_VELOCITY, target.getDeltaMovement());
                blackboard.set(CommonBlackboardKeys.LAST_SEEN_TICK, (int) now);
            } else if (shared) {
                blackboard.set(SuperMutantBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
            }
            blackboard.set(CommonBlackboardKeys.LAST_KNOWN_TARGET_POS, target.blockPosition());
            if (!visible && now - lastSeenTick > (shared ? LOSE_SHARED_TICKS : LOSE_SIGHT_TICKS)) {
                loseTarget(now);
                target = null;
            }
        }
        if ((now + mutant.getId()) % SENSOR_INTERVAL == 0) {
            var found = scan();
            if (found != null && found != target && shouldSwitch(target, found, blackboard)) {
                acquire(found, false, now);
            }
        }
    }

    private boolean shouldSwitch(@Nullable LivingEntity current, LivingEntity found, Blackboard blackboard) {
        if (current == null || !visible(blackboard)) {
            return true;
        }
        return mutant.distanceToSqr(found) * 4.0 < mutant.distanceToSqr(current);
    }

    @Nullable
    private LivingEntity scan() {
        var range = AtomicDawn.CONFIG.server.entities.mutants.superMutants.behavior.DETECTION_RANGE;
        return mutant.level()
            .getEntitiesOfClass(
                LivingEntity.class,
                mutant.getBoundingBox().inflate(range),
                candidate -> candidate != mutant && validTarget(candidate) && withinLeash(candidate) && mutant
                    .distanceToSqr(candidate) <= range * range && mutant.getSensing().hasLineOfSight(candidate)
            )
            .stream()
            .min(Comparator.comparingDouble(mutant::distanceToSqr))
            .orElse(null);
    }

    private static boolean visible(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(SuperMutantBlackboardKeys.TARGET_VISIBLE));
    }

    private static boolean shared(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(SuperMutantBlackboardKeys.SHARED_TARGET));
    }

    private void acquire(LivingEntity target, boolean shared, long now) {
        var blackboard = blackboard();
        var fresh = target() == null;
        blackboard.set(CommonBlackboardKeys.TARGET, target);
        blackboard.set(SuperMutantBlackboardKeys.SHARED_TARGET, shared);
        blackboard.set(SuperMutantBlackboardKeys.TARGET_VISIBLE, !shared);
        blackboard.set(SuperMutantBlackboardKeys.NEW_TARGET, !shared && fresh);
        blackboard.set(SuperMutantBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
        blackboard.remove(SuperMutantBlackboardKeys.INVESTIGATE_UNTIL);
        blackboard.set(SuperMutantBlackboardKeys.RETURNING, false);
        if (!shared) {
            blackboard.remove(SuperMutantBlackboardKeys.ALERT_ORIGIN);
        }
        if (fresh) {
            runtime.getCooldowns().set(TAUNT_COOLDOWN, tauntDelay());
        }
        lastSeenTick = now;
    }

    public int tauntDelay() {
        var config = AtomicDawn.CONFIG.server.entities.mutants.superMutants.behavior;
        var min = Math.min(config.TAUNT_MIN_SECONDS, config.TAUNT_MAX_SECONDS);
        var max = Math.max(config.TAUNT_MIN_SECONDS, config.TAUNT_MAX_SECONDS);
        return (min + mutant.getRandom().nextInt(max - min + 1)) * 20;
    }

    public void acceptShared(LivingEntity target, BlockPos origin, long now) {
        if (!mutant.isAlive() || !validTarget(target) || !withinLeash(target)) {
            return;
        }
        var blackboard = blackboard();
        var current = target();
        if (current == target) {
            if (!visible(blackboard)) {
                lastSeenTick = Math.max(lastSeenTick, now - LOSE_SIGHT_TICKS / 2);
                blackboard.set(SuperMutantBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
            }
            return;
        }
        if ((current != null && visible(blackboard)) || now - lastAcceptTick < RECEIVE_THROTTLE) {
            return;
        }
        lastAcceptTick = now;
        acquire(target, true, now);
        blackboard.set(SuperMutantBlackboardKeys.ALERT_ORIGIN, origin);
        runtime.getCooldowns().set(ALERT_COOLDOWN, 60 + mutant.getRandom().nextInt(60));
    }

    public void onHurt(LivingEntity attacker, long now) {
        if (!validTarget(attacker)) {
            return;
        }
        var current = target();
        if (
            current != attacker && (current == null || !visible(blackboard()) || mutant.distanceToSqr(attacker) < mutant
                .distanceToSqr(current))
        ) {
            acquire(attacker, false, now);
        }
        SuperMutantPack.call(mutant, attacker);
    }

    public void onDisturbance(Vec3 pos, @Nullable Entity source, long now) {
        if (target() != null) {
            return;
        }
        var config = AtomicDawn.CONFIG.server.entities.mutants.superMutants.behavior;
        if (
            source instanceof LivingEntity living && validTarget(living) && withinLeash(living) && mutant.distanceToSqr(
                living
            ) <= config.DETECTION_RANGE * config.DETECTION_RANGE && mutant.getSensing().hasLineOfSight(living)
        ) {
            acquire(living, false, now);
            return;
        }
        if (!withinLeash(pos)) {
            return;
        }
        var blackboard = blackboard();
        blackboard.set(SuperMutantBlackboardKeys.LAST_KNOWN_POS, BlockPos.containing(pos));
        blackboard.set(SuperMutantBlackboardKeys.INVESTIGATE_UNTIL, now + config.INVESTIGATE_SECONDS * 20L);
        blackboard.set(SuperMutantBlackboardKeys.RETURNING, false);
        mutant.getLookControl().setLookAt(pos.x, pos.y, pos.z);
    }

    public boolean tryCall(LivingEntity target, long now) {
        var wait = target == lastCallTarget ? CALL_INTERVAL : 10;
        if (now - lastCallTick < wait) {
            return false;
        }
        lastCallTick = now;
        lastCallTarget = target;
        return true;
    }

    private void loseTarget(long now) {
        var blackboard = blackboard();
        var last = blackboard.get(SuperMutantBlackboardKeys.LAST_KNOWN_POS);
        Vec3 velocity = blackboard.get(CommonBlackboardKeys.LAST_SEEN_VELOCITY);
        clearTarget();
        if (last == null) {
            blackboard.set(SuperMutantBlackboardKeys.RETURNING, true);
            return;
        }
        var guess = Vec3.atBottomCenterOf(last);
        if (velocity != null) {
            var lead = new Vec3(velocity.x, 0, velocity.z).scale(PREDICTION_TICKS);
            if (lead.length() > MAX_PREDICTION) {
                lead = lead.normalize().scale(MAX_PREDICTION);
            }
            var predicted = guess.add(lead);
            if (withinLeash(predicted)) {
                guess = predicted;
            }
        }
        blackboard.set(SuperMutantBlackboardKeys.LAST_KNOWN_POS, BlockPos.containing(guess));
        blackboard.set(
            SuperMutantBlackboardKeys.INVESTIGATE_UNTIL,
            now + AtomicDawn.CONFIG.server.entities.mutants.superMutants.behavior.INVESTIGATE_SECONDS * 20L
        );
    }

    private void abandon() {
        var blackboard = blackboard();
        clearTarget();
        blackboard.remove(SuperMutantBlackboardKeys.INVESTIGATE_UNTIL);
        blackboard.remove(SuperMutantBlackboardKeys.LAST_KNOWN_POS);
        blackboard.set(SuperMutantBlackboardKeys.RETURNING, true);
    }

    private void clearTarget() {
        var blackboard = blackboard();
        blackboard.remove(CommonBlackboardKeys.TARGET);
        blackboard.remove(SuperMutantBlackboardKeys.SHARED_TARGET);
        blackboard.remove(SuperMutantBlackboardKeys.NEW_TARGET);
        blackboard.remove(SuperMutantBlackboardKeys.ALERT_ORIGIN);
        blackboard.set(SuperMutantBlackboardKeys.TARGET_VISIBLE, false);
    }

    private boolean withinLeash(LivingEntity target) {
        return withinLeash(mutant.position()) && withinLeash(target.position());
    }

    private boolean withinLeash(Vec3 pos) {
        var home = mutant.home();
        if (home == null) {
            return true;
        }
        var leash = (double) AtomicDawn.CONFIG.server.entities.mutants.superMutants.behavior.LEASH_RANGE;
        return pos.distanceToSqr(Vec3.atCenterOf(home)) <= leash * leash;
    }

    public static boolean ally(Entity entity) {
        return entity instanceof SuperMutantEntity || BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType())
            .is(ALLIES);
    }

    public static boolean validTarget(LivingEntity candidate) {
        if (!candidate.isAlive() || candidate.isSpectator() || ally(candidate)) {
            return false;
        }
        if (candidate instanceof Player player) {
            return !player.isCreative();
        }
        if (candidate instanceof NpcEntity npc) {
            return !npc.isInvulnerable();
        }
        if (candidate instanceof AbstractVillager) {
            return AtomicDawn.CONFIG.server.entities.mutants.superMutants.behavior.ATTACK_VILLAGERS;
        }
        return candidate instanceof AbstractGolem || BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(candidate.getType())
            .is(TARGETS);
    }
}

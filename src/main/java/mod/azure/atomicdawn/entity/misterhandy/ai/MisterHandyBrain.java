package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CortexRuntime;
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
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.registry.ADSounds;

public final class MisterHandyBrain {

    public static final TagKey<EntityType<?>> ALLIES = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("mister_handy_allies")
    );

    public static final TagKey<EntityType<?>> TARGETS = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("mister_handy_targets")
    );

    public static final String GLOBAL_COOLDOWN = "atomicdawn:mister_handy/global";

    public static final String SAW_COOLDOWN = "atomicdawn:mister_handy/saw";

    public static final String CLAW_COOLDOWN = "atomicdawn:mister_handy/claw";

    public static final String FLAMER_COOLDOWN = "atomicdawn:mister_handy/flamer";

    public static final String PATROL_COOLDOWN = "atomicdawn:mister_handy/patrol";

    private static final int SENSOR_INTERVAL = 10;

    private static final int LOSE_SIGHT_TICKS = 140;

    private final MisterHandyEntity robot;

    private final CortexRuntime<MisterHandyEntity, Void> runtime;

    private long lastSeenTick;

    public MisterHandyBrain(MisterHandyEntity robot) {
        this.robot = robot;
        this.runtime = new CortexRuntime<>(robot, null, MisterHandyTree.create());
    }

    public Blackboard blackboard() {
        return runtime.getBlackboard();
    }

    public void tick() {
        sense(robot.level().getGameTime());
        runtime.tick();
    }

    @Nullable
    public LivingEntity target() {
        LivingEntity target = blackboard().get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    private double detection() {
        var range = AtomicDawn.CONFIG.misterHandy.DETECTION_RANGE;
        return robot.opticsDamaged() ? range * 0.5 : range;
    }

    private void sense(long now) {
        var blackboard = blackboard();
        var until = blackboard.get(MisterHandyBlackboardKeys.INVESTIGATE_UNTIL);
        if (until != null && now >= until) {
            blackboard.remove(MisterHandyBlackboardKeys.INVESTIGATE_UNTIL);
            blackboard.remove(MisterHandyBlackboardKeys.LAST_KNOWN_POS);
        }
        var target = target();
        if (target == null && blackboard.has(CommonBlackboardKeys.TARGET)) {
            clearTarget();
        } else if (target != null && (!validTarget(target) || !withinLeash(target.position()))) {
            clearTarget();
            target = null;
        }
        if (target != null) {
            var range = detection() * 1.5;
            var visible = robot.getSensing().hasLineOfSight(target) && robot.distanceToSqr(target) <= range * range;
            blackboard.set(MisterHandyBlackboardKeys.TARGET_VISIBLE, visible);
            if (visible) {
                lastSeenTick = now;
                blackboard.set(MisterHandyBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
            }
            if (!visible && now - lastSeenTick > LOSE_SIGHT_TICKS) {
                clearTarget();
                blackboard.set(
                    MisterHandyBlackboardKeys.INVESTIGATE_UNTIL,
                    now + AtomicDawn.CONFIG.misterHandy.INVESTIGATE_SECONDS * 20L
                );
                target = null;
            }
        }
        if ((now + robot.getId()) % SENSOR_INTERVAL == 0 && (target == null || !visible(blackboard))) {
            var found = scan();
            if (found != null && found != target) {
                acquire(found, now);
            }
        }
    }

    @Nullable
    private LivingEntity scan() {
        var range = detection();
        return robot.level()
            .getEntitiesOfClass(
                LivingEntity.class,
                robot.getBoundingBox().inflate(range),
                candidate -> candidate != robot && validTarget(candidate) && withinLeash(candidate.position()) && robot
                    .distanceToSqr(candidate) <= range * range && robot.getSensing().hasLineOfSight(candidate)
            )
            .stream()
            .min(Comparator.comparingDouble(robot::distanceToSqr))
            .orElse(null);
    }

    private static boolean visible(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(MisterHandyBlackboardKeys.TARGET_VISIBLE));
    }

    private void acquire(LivingEntity target, long now) {
        var blackboard = blackboard();
        var fresh = target() == null;
        blackboard.set(CommonBlackboardKeys.TARGET, target);
        blackboard.set(MisterHandyBlackboardKeys.TARGET_VISIBLE, true);
        blackboard.set(MisterHandyBlackboardKeys.LAST_KNOWN_POS, target.blockPosition());
        blackboard.remove(MisterHandyBlackboardKeys.INVESTIGATE_UNTIL);
        lastSeenTick = now;
        if (fresh) {
            robot.setScanning(false);
            robot.playSound(ADSounds.MISTER_HANDY_ALERT.get(), 1.4F, 1.0F);
        }
    }

    public void onHurt(LivingEntity attacker, long now) {
        if (!validTarget(attacker)) {
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
        blackboard.set(MisterHandyBlackboardKeys.TARGET_VISIBLE, false);
    }

    private boolean withinLeash(Vec3 pos) {
        var home = robot.home();
        if (home == null) {
            return true;
        }
        var leash = (double) AtomicDawn.CONFIG.misterHandy.LEASH_RANGE;
        return pos.distanceToSqr(Vec3.atCenterOf(home)) <= leash * leash;
    }

    public static boolean validTarget(LivingEntity candidate) {
        if (!candidate.isAlive() || candidate.isSpectator() || MisterHandyEntity.ally(candidate)) {
            return false;
        }
        if (candidate instanceof Player player) {
            return !player.isCreative();
        }
        if (candidate instanceof NpcEntity npc) {
            return !npc.isInvulnerable();
        }
        if (candidate instanceof AbstractVillager || candidate instanceof AbstractGolem) {
            return true;
        }
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(candidate.getType()).is(TARGETS);
    }
}

package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantWeapons;
import mod.azure.atomicdawn.weapon.combat.MobGunfire;

final class SuperMutantRangedTask extends SuperMutantTask {

    private static final int AIM_TICKS = 14;

    private static final double CLOSE_SPEED = 1.05;

    private static final double BACKSTEP_SPEED = 0.9;

    private static final double STRAFE_SPEED = 0.75;

    private static final int VANTAGE_INTERVAL = 30;

    private static final int PLANT_TICKS = 6;

    private static final float FACING_TOLERANCE = 20F;

    private int ticks;

    private int aimTicks;

    private int strafeTicks;

    private int blindTicks;

    SuperMutantRangedTask() {
        super(45, true);
    }

    static boolean ready(SuperMutantEntity agent, Blackboard blackboard) {
        return hunting(blackboard) && SuperMutantWeapons.kind(agent) == SuperMutantWeapons.Kind.GUN
            && !SuperMutantReloadTask.ready(agent);
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        aimTicks = 0;
        blindTicks = 0;
        strafeTicks = 30 + agent.getRandom().nextInt(30);
        agent.setStance(SuperMutantWeapons.stance(agent));
        state(blackboard, SuperMutantCombatState.RANGED);
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        var definition = SuperMutantWeapons.definition(agent);
        if (target == null || definition == null || SuperMutantWeapons.kind(agent) != SuperMutantWeapons.Kind.GUN) {
            return ActionOutcome.success();
        }
        if (MobGunfire.needsReload(agent.getMainHandItem(), definition)) {
            return ActionOutcome.success();
        }
        var config = AtomicDawn.CONFIG.superMutants;
        var min = Math.min(config.MIN_ENGAGE_RANGE, config.MAX_ENGAGE_RANGE);
        var max = Math.max(config.MIN_ENGAGE_RANGE, config.MAX_ENGAGE_RANGE);
        var range = Math.min(max, definition.damage().range());
        var distance = agent.distanceTo(target);
        var visible = visible(blackboard);
        ticks++;
        if (visible) {
            blindTicks = 0;
            agent.getLookControl().setLookAt(target, 30F, 30F);
        } else {
            blindTicks++;
            var last = blackboard.get(SuperMutantBlackboardKeys.LAST_KNOWN_POS);
            if (last != null) {
                agent.getLookControl().setLookAt(last.getX() + 0.5, last.getY() + 1.5, last.getZ() + 0.5);
            }
        }
        if (!visible) {
            aimTicks = 0;
            if (blindTicks % VANTAGE_INTERVAL == 1) {
                seekSight(agent, target, blackboard);
            }
        } else if (distance > range) {
            aimTicks = Math.min(aimTicks, AIM_TICKS / 2);
            if (ticks % 10 == 1) {
                agent.getNavigation().moveTo(target, CLOSE_SPEED);
            }
        } else if (distance < min) {
            if (ticks % 15 == 1) {
                backstep(agent, target, min);
            }
        } else if (--strafeTicks <= 0) {
            strafe(agent, target);
            strafeTicks = 50 + agent.getRandom().nextInt(50);
        }
        var inRange = visible && distance <= range + 2;
        if (inRange) {
            aimTicks++;
        }
        var ready = cooldowns.ready(SuperMutantBrain.FIRE_COOLDOWN);
        if (inRange && ready && aimTicks >= AIM_TICKS - PLANT_TICKS) {
            agent.getNavigation().stop();
            face(agent, target);
        } else if (visible && agent.getNavigation().isDone()) {
            face(agent, target);
        }
        if (
            inRange && aimTicks >= AIM_TICKS && ready && facing(agent, target)
                && agent.level() instanceof ServerLevel level
        ) {
            var fired = MobGunfire.fire(
                level,
                agent,
                agent.getMainHandItem(),
                definition,
                target,
                (float) config.AIM_SPREAD,
                (float) config.GUN_DAMAGE_MULTIPLIER,
                entity -> !(entity instanceof SuperMutantEntity)
            );
            if (fired) {
                agent.playAction(SuperMutantWeapons.fireAction(definition));
                cooldowns.set(
                    SuperMutantBrain.FIRE_COOLDOWN,
                    SuperMutantWeapons.fireInterval(definition, agent.getRandom())
                );
            }
        }
        return ActionOutcome.running();
    }

    private static float yawTo(SuperMutantEntity agent, LivingEntity target) {
        var dx = target.getX() - agent.getX();
        var dz = target.getZ() - agent.getZ();
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
    }

    private static void face(SuperMutantEntity agent, LivingEntity target) {
        var yaw = yawTo(agent, target);
        agent.setYRot(yaw);
        agent.yBodyRot = yaw;
        agent.setYHeadRot(yaw);
        agent.getLookControl().setLookAt(target, 30F, 30F);
    }

    private static boolean facing(SuperMutantEntity agent, LivingEntity target) {
        return Math.abs(Mth.wrapDegrees(yawTo(agent, target) - agent.yBodyRot)) <= FACING_TOLERANCE;
    }

    private void seekSight(SuperMutantEntity agent, LivingEntity target, Blackboard blackboard) {
        var vantage = vantage(agent, target);
        if (vantage != null && agent.getNavigation().moveTo(vantage.x, vantage.y, vantage.z, CLOSE_SPEED)) {
            return;
        }
        var last = blackboard.get(SuperMutantBlackboardKeys.LAST_KNOWN_POS);
        if (last != null) {
            agent.getNavigation().moveTo(last.getX() + 0.5, last.getY(), last.getZ() + 0.5, CLOSE_SPEED);
        } else {
            agent.getNavigation().moveTo(target, CLOSE_SPEED);
        }
    }

    @Nullable
    private static Vec3 vantage(SuperMutantEntity agent, LivingEntity target) {
        var level = agent.level();
        var eye = agent.getEyeHeight();
        var aim = target.getEyePosition();
        var toward = Math.atan2(target.getZ() - agent.getZ(), target.getX() - agent.getX());
        Vec3 best = null;
        var bestScore = Double.MAX_VALUE;
        for (var ring = 4; ring <= 10; ring += 3) {
            for (var step = 0; step < 10; step++) {
                var angle = toward + (step - 4.5) * (Math.PI / 6.0);
                var x = agent.getX() + Math.cos(angle) * ring;
                var z = agent.getZ() + Math.sin(angle) * ring;
                var y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(x),
                    Mth.floor(z)
                );
                if (Math.abs(y - agent.getY()) > 4) {
                    continue;
                }
                var from = new Vec3(x, y + eye, z);
                var hit = level.clip(
                    new ClipContext(from, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, agent)
                );
                if (hit.getType() != HitResult.Type.MISS) {
                    continue;
                }
                var score = agent.position().distanceToSqr(x, y, z);
                if (score < bestScore) {
                    bestScore = score;
                    best = new Vec3(x, y, z);
                }
            }
        }
        return best;
    }

    private static void backstep(SuperMutantEntity agent, LivingEntity target, int min) {
        var away = DefaultRandomPos.getPosAway(agent, Math.max(4, min / 2 + 2), 3, target.position());
        if (away != null) {
            agent.getNavigation().moveTo(away.x, away.y, away.z, BACKSTEP_SPEED);
        }
    }

    private static void strafe(SuperMutantEntity agent, LivingEntity target) {
        var random = agent.getRandom();
        if (random.nextInt(3) == 0) {
            agent.getNavigation().stop();
            return;
        }
        var dx = target.getX() - agent.getX();
        var dz = target.getZ() - agent.getZ();
        var length = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        var side = random.nextBoolean() ? 1.0 : -1.0;
        var step = 2.0 + random.nextDouble() * 2.0;
        var x = agent.getX() - dz / length * side * step;
        var z = agent.getZ() + dx / length * side * step;
        agent.getNavigation().moveTo(x, agent.getY(), z, STRAFE_SPEED);
    }

    @Override
    public void stop(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

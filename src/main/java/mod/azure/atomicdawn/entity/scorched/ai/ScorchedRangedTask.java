package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;
import mod.azure.atomicdawn.weapon.combat.MobGunfire;

final class ScorchedRangedTask extends ScorchedTask {

    private static final int AIM_TICKS = 12;

    private static final double CLOSE_SPEED = 1.15;

    private static final double RETREAT_SPEED = 1.1;

    private static final double STRAFE_SPEED = 0.85;

    private static final int PLANT_TICKS = 6;

    private static final float FACING_TOLERANCE = 20F;

    private int ticks;

    private int aimTicks;

    private int strafeTicks;

    ScorchedRangedTask() {
        super(45, true);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard) {
        return hunting(agent, blackboard) && ScorchedWeapons.kind(agent) == ScorchedWeapons.Kind.GUN
            && !ScorchedReloadTask.ready(agent);
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        aimTicks = 0;
        strafeTicks = 20 + agent.getRandom().nextInt(20);
        blackboard.remove(ScorchedBlackboardKeys.NEW_TARGET);
        state(blackboard, ScorchedCombatState.RANGED);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        var definition = ScorchedWeapons.definition(agent);
        if (target == null || definition == null || ScorchedWeapons.kind(agent) != ScorchedWeapons.Kind.GUN) {
            return ActionOutcome.success();
        }
        if (MobGunfire.needsReload(agent.getMainHandItem(), definition)) {
            return ActionOutcome.success();
        }
        var config = AtomicDawn.CONFIG.server.entities.mutants.scorched.combat;
        var min = Math.min(config.MIN_ENGAGE_RANGE, config.MAX_ENGAGE_RANGE);
        var max = Math.max(config.MIN_ENGAGE_RANGE, config.MAX_ENGAGE_RANGE);
        var distance = agent.distanceTo(target);
        var visible = visible(blackboard);
        agent.getLookControl().setLookAt(target, 30F, 30F);
        ticks++;
        if (!visible || distance > max) {
            aimTicks = 0;
            if (ticks % 10 == 1) {
                agent.getNavigation().moveTo(target, CLOSE_SPEED);
            }
        } else if (distance < min) {
            if (ticks % 10 == 1) {
                retreat(agent, target);
            }
        } else if (--strafeTicks <= 0) {
            strafe(agent, target);
            strafeTicks = 40 + agent.getRandom().nextInt(40);
        }
        var inRange = visible && distance <= max + 2;
        if (inRange) {
            aimTicks++;
        }
        agent.setStance(ScorchedWeapons.stance(agent, inRange));
        var ready = cooldowns.ready(ScorchedBrain.FIRE_COOLDOWN);
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
                entity -> !(entity instanceof ScorchedEntity)
            );
            if (fired) {
                agent.playAction(ScorchedWeapons.fireAction(definition));
                cooldowns.set(ScorchedBrain.FIRE_COOLDOWN, ScorchedWeapons.fireInterval(definition, agent.getRandom()));
            }
        }
        return ActionOutcome.running();
    }

    private static float yawTo(ScorchedEntity agent, LivingEntity target) {
        var dx = target.getX() - agent.getX();
        var dz = target.getZ() - agent.getZ();
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
    }

    private static void face(ScorchedEntity agent, LivingEntity target) {
        var yaw = yawTo(agent, target);
        agent.setYRot(yaw);
        agent.yBodyRot = yaw;
        agent.setYHeadRot(yaw);
        agent.getLookControl().setLookAt(target, 30F, 30F);
    }

    private static boolean facing(ScorchedEntity agent, LivingEntity target) {
        return Math.abs(Mth.wrapDegrees(yawTo(agent, target) - agent.yBodyRot)) <= FACING_TOLERANCE;
    }

    private void retreat(ScorchedEntity agent, LivingEntity target) {
        var away = DefaultRandomPos.getPosAway(agent, 8, 4, target.position());
        if (away != null) {
            agent.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED);
        }
    }

    private void strafe(ScorchedEntity agent, LivingEntity target) {
        var random = agent.getRandom();
        if (random.nextInt(3) == 0) {
            agent.getNavigation().stop();
            return;
        }
        var dx = target.getX() - agent.getX();
        var dz = target.getZ() - agent.getZ();
        var length = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        var side = random.nextBoolean() ? 1.0 : -1.0;
        var step = 2.5 + random.nextDouble() * 2.0;
        var x = agent.getX() - dz / length * side * step;
        var z = agent.getZ() + dx / length * side * step;
        agent.getNavigation().moveTo(x, agent.getY(), z, STRAFE_SPEED);
    }

    @Override
    public void stop(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
        agent.setStance(ScorchedWeapons.stance(agent, false));
    }
}

package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;

final class ScorchedPatrolTask extends ScorchedTask {

    private static final int MAX_TICKS = 200;

    private static final double SPEED = 0.8;

    private int ticks;

    ScorchedPatrolTask() {
        super(10, true);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        return !agent.isDormant() && target(blackboard) == null && !investigating(blackboard) && !returning(
            blackboard
        ) && cooldowns.ready(ScorchedBrain.PATROL_COOLDOWN);
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.setStance(ScorchedWeapons.stance(agent, false));
        state(blackboard, ScorchedCombatState.PATROLLING);
        var destination = destination(agent);
        if (destination != null) {
            agent.getNavigation().moveTo(destination.x, destination.y, destination.z, SPEED);
        }
    }

    private static Vec3 destination(ScorchedEntity agent) {
        var home = agent.home();
        var random = agent.getRandom();
        if (home == null) {
            return DefaultRandomPos.getPos(agent, 10, 6);
        }
        var angle = random.nextFloat() * Mth.TWO_PI;
        var distance = Math.sqrt(random.nextDouble()) * agent.patrolRadius();
        var goal = new Vec3(
            home.getX() + 0.5 + Mth.cos(angle) * distance,
            agent.getY(),
            home.getZ() + 0.5 + Mth.sin(angle) * distance
        );
        var step = DefaultRandomPos.getPosTowards(agent, 14, 7, goal, Math.PI / 2);
        if (step == null) {
            return null;
        }
        var radius = agent.patrolRadius();
        return step.distanceToSqr(Vec3.atBottomCenterOf(home)) <= radius * radius ? step : null;
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (agent.getNavigation().isDone() || ++ticks >= MAX_TICKS) {
            return ActionOutcome.success();
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
        cooldowns.set(ScorchedBrain.PATROL_COOLDOWN, 100 + agent.getRandom().nextInt(200));
    }
}

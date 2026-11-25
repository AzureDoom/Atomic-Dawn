package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;

final class ScorchedReturnHomeTask extends ScorchedTask {

    private static final double SPEED = 1.0;

    private static final int MAX_TICKS = 900;

    private int ticks;

    ScorchedReturnHomeTask() {
        super(20, true);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard) {
        var home = agent.home();
        if (agent.isDormant() || target(blackboard) != null || investigating(blackboard) || home == null) {
            return false;
        }
        var radius = agent.patrolRadius() + 8.0;
        return returning(blackboard) || agent.distanceToSqr(Vec3.atCenterOf(home)) > radius * radius;
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.setStance(ScorchedWeapons.stance(agent, false));
        state(blackboard, ScorchedCombatState.RETURNING);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var home = agent.home();
        if (home == null) {
            blackboard.set(ScorchedBlackboardKeys.RETURNING, false);
            return ActionOutcome.success();
        }
        var arrive = Math.max(6.0, agent.patrolRadius() * 0.35);
        if (agent.distanceToSqr(Vec3.atCenterOf(home)) <= arrive * arrive || ++ticks >= MAX_TICKS) {
            blackboard.set(ScorchedBlackboardKeys.RETURNING, false);
            return ActionOutcome.success();
        }
        if (ticks % 40 == 1 || agent.getNavigation().isDone()) {
            var goal = Vec3.atBottomCenterOf(home);
            if (!agent.getNavigation().moveTo(goal.x, goal.y, goal.z, SPEED)) {
                var step = DefaultRandomPos.getPosTowards(agent, 16, 7, goal, Math.PI / 2);
                if (step != null) {
                    agent.getNavigation().moveTo(step.x, step.y, step.z, SPEED);
                }
            }
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

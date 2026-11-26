package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

final class ScorchbeastGroundChaseTask extends ScorchbeastTask {

    private static final double SPEED = 1.0;

    private int ticks;

    ScorchbeastGroundChaseTask() {
        super(40, true);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard) {
        return hunting(blackboard) && agent.grounded();
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null || !agent.grounded()) {
            return ActionOutcome.success();
        }
        agent.getLookControl().setLookAt(target, 15F, 15F);
        var reach = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.groundAttacks.WING_SWEEP_RANGE + agent
            .getBbWidth() * 0.5;
        var distance = horizontalDistance(agent, target);
        if (distance <= reach * 0.85) {
            agent.getNavigation().stop();
            agent.faceTowards(target.position());
            blackboard.remove(ScorchbeastBlackboardKeys.GROUND_STUCK);
            return ActionOutcome.running();
        }
        if (ticks++ % 10 == 0) {
            var moving = agent.getNavigation().moveTo(target, SPEED);
            var stuck = blackboard.get(ScorchbeastBlackboardKeys.GROUND_STUCK);
            var count = stuck == null ? 0 : stuck;
            blackboard.set(ScorchbeastBlackboardKeys.GROUND_STUCK, moving ? Math.max(0, count - 1) : count + 1);
        }
        return ActionOutcome.running();
    }

    @Override
    void end(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

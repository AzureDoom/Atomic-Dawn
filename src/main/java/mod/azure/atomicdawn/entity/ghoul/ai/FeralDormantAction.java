package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

final class FeralDormantAction extends FeralAction {

    FeralDormantAction() {
        super(100, false);
    }

    @Override
    public void start(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
    }

    @Override
    public ActionOutcome<Void> tick(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (!agent.isDormant()) {
            return ActionOutcome.success();
        }
        agent.getNavigation().stop();
        var target = target(blackboard);
        var range = AtomicDawn.CONFIG.feralGhouls.WAKE_RANGE;
        if (target != null && agent.distanceToSqr(target) <= range * range) {
            agent.requestWake();
        }
        return ActionOutcome.running();
    }
}

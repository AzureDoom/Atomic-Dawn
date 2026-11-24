package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

final class FeralIdleAction extends FeralAction {

    FeralIdleAction() {
        super(0, true);
    }

    @Override
    public void start(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
    }

    @Override
    public ActionOutcome<Void> tick(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (agent.getRandom().nextInt(80) == 0) {
            agent.setYHeadRot(agent.getYHeadRot() + (agent.getRandom().nextFloat() - 0.5F) * 90F);
        }
        return ActionOutcome.running();
    }
}

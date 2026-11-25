package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

final class ScorchbeastIdleTask extends ScorchbeastTask {

    ScorchbeastIdleTask() {
        super(0, true);
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
        if (agent.flying()) {
            agent.hover();
        }
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (agent.flying()) {
            agent.hover();
        } else if (agent.getRandom().nextInt(120) == 0) {
            agent.setYHeadRot(agent.getYHeadRot() + (agent.getRandom().nextFloat() - 0.5F) * 60F);
        }
        return ActionOutcome.running();
    }
}

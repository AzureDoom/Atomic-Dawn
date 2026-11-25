package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedStance;

final class ScorchedDormantTask extends ScorchedTask {

    ScorchedDormantTask() {
        super(100, false);
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
        agent.setStance(ScorchedStance.NONE);
        state(blackboard, ScorchedCombatState.DORMANT);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (!agent.isDormant()) {
            return ActionOutcome.success();
        }
        agent.getNavigation().stop();
        return ActionOutcome.running();
    }
}

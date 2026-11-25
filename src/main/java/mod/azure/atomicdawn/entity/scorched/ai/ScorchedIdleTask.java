package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;

final class ScorchedIdleTask extends ScorchedTask {

    ScorchedIdleTask() {
        super(0, true);
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
        if (!agent.isDormant()) {
            agent.setStance(ScorchedWeapons.stance(agent, false));
            state(blackboard, ScorchedCombatState.IDLE);
        }
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (agent.getRandom().nextInt(80) == 0) {
            agent.setYHeadRot(agent.getYHeadRot() + (agent.getRandom().nextFloat() - 0.5F) * 90F);
        }
        return ActionOutcome.running();
    }
}

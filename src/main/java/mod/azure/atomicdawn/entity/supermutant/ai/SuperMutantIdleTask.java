package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantWeapons;

final class SuperMutantIdleTask extends SuperMutantTask {

    SuperMutantIdleTask() {
        super(0, true);
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
        agent.setStance(SuperMutantWeapons.stance(agent));
        state(blackboard, SuperMutantCombatState.IDLE);
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (agent.getRandom().nextInt(100) == 0) {
            agent.setYHeadRot(agent.getYHeadRot() + (agent.getRandom().nextFloat() - 0.5F) * 70F);
        }
        return ActionOutcome.running();
    }
}

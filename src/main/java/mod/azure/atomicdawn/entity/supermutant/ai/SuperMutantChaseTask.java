package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantWeapons;

final class SuperMutantChaseTask extends SuperMutantTask {

    private static final double RUN_SPEED = 1.2;

    private static final double CLOSE_SPEED = 0.95;

    private static final int REPATH_TICKS = 8;

    private int ticks;

    SuperMutantChaseTask() {
        super(40, true);
    }

    static boolean ready(SuperMutantEntity agent, Blackboard blackboard) {
        return hunting(blackboard) && SuperMutantWeapons.kind(agent) != SuperMutantWeapons.Kind.GUN;
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.setStance(SuperMutantWeapons.stance(agent));
        state(blackboard, SuperMutantCombatState.MELEE);
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null) {
            return ActionOutcome.success();
        }
        if (agent.busy()) {
            agent.getNavigation().stop();
            return ActionOutcome.running();
        }
        if (ticks++ % REPATH_TICKS != 0) {
            return ActionOutcome.running();
        }
        if (visible(blackboard)) {
            agent.getLookControl().setLookAt(target, 20F, 20F);
            var speed = agent.inReach(target, 2.5) ? CLOSE_SPEED : RUN_SPEED;
            agent.getNavigation().moveTo(target, speed);
            return ActionOutcome.running();
        }
        var last = blackboard.get(SuperMutantBlackboardKeys.LAST_KNOWN_POS);
        if (last != null) {
            agent.getNavigation().moveTo(last.getX() + 0.5, last.getY(), last.getZ() + 0.5, RUN_SPEED);
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

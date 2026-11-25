package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;

final class ScorchedChaseTask extends ScorchedTask {

    private static final double RUN_SPEED = 1.3;

    private static final int REPATH_TICKS = 6;

    private int ticks;

    ScorchedChaseTask() {
        super(40, true);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard) {
        return hunting(agent, blackboard) && ScorchedWeapons.kind(agent) != ScorchedWeapons.Kind.GUN;
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        blackboard.remove(ScorchedBlackboardKeys.NEW_TARGET);
        agent.setStance(ScorchedWeapons.stance(agent, false));
        state(blackboard, ScorchedCombatState.MELEE);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null) {
            return ActionOutcome.success();
        }
        agent.getLookControl().setLookAt(target, 30F, 30F);
        if (ticks++ % REPATH_TICKS == 0) {
            agent.getNavigation().moveTo(target, RUN_SPEED);
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

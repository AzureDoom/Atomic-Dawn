package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.scorched.ScorchedAction;
import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedHiveMind;
import mod.azure.atomicdawn.entity.scorched.ScorchedStance;

final class ScorchedAlertTask extends ScorchedTask {

    private static final int DURATION = 16;

    private static final int CALL_TICK = 6;

    private int ticks;

    ScorchedAlertTask() {
        super(90, false);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        return hunting(agent, blackboard) && visible(blackboard) && Boolean.TRUE.equals(
            blackboard.get(ScorchedBlackboardKeys.NEW_TARGET)
        ) && cooldowns.ready(ScorchedBrain.ALERT_COOLDOWN) && !agent.inMeleeRange(target(blackboard));
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        blackboard.remove(ScorchedBlackboardKeys.NEW_TARGET);
        cooldowns.set(ScorchedBrain.ALERT_COOLDOWN, 200 + agent.getRandom().nextInt(200));
        agent.getNavigation().stop();
        agent.setStance(ScorchedStance.NONE);
        agent.playAction(ScorchedAction.ALERT);
        agent.alertCry();
        state(blackboard, ScorchedCombatState.ALERTED);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null) {
            agent.getLookControl().setLookAt(target, 30F, 30F);
        }
        ticks++;
        if (ticks == CALL_TICK && target != null) {
            ScorchedHiveMind.broadcast(agent, target, true);
        }
        return ticks >= DURATION ? ActionOutcome.success() : ActionOutcome.running();
    }
}

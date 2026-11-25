package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantAction;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantPack;

final class SuperMutantRoarTask extends SuperMutantTask {

    private final boolean alert;

    private int ticks;

    SuperMutantRoarTask(boolean alert) {
        super(alert ? 90 : 52, false);
        this.alert = alert;
    }

    static boolean alertReady(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        return hunting(blackboard) && visible(blackboard) && Boolean.TRUE.equals(
            blackboard.get(SuperMutantBlackboardKeys.NEW_TARGET)
        ) && cooldowns.ready(SuperMutantBrain.ALERT_COOLDOWN) && !agent.inReach(target(blackboard), 1.0);
    }

    static boolean tauntReady(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        return target != null && visible(blackboard) && cooldowns.ready(SuperMutantBrain.TAUNT_COOLDOWN) && !agent
            .busy() && !agent.inReach(target, 3.0) && agent.distanceToSqr(target) < 30.0 * 30.0;
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        blackboard.remove(SuperMutantBlackboardKeys.NEW_TARGET);
        agent.getNavigation().stop();
        agent.playAction(action());
        if (alert) {
            cooldowns.set(SuperMutantBrain.ALERT_COOLDOWN, 200 + agent.getRandom().nextInt(200));
            agent.roar();
            state(blackboard, SuperMutantCombatState.ALERTED);
        } else {
            agent.taunt();
            state(blackboard, SuperMutantCombatState.TAUNTING);
        }
        var brain = agent.brain();
        if (brain != null) {
            cooldowns.set(SuperMutantBrain.TAUNT_COOLDOWN, brain.tauntDelay());
        }
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null) {
            agent.getLookControl().setLookAt(target, 30F, 30F);
        }
        ticks++;
        if (ticks == action().impactTick() && target != null) {
            SuperMutantPack.call(agent, target);
        }
        return ticks >= action().duration() ? ActionOutcome.success() : ActionOutcome.running();
    }

    private SuperMutantAction action() {
        return alert ? SuperMutantAction.ROAR : SuperMutantAction.TAUNT;
    }
}

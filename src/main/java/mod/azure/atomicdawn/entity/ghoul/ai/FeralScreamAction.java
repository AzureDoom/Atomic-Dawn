package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.ghoul.FeralGhoulAction;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

final class FeralScreamAction extends FeralAction {

    private static final int DURATION = 28;

    private static final int ALERT_TICK = 8;

    private int ticks;

    FeralScreamAction() {
        super(90, false);
    }

    @Override
    public void start(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        cooldowns.set(FeralGhoulBrain.SCREAM_COOLDOWN, FeralGhoulBrain.SCREAM_INTERVAL);
        agent.playAction(FeralGhoulAction.SCREAM);
    }

    @Override
    public ActionOutcome<Void> tick(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null) {
            agent.getLookControl().setLookAt(target, 30F, 30F);
        }
        ticks++;
        if (ticks == ALERT_TICK) {
            agent.screech();
            if (target != null) {
                agent.alertPack(target);
            }
        }
        return ticks >= DURATION ? ActionOutcome.success() : ActionOutcome.running();
    }
}

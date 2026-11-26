package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;

final class MisterHandyChaseTask extends MisterHandyTask {

    private static final double SPEED = 1.2;

    private int ticks;

    MisterHandyChaseTask() {
        super(40, true);
    }

    static boolean ready(Blackboard blackboard) {
        return hunting(blackboard);
    }

    @Override
    void begin(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
    }

    @Override
    public ActionOutcome<Void> tick(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null) {
            return ActionOutcome.success();
        }
        if (agent.busy()) {
            agent.getNavigation().stop();
            return ActionOutcome.running();
        }
        agent.getLookControl().setLookAt(target, 30F, 30F);
        if (agent.inReach(target, -0.2)) {
            agent.getNavigation().stop();
            agent.face(target);
            return ActionOutcome.running();
        }
        if (ticks++ % 8 == 0) {
            if (visible(blackboard)) {
                agent.getNavigation().moveTo(target, SPEED);
            } else {
                var last = blackboard.get(MisterHandyBlackboardKeys.LAST_KNOWN_POS);
                if (last != null) {
                    agent.getNavigation().moveTo(last.getX() + 0.5, last.getY(), last.getZ() + 0.5, SPEED);
                }
            }
        }
        return ActionOutcome.running();
    }

    @Override
    void end(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;

import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

final class FeralWanderAction extends FeralAction {

    private static final int MAX_TICKS = 160;

    private int ticks;

    FeralWanderAction() {
        super(10, true);
    }

    @Override
    public void start(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        var destination = DefaultRandomPos.getPos(agent, 10, 6);
        if (destination != null) {
            agent.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.7);
        }
    }

    @Override
    public ActionOutcome<Void> tick(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks++;
        if (agent.getNavigation().isDone() || ticks >= MAX_TICKS) {
            return ActionOutcome.success();
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
        cooldowns.set(FeralGhoulBrain.WANDER_COOLDOWN, 80 + agent.getRandom().nextInt(160));
    }
}

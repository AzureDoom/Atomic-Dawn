package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastFlight;

final class ScorchbeastTakeoffTask extends ScorchbeastTask {

    private static final int STUCK_LIMIT = 6;

    private static final double ELEVATION_LIMIT = 6.0;

    ScorchbeastTakeoffTask() {
        super(85, false);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (!agent.grounded() || !agent.onGround() || agent.wingCrippled() || agent.busy()) {
            return false;
        }
        if (!cooldowns.ready(ScorchbeastBrain.TAKEOFF_COOLDOWN)) {
            return false;
        }
        var target = target(blackboard);
        var random = agent.getRandom();
        if (target == null) {
            return random.nextInt(300) == 0;
        }
        var stuck = blackboard.get(ScorchbeastBlackboardKeys.GROUND_STUCK);
        if (stuck != null && stuck >= STUCK_LIMIT) {
            return true;
        }
        if (target.getY() - agent.getY() > ELEVATION_LIMIT) {
            return true;
        }
        if (
            horizontalDistance(
                agent,
                target
            ) > AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.flight.TAKEOFF_DISTANCE
        ) {
            return true;
        }
        return random.nextInt(100) == 0;
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
        agent.beginTakeoff();
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        return agent.flight() == ScorchbeastFlight.TAKEOFF ? ActionOutcome.running() : ActionOutcome.success();
    }
}

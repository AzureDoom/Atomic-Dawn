package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastFlight;

final class ScorchbeastLandTask extends ScorchbeastTask {

    private static final int ATTEMPTS = 24;

    private boolean started;

    ScorchbeastLandTask() {
        super(90, false);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (!agent.flying()) {
            return false;
        }
        if (agent.wingCrippled()) {
            return true;
        }
        if (!cooldowns.ready(ScorchbeastBrain.LAND_COOLDOWN)) {
            return false;
        }
        var target = target(blackboard);
        var random = agent.getRandom();
        if (target == null) {
            return random.nextInt(400) == 0;
        }
        return target.onGround() && random.nextInt(60) == 0;
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        started = false;
        if (agent.wingCrippled()) {
            agent.beginDescent(ScorchbeastLanding.below(agent), true);
            started = true;
            return;
        }
        var target = target(blackboard);
        var home = agent.home();
        var center = target != null ? target.position() : home != null ? Vec3.atCenterOf(home) : agent.position();
        var spot = ScorchbeastLanding.find(agent, center, target != null ? 6.0 : 0.0, 14.0, ATTEMPTS);
        if (spot == null) {
            cooldowns.set(ScorchbeastBrain.LAND_COOLDOWN, 80 + agent.getRandom().nextInt(80));
            return;
        }
        agent.beginDescent(spot, false);
        started = true;
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (!started) {
            return ActionOutcome.failed();
        }
        var flight = agent.flight();
        if (flight == ScorchbeastFlight.DESCENDING || flight == ScorchbeastFlight.LANDING) {
            return ActionOutcome.running();
        }
        return flight == ScorchbeastFlight.GROUNDED ? ActionOutcome.success() : ActionOutcome.failed();
    }
}

package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;

final class ScorchedEnterDormancyTask extends ScorchedTask {

    ScorchedEnterDormancyTask() {
        super(15, false);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard) {
        var brain = agent.brain();
        if (
            brain == null || agent.isDormant() || target(blackboard) != null || investigating(blackboard) || returning(
                blackboard
            ) || !agent.onGround()
        ) {
            return false;
        }
        var home = agent.home();
        var radius = agent.patrolRadius();
        return (home == null || agent.distanceToSqr(Vec3.atCenterOf(home)) <= radius * radius) && brain
            .readyForDormancy(
                agent.level().getGameTime()
            );
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.setDormant(true);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        return ActionOutcome.success();
    }
}

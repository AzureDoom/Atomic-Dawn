package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;

final class ScorchedAssistTask extends ScorchedTask {

    private static final double SPEED = 1.25;

    private static final double ARRIVE_SQR = 5.0 * 5.0;

    private int ticks;

    ScorchedAssistTask() {
        super(48, true);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard) {
        return hunting(agent, blackboard) && shared(blackboard) && !visible(blackboard) && blackboard.has(
            ScorchedBlackboardKeys.ALERT_ORIGIN
        );
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.setStance(ScorchedWeapons.stance(agent, false));
        state(blackboard, ScorchedCombatState.ASSISTING);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var origin = blackboard.get(ScorchedBlackboardKeys.ALERT_ORIGIN);
        if (origin == null || visible(blackboard) || target(blackboard) == null) {
            return ActionOutcome.success();
        }
        if (agent.distanceToSqr(Vec3.atCenterOf(origin)) <= ARRIVE_SQR) {
            return ActionOutcome.success();
        }
        if (
            ticks++ % 20 == 0 && !agent.getNavigation()
                .moveTo(
                    origin.getX() + 0.5,
                    origin.getY(),
                    origin.getZ()
                        + 0.5,
                    SPEED
                )
        ) {
            return ActionOutcome.failed();
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        blackboard.remove(ScorchedBlackboardKeys.ALERT_ORIGIN);
        agent.getNavigation().stop();
    }
}

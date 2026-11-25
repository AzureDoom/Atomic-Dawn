package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantWeapons;

final class SuperMutantAssistTask extends SuperMutantTask {

    private static final double SPEED = 1.15;

    private static final double ARRIVE_SQR = 5.0 * 5.0;

    private int ticks;

    SuperMutantAssistTask() {
        super(48, true);
    }

    static boolean ready(Blackboard blackboard) {
        return hunting(blackboard) && shared(blackboard) && !visible(blackboard) && blackboard.has(
            SuperMutantBlackboardKeys.ALERT_ORIGIN
        );
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.setStance(SuperMutantWeapons.stance(agent));
        state(blackboard, SuperMutantCombatState.ASSISTING);
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var origin = blackboard.get(SuperMutantBlackboardKeys.ALERT_ORIGIN);
        if (origin == null || visible(blackboard) || target(blackboard) == null) {
            return ActionOutcome.success();
        }
        if (agent.distanceToSqr(Vec3.atCenterOf(origin)) <= ARRIVE_SQR) {
            return ActionOutcome.success();
        }
        if (
            ticks++ % 20 == 0 && !agent.getNavigation()
                .moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, SPEED)
        ) {
            return ActionOutcome.failed();
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        blackboard.remove(SuperMutantBlackboardKeys.ALERT_ORIGIN);
        agent.getNavigation().stop();
    }
}

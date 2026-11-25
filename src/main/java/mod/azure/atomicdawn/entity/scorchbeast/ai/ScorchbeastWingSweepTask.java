package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastAction;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

final class ScorchbeastWingSweepTask extends ScorchbeastTask {

    private static final int IMPACT = 11;

    private static final int DURATION = 24;

    private static final float FACING_TOLERANCE = 50F;

    private boolean left;

    private int ticks;

    ScorchbeastWingSweepTask() {
        super(65, false);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null || !agent.grounded() || !agent.onGround() || !visible(blackboard)) {
            return false;
        }
        if (!attackReady(cooldowns, ScorchbeastBrain.SWEEP_COOLDOWN)) {
            return false;
        }
        var reach = AtomicDawn.CONFIG.scorchbeast.WING_SWEEP_RANGE + (agent.getBbWidth() + target.getBbWidth()) * 0.5;
        return horizontalDistance(agent, target) <= reach && Math.abs(target.getY() - agent.getY()) < agent
            .getBbHeight();
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        left = !Boolean.TRUE.equals(blackboard.get(ScorchbeastBlackboardKeys.LEFT_SWING));
        blackboard.set(ScorchbeastBlackboardKeys.LEFT_SWING, left);
        agent.playAction(left ? ScorchbeastAction.ATTACK_LEFT : ScorchbeastAction.ATTACK_RIGHT);
        agent.lockFor(DURATION);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null && ticks < IMPACT) {
            agent.faceTowards(target.position());
        }
        ticks++;
        if (ticks == IMPACT && agent.level() instanceof ServerLevel level) {
            agent.sweep(level, left);
        }
        return ticks >= DURATION ? ActionOutcome.success() : ActionOutcome.running();
    }

    @Override
    void end(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        cooldowns.set(ScorchbeastBrain.SWEEP_COOLDOWN, 10 + agent.getRandom().nextInt(16));
        cooldowns.set(ScorchbeastBrain.GLOBAL_COOLDOWN, 6);
    }
}

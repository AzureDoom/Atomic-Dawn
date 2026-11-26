package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;

import mod.azure.atomicdawn.entity.misterhandy.MisterHandyAction;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;

final class MisterHandyClawTask extends MisterHandyTask {

    private static final int IMPACT = 9;

    private static final double GRACE = 0.6;

    private int ticks;

    MisterHandyClawTask() {
        super(60, false);
    }

    static boolean ready(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        return target != null && visible(blackboard) && agent.inReach(target, 0.0) && attackReady(
            cooldowns,
            MisterHandyBrain.CLAW_COOLDOWN
        );
    }

    @Override
    void begin(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        agent.playAction(MisterHandyAction.CLAW);
    }

    @Override
    public ActionOutcome<Void> tick(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null && ticks < IMPACT) {
            agent.face(target);
        }
        ticks++;
        if (
            ticks == IMPACT && target != null && agent.inReach(target, GRACE)
                && agent.level() instanceof ServerLevel level
        ) {
            agent.clawHit(level, target);
        }
        return ticks >= MisterHandyAction.CLAW.duration() ? ActionOutcome.success() : ActionOutcome.running();
    }

    @Override
    void end(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        cooldowns.set(MisterHandyBrain.CLAW_COOLDOWN, 20 + agent.getRandom().nextInt(20));
        finishAttack(cooldowns);
    }
}

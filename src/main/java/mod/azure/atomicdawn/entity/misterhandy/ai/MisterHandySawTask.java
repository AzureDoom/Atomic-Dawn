package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;

import mod.azure.atomicdawn.entity.misterhandy.MisterHandyAction;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;

final class MisterHandySawTask extends MisterHandyTask {

    private static final int[] HITS = { 7, 10, 13, 16 };

    private static final double GRACE = 0.6;

    private int ticks;

    MisterHandySawTask() {
        super(65, false);
    }

    static boolean ready(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        return target != null && visible(blackboard) && !agent.sawDisabled() && agent.inReach(target, 0.0)
            && attackReady(cooldowns, MisterHandyBrain.SAW_COOLDOWN);
    }

    @Override
    void begin(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        agent.playAction(MisterHandyAction.SAW);
    }

    @Override
    public ActionOutcome<Void> tick(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null) {
            agent.face(target);
        }
        ticks++;
        for (var hit : HITS) {
            if (
                ticks == hit && target != null && agent.inReach(target, GRACE)
                    && agent.level() instanceof ServerLevel level
            ) {
                agent.sawHit(level, target);
            }
        }
        return ticks >= MisterHandyAction.SAW.duration() ? ActionOutcome.success() : ActionOutcome.running();
    }

    @Override
    void end(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        cooldowns.set(MisterHandyBrain.SAW_COOLDOWN, 30 + agent.getRandom().nextInt(25));
        finishAttack(cooldowns);
    }
}

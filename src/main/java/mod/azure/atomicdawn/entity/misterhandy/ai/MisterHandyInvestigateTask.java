package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;
import mod.azure.atomicdawn.registry.ADSounds;

final class MisterHandyInvestigateTask extends MisterHandyTask {

    private static final double SPEED = 1.0;

    private static final double ARRIVE_SQR = 2.5 * 2.5;

    private boolean arrived;

    MisterHandyInvestigateTask() {
        super(25, true);
    }

    static boolean ready(Blackboard blackboard) {
        return !hunting(blackboard) && blackboard.has(MisterHandyBlackboardKeys.INVESTIGATE_UNTIL) && blackboard.has(
            MisterHandyBlackboardKeys.LAST_KNOWN_POS
        );
    }

    @Override
    void begin(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        arrived = false;
        var pos = blackboard.get(MisterHandyBlackboardKeys.LAST_KNOWN_POS);
        if (pos != null) {
            agent.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SPEED);
        }
    }

    @Override
    public ActionOutcome<Void> tick(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var pos = blackboard.get(MisterHandyBlackboardKeys.LAST_KNOWN_POS);
        if (pos == null || !blackboard.has(MisterHandyBlackboardKeys.INVESTIGATE_UNTIL)) {
            return ActionOutcome.success();
        }
        if (!arrived && (agent.distanceToSqr(Vec3.atCenterOf(pos)) <= ARRIVE_SQR || agent.getNavigation().isDone())) {
            arrived = true;
            agent.getNavigation().stop();
            agent.setScanning(true);
            agent.playSound(ADSounds.MISTER_HANDY_SCAN.get(), 0.8F, 1.0F);
        }
        if (arrived && agent.getRandom().nextInt(25) == 0) {
            agent.setYHeadRot(agent.getYHeadRot() + (agent.getRandom().nextFloat() - 0.5F) * 120F);
        }
        return ActionOutcome.running();
    }

    @Override
    void end(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
        agent.setScanning(false);
    }
}

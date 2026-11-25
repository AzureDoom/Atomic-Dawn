package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;

final class ScorchedInvestigateTask extends ScorchedTask {

    private static final double SPEED = 1.0;

    private static final double ARRIVE_SQR = 2.5 * 2.5;

    private boolean arrived;

    private int searchTicks;

    ScorchedInvestigateTask() {
        super(25, true);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard) {
        return !agent.isDormant() && target(blackboard) == null && investigating(blackboard);
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        arrived = false;
        searchTicks = 0;
        agent.setStance(ScorchedWeapons.stance(agent, false));
        state(blackboard, ScorchedCombatState.INVESTIGATING);
        var pos = blackboard.get(ScorchedBlackboardKeys.LAST_KNOWN_POS);
        if (pos != null) {
            agent.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SPEED);
        }
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var pos = blackboard.get(ScorchedBlackboardKeys.LAST_KNOWN_POS);
        if (pos == null || !investigating(blackboard)) {
            return ActionOutcome.success();
        }
        if (!arrived) {
            if (agent.distanceToSqr(Vec3.atCenterOf(pos)) <= ARRIVE_SQR || agent.getNavigation().isDone()) {
                arrived = true;
                agent.getNavigation().stop();
            }
            return ActionOutcome.running();
        }
        var random = agent.getRandom();
        if (random.nextInt(25) == 0) {
            agent.setYHeadRot(agent.getYHeadRot() + (random.nextFloat() - 0.5F) * 120F);
        }
        if (++searchTicks % 60 == 0 && agent.getNavigation().isDone()) {
            var next = DefaultRandomPos.getPos(agent, 6, 3);
            if (next != null) {
                agent.getNavigation().moveTo(next.x, next.y, next.z, 0.8);
            }
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

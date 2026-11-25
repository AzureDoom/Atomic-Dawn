package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantWeapons;

final class SuperMutantInvestigateTask extends SuperMutantTask {

    private static final double SPEED = 0.95;

    private static final double ARRIVE_SQR = 3.0 * 3.0;

    @Nullable
    private BlockPos heading;

    private boolean arrived;

    private int searchTicks;

    SuperMutantInvestigateTask() {
        super(25, true);
    }

    static boolean ready(Blackboard blackboard) {
        return !hunting(blackboard) && investigating(blackboard);
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        heading = null;
        arrived = false;
        searchTicks = 0;
        agent.setStance(SuperMutantWeapons.stance(agent));
        state(blackboard, SuperMutantCombatState.INVESTIGATING);
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var pos = blackboard.get(SuperMutantBlackboardKeys.LAST_KNOWN_POS);
        if (pos == null || !investigating(blackboard)) {
            return ActionOutcome.success();
        }
        if (!pos.equals(heading)) {
            heading = pos;
            arrived = false;
            agent.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SPEED);
        }
        if (!arrived) {
            agent.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
            if (agent.distanceToSqr(Vec3.atCenterOf(pos)) <= ARRIVE_SQR || agent.getNavigation().isDone()) {
                arrived = true;
                agent.getNavigation().stop();
            }
            return ActionOutcome.running();
        }
        var random = agent.getRandom();
        if (random.nextInt(30) == 0) {
            agent.setYHeadRot(agent.getYHeadRot() + (random.nextFloat() - 0.5F) * 120F);
        }
        if (++searchTicks % 70 == 0 && agent.getNavigation().isDone()) {
            var next = DefaultRandomPos.getPos(agent, 7, 3);
            if (next != null) {
                agent.getNavigation().moveTo(next.x, next.y, next.z, 0.7);
            }
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantWeapons;

final class SuperMutantPatrolTask extends SuperMutantTask {

    private static final int MAX_TICKS = 300;

    private static final double SPEED = 0.7;

    private int ticks;

    SuperMutantPatrolTask() {
        super(10, true);
    }

    static boolean ready(Blackboard blackboard, CooldownTracker cooldowns) {
        return !hunting(blackboard) && !investigating(blackboard) && !returning(blackboard) && cooldowns.ready(
            SuperMutantBrain.PATROL_COOLDOWN
        );
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.setStance(SuperMutantWeapons.stance(agent));
        state(blackboard, SuperMutantCombatState.PATROLLING);
        var destination = destination(agent);
        if (destination != null) {
            agent.getNavigation().moveTo(destination.x, destination.y, destination.z, SPEED);
        }
    }

    @Nullable
    private static Vec3 destination(SuperMutantEntity agent) {
        var home = agent.home();
        var random = agent.getRandom();
        if (home == null) {
            return DefaultRandomPos.getPos(agent, 10, 5);
        }
        var angle = random.nextFloat() * Mth.TWO_PI;
        var distance = (0.3 + random.nextDouble() * 0.7) * agent.patrolRadius();
        var goal = new Vec3(
            home.getX() + 0.5 + Mth.cos(angle) * distance,
            agent.getY(),
            home.getZ() + 0.5 + Mth.sin(angle) * distance
        );
        var step = DefaultRandomPos.getPosTowards(agent, 14, 6, goal, Math.PI / 2);
        if (step == null) {
            return null;
        }
        var radius = agent.patrolRadius() + 4.0;
        return step.distanceToSqr(Vec3.atBottomCenterOf(home)) <= radius * radius ? step : null;
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (agent.getNavigation().isDone() || ++ticks >= MAX_TICKS) {
            return ActionOutcome.success();
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
        cooldowns.set(SuperMutantBrain.PATROL_COOLDOWN, 120 + agent.getRandom().nextInt(240));
    }
}

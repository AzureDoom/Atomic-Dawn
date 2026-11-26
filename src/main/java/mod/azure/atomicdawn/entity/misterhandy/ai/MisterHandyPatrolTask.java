package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;

final class MisterHandyPatrolTask extends MisterHandyTask {

    private static final double SPEED = 0.8;

    private static final int MAX_TICKS = 240;

    private int ticks;

    MisterHandyPatrolTask() {
        super(10, true);
    }

    static boolean ready(Blackboard blackboard, CooldownTracker cooldowns) {
        return !hunting(blackboard) && !blackboard.has(MisterHandyBlackboardKeys.INVESTIGATE_UNTIL)
            && cooldowns.ready(MisterHandyBrain.PATROL_COOLDOWN);
    }

    @Override
    void begin(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        var home = agent.home();
        var random = agent.getRandom();
        Vec3 goal;
        if (home == null) {
            goal = DefaultRandomPos.getPos(agent, 10, 4);
        } else {
            var radius = AtomicDawn.CONFIG.server.entities.robots.misterHandy.behavior.PATROL_RADIUS;
            var angle = random.nextFloat() * Mth.TWO_PI;
            var distance = random.nextDouble() * radius;
            var point = new Vec3(
                home.getX() + 0.5 + Mth.cos(angle) * distance,
                agent.getY(),
                home.getZ() + 0.5 + Mth.sin(angle) * distance
            );
            goal = DefaultRandomPos.getPosTowards(agent, 12, 4, point, Math.PI / 2);
        }
        if (goal != null) {
            agent.getNavigation().moveTo(goal.x, goal.y, goal.z, SPEED);
        }
    }

    @Override
    public ActionOutcome<Void> tick(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        return agent.getNavigation().isDone() || ++ticks >= MAX_TICKS
            ? ActionOutcome.success()
            : ActionOutcome
                .running();
    }

    @Override
    void end(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
        cooldowns.set(MisterHandyBrain.PATROL_COOLDOWN, 100 + agent.getRandom().nextInt(200));
    }
}

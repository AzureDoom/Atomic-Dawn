package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

final class ScorchbeastPatrolTask extends ScorchbeastTask {

    private static final double RADIUS = 28.0;

    private static final double SPEED = 0.45;

    private double angle;

    private int direction = 1;

    ScorchbeastPatrolTask() {
        super(10, true);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard) {
        return !hunting(blackboard) && agent.flying();
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var center = center(agent);
        angle = Math.atan2(agent.getZ() - center.z, agent.getX() - center.x);
        direction = agent.getRandom().nextBoolean() ? 1 : -1;
    }

    private static Vec3 center(ScorchbeastEntity agent) {
        var home = agent.home();
        return home != null ? Vec3.atCenterOf(home) : agent.position();
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (!agent.flying() || hunting(blackboard)) {
            return ActionOutcome.success();
        }
        var center = center(agent);
        angle += direction * SPEED / RADIUS;
        var x = center.x + Math.cos(angle) * RADIUS;
        var z = center.z + Math.sin(angle) * RADIUS;
        var altitude = agent.groundY(x, z) + AtomicDawn.CONFIG.scorchbeast.CRUISE_ALTITUDE + 6.0 + Mth.sin(
            agent.tickCount * 0.02F
        ) * 3.0;
        agent.flyTo(new Vec3(x, altitude, z), SPEED);
        return ActionOutcome.running();
    }
}

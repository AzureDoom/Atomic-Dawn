package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;

final class ScorchbeastCircleTask extends ScorchbeastTask {

    private static final double SPEED = 0.6;

    private double angle;

    private int direction = 1;

    private int switchTicks;

    ScorchbeastCircleTask() {
        super(40, true);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard) {
        return hunting(blackboard) && agent.flying();
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null) {
            angle = Math.atan2(agent.getZ() - target.getZ(), agent.getX() - target.getX());
        }
        direction = agent.getRandom().nextBoolean() ? 1 : -1;
        switchTicks = 160 + agent.getRandom().nextInt(160);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null || !agent.flying()) {
            return ActionOutcome.success();
        }
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.flight;
        if (--switchTicks <= 0) {
            direction = -direction;
            switchTicks = 160 + agent.getRandom().nextInt(160);
        }
        var radius = config.CIRCLE_RADIUS + Mth.sin(agent.tickCount * 0.03F) * 4.0;
        angle += direction * SPEED / Math.max(4.0, radius);
        var x = target.getX() + Math.cos(angle) * radius;
        var z = target.getZ() + Math.sin(angle) * radius;
        var altitude = Math.max(
            target.getY() + config.CRUISE_ALTITUDE,
            agent.groundY(x, z) + config.CRUISE_ALTITUDE * 0.75
        );
        agent.flyTo(new Vec3(x, altitude, z), SPEED);
        if (visible(blackboard) && horizontalDistance(agent, target) < radius * 1.5) {
            agent.getLookControl().setLookAt(target, 20F, 20F);
        }
        return ActionOutcome.running();
    }
}

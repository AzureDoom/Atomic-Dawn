package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

final class FeralChaseAction extends FeralAction {

    private static final double RUN_SPEED = 1.35;

    private static final double DIRECT_RANGE = 3.5;

    private static final int DIRECT_REPATH_TICKS = 4;

    private static final int ZIG_REPATH_TICKS = 5;

    private int ticks;

    private int swerveTicks;

    private float side;

    private boolean straight;

    private double speed;

    FeralChaseAction() {
        super(40, true);
    }

    @Override
    public void start(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        swerveTicks = 0;
        side = agent.getRandom().nextBoolean() ? 1F : -1F;
        straight = false;
        speed = RUN_SPEED;
    }

    @Override
    public ActionOutcome<Void> tick(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null) {
            return ActionOutcome.success();
        }
        agent.getLookControl().setLookAt(target, 30F, 30F);
        ticks++;
        var distance = agent.distanceTo(target);
        if (distance <= DIRECT_RANGE) {
            if (ticks % DIRECT_REPATH_TICKS == 0) {
                agent.getNavigation().moveTo(target, RUN_SPEED);
            }
            return ActionOutcome.running();
        }
        var repath = false;
        if (--swerveTicks <= 0) {
            var random = agent.getRandom();
            straight = random.nextFloat() < 0.25F;
            side = -side;
            swerveTicks = 8 + random.nextInt(11);
            speed = RUN_SPEED * (0.9 + random.nextDouble() * 0.2);
            repath = true;
        }
        if (repath || ticks % ZIG_REPATH_TICKS == 0) {
            zig(agent, target, distance);
        }
        return ActionOutcome.running();
    }

    private void zig(FeralGhoulEntity agent, LivingEntity target, double distance) {
        var dx = target.getX() - agent.getX();
        var dz = target.getZ() - agent.getZ();
        var length = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        var dirX = dx / length;
        var dirZ = dz / length;
        var ahead = Math.min(distance - 1.0, 4.0 + agent.getRandom().nextDouble() * 2.0);
        var lateral = straight ? 0.0 : side * Math.min(3.0, 1.2 + distance * 0.15);
        var x = agent.getX() + dirX * ahead - dirZ * lateral;
        var z = agent.getZ() + dirZ * ahead + dirX * lateral;
        var y = Mth.lerp(ahead / Math.max(1.0, distance), agent.getY(), target.getY());
        if (!agent.getNavigation().moveTo(x, y, z, speed)) {
            agent.getNavigation().moveTo(target, speed);
        }
    }

    @Override
    public void stop(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }
}

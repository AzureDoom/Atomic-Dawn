package mod.azure.atomicdawn.entity.ghoul.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulAction;
import mod.azure.atomicdawn.entity.ghoul.FeralGhoulEntity;

final class FeralLeapAction extends FeralAction {

    private static final int LAUNCH_TICK = 6;

    private static final int MAX_TICKS = 40;

    private int ticks;

    private boolean landedHit;

    FeralLeapAction() {
        super(55, false);
    }

    static boolean canLeap(FeralGhoulEntity agent, LivingEntity target) {
        if (!AtomicDawn.CONFIG.feralGhouls.LEAP_ENABLED || !agent.onGround()) {
            return false;
        }
        var distance = agent.distanceTo(target);
        return distance >= 3.5F && distance <= 7.5F && Math.abs(target.getY() - agent.getY()) < 2.0 && agent
            .hasLineOfSight(target);
    }

    @Override
    public void start(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        landedHit = false;
        agent.getNavigation().stop();
        cooldowns.set(FeralGhoulBrain.LEAP_COOLDOWN, FeralGhoulBrain.LEAP_INTERVAL + agent.getRandom().nextInt(40));
        agent.playAction(FeralGhoulAction.LEAP);
    }

    @Override
    public ActionOutcome<Void> tick(FeralGhoulEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        ticks++;
        if (target != null && ticks <= LAUNCH_TICK) {
            agent.getLookControl().setLookAt(target, 30F, 30F);
        }
        if (ticks == LAUNCH_TICK) {
            if (target == null) {
                return ActionOutcome.failed();
            }
            var horizontal = new Vec3(target.getX() - agent.getX(), 0, target.getZ() - agent.getZ());
            var distance = horizontal.length();
            var push = horizontal.normalize().scale(Math.min(1.1, 0.25 + distance * 0.13));
            agent.setDeltaMovement(push.x, 0.42, push.z);
            agent.setYRot((float) (Math.atan2(horizontal.z, horizontal.x) * (180.0 / Math.PI)) - 90F);
            agent.setYBodyRot(agent.getYRot());
            agent.attackCry();
            agent.needsSync = true;
        }
        if (
            ticks > LAUNCH_TICK && !landedHit && target != null && agent.level() instanceof ServerLevel level && agent
                .getBoundingBox()
                .inflate(0.35)
                .intersects(target.getBoundingBox())
        ) {
            landedHit = true;
            agent.doHurtTarget(level, target);
            target.knockback(
                0.6,
                agent.getX() - target.getX(),
                agent.getZ() - target.getZ(),
                agent.damageSources().mobAttack(agent),
                0.0F
            );
        }
        if (ticks > LAUNCH_TICK + 4 && agent.onGround()) {
            return ActionOutcome.success();
        }
        return ticks >= MAX_TICKS ? ActionOutcome.success() : ActionOutcome.running();
    }
}

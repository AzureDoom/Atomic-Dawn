package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastFlight;
import mod.azure.atomicdawn.registry.ADSounds;

final class ScorchbeastLungeTask extends ScorchbeastTask {

    private static final int WINDUP = 10;

    private static final int TURN_TICKS = 16;

    private static final int MAX_TICKS = 100;

    private static final double MIN_RANGE = 7.0;

    private static final double OVERSHOOT = 5.0;

    private int ticks;

    private int landedAt;

    private boolean launched;

    private boolean struck;

    ScorchbeastLungeTask() {
        super(50, false);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null || !agent.grounded() || !agent.onGround() || !visible(blackboard)) {
            return false;
        }
        var distance = horizontalDistance(agent, target);
        return attackReady(cooldowns, ScorchbeastBrain.LUNGE_COOLDOWN) && distance >= MIN_RANGE
            && distance <= AtomicDawn.CONFIG.scorchbeast.LUNGE_RANGE;
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        landedAt = -1;
        launched = false;
        struck = false;
        agent.getNavigation().stop();
        agent.playSound(ADSounds.SCORCHBEAST_SCREECH.get(), 3F, agent.getVoicePitch());
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        ticks++;
        if (ticks > MAX_TICKS) {
            return ActionOutcome.success();
        }
        if (!launched) {
            if (target == null) {
                return ActionOutcome.success();
            }
            agent.faceTowards(target.position());
            if (ticks >= WINDUP) {
                var flat = new Vec3(target.getX() - agent.getX(), 0, target.getZ() - agent.getZ());
                var distance = flat.length() + OVERSHOOT;
                var direction = flat.normalize();
                var horizontal = Math.min(1.8, distance * 0.075);
                launched = agent.beginLunge(new Vec3(direction.x * horizontal, 0.6, direction.z * horizontal));
                if (!launched) {
                    return ActionOutcome.success();
                }
            }
            return ActionOutcome.running();
        }
        if (agent.flight() == ScorchbeastFlight.LUNGING) {
            if (
                !struck && target != null && agent.contactBox().intersects(target.getBoundingBox())
                    && agent.level() instanceof ServerLevel level
            ) {
                agent.lungeContact(level, target);
                struck = true;
            }
            return ActionOutcome.running();
        }
        if (landedAt < 0) {
            landedAt = ticks;
        }
        if (target != null) {
            agent.faceTowards(target.position());
            if (agent.facing(target.position(), 15F)) {
                return ActionOutcome.success();
            }
        }
        return ticks - landedAt >= TURN_TICKS ? ActionOutcome.success() : ActionOutcome.running();
    }

    @Override
    void end(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        cooldowns.set(
            ScorchbeastBrain.LUNGE_COOLDOWN,
            AtomicDawn.CONFIG.scorchbeast.LUNGE_COOLDOWN_TICKS + agent.getRandom().nextInt(60)
        );
        finishAttack(agent, cooldowns);
    }
}

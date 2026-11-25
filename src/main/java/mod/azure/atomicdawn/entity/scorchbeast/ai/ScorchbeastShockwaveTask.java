package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastAction;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastSummons;
import mod.azure.atomicdawn.registry.ADSounds;

final class ScorchbeastShockwaveTask extends ScorchbeastTask {

    private static final int IMPACT = 16;

    private static final int DURATION = 52;

    private int ticks;

    ScorchbeastShockwaveTask() {
        super(70, false);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null || !agent.grounded() || !agent.onGround()) {
            return false;
        }
        if (!attackReady(cooldowns, ScorchbeastBrain.SHOCKWAVE_COOLDOWN)) {
            return false;
        }
        return agent.distanceTo(target) <= AtomicDawn.CONFIG.scorchbeast.SHOCKWAVE_RADIUS + 4.0;
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        agent.playAction(ScorchbeastAction.SCREAM);
        agent.playSound(ADSounds.SCORCHBEAST_SCREAM.get(), 4F, agent.getVoicePitch() * 0.9F);
        agent.lockFor(DURATION);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null && ticks < IMPACT) {
            agent.faceTowards(target.position());
        }
        ticks++;
        if (ticks == IMPACT && agent.level() instanceof ServerLevel level) {
            agent.shockwave(level);
            ScorchbeastSummons.summon(level, agent);
        }
        return ticks >= DURATION ? ActionOutcome.success() : ActionOutcome.running();
    }

    @Override
    void end(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        cooldowns.set(
            ScorchbeastBrain.SHOCKWAVE_COOLDOWN,
            AtomicDawn.CONFIG.scorchbeast.SHOCKWAVE_COOLDOWN_TICKS + agent.getRandom().nextInt(100)
        );
        finishAttack(agent, cooldowns);
    }
}

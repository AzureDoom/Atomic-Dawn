package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastAction;
import mod.azure.atomicdawn.entity.scorchbeast.ScorchbeastEntity;
import mod.azure.atomicdawn.entity.scorchbeast.SonicBlastEntity;
import mod.azure.atomicdawn.registry.ADSounds;

final class ScorchbeastSonicTask extends ScorchbeastTask {

    private static final int[] SHOTS = { 12, 20, 28 };

    private static final int DURATION = 52;

    private static final int MIN_COOLDOWN = 200;

    private static final double MIN_RANGE = 8.0;

    private int ticks;

    ScorchbeastSonicTask() {
        super(60, false);
    }

    static boolean ready(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null || !agent.flying() || !visible(blackboard) || agent.wingCrippled()) {
            return false;
        }
        if (!attackReady(cooldowns, ScorchbeastBrain.SONIC_COOLDOWN)) {
            return false;
        }
        var distance = agent.distanceTo(target);
        return distance >= MIN_RANGE
            && distance <= AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.sonicAttack.SONIC_RANGE;
    }

    @Override
    void begin(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.hover();
        agent.playAction(ScorchbeastAction.FLYING_SCREAM);
        agent.playSound(ADSounds.SCORCHBEAST_SCREAM.get(), 4F, agent.getVoicePitch());
        agent.lockFor(DURATION);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        agent.hover();
        if (target != null) {
            agent.faceTowards(target.position());
            agent.getLookControl().setLookAt(target, 30F, 30F);
        }
        ticks++;
        for (var shot : SHOTS) {
            if (ticks == shot && target != null && agent.level() instanceof ServerLevel level) {
                fire(level, agent, target);
            }
        }
        return ticks >= DURATION ? ActionOutcome.success() : ActionOutcome.running();
    }

    private static void fire(ServerLevel level, ScorchbeastEntity agent, LivingEntity target) {
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.sonicAttack;
        var origin = agent.mouth();
        var aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
        var travel = aim.distanceTo(origin) / config.SONIC_SPEED;
        var lead = target.getDeltaMovement().multiply(1, 0, 1).scale(travel * 0.6);
        var random = agent.getRandom();
        var spread = config.SONIC_SPREAD;
        var direction = aim.add(lead)
            .add(random.nextGaussian() * spread, random.nextGaussian() * spread * 0.5, random.nextGaussian() * spread)
            .subtract(origin)
            .normalize();
        level.addFreshEntity(SonicBlastEntity.create(level, agent, origin, direction.scale(config.SONIC_SPEED)));
        agent.playSound(ADSounds.SCORCHBEAST_SONIC.get(), 4F, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    void end(ScorchbeastEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        var config = AtomicDawn.CONFIG.server.entities.bosses.scorchbeast.sonicAttack;
        var cooldown = Math.max(MIN_COOLDOWN, config.SONIC_COOLDOWN_TICKS);
        cooldowns.set(ScorchbeastBrain.SONIC_COOLDOWN, cooldown + agent.getRandom().nextInt(80));
        finishAttack(agent, cooldowns);
    }
}

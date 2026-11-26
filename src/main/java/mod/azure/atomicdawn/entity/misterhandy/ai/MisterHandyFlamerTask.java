package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyAction;
import mod.azure.atomicdawn.entity.misterhandy.MisterHandyEntity;
import mod.azure.atomicdawn.registry.ADSounds;

final class MisterHandyFlamerTask extends MisterHandyTask {

    private static final int FLAME_START = 7;

    private static final int FLAME_END = 33;

    private static final double MIN_RANGE = 1.8;

    private int ticks;

    MisterHandyFlamerTask() {
        super(55, false);
    }

    static boolean ready(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null || !visible(blackboard) || agent.flamerDisabled()) {
            return false;
        }
        var distance = agent.distanceTo(target);
        return distance >= MIN_RANGE
            && distance <= AtomicDawn.CONFIG.server.entities.robots.misterHandy.combat.FLAMER_RANGE
            && attackReady(cooldowns, MisterHandyBrain.FLAMER_COOLDOWN);
    }

    @Override
    void begin(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        agent.playAction(MisterHandyAction.FLAMER);
    }

    @Override
    public ActionOutcome<Void> tick(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null) {
            agent.face(target);
        }
        ticks++;
        if (ticks >= FLAME_START && ticks <= FLAME_END && agent.level() instanceof ServerLevel level) {
            if (ticks == FLAME_START || (ticks - FLAME_START) % 12 == 0) {
                agent.playSound(ADSounds.MISTER_HANDY_FLAMER.get(), 1.2F, 0.9F + agent.getRandom().nextFloat() * 0.2F);
            }
            agent.flame(level, target, ticks % 4 == 0);
        }
        return ticks >= MisterHandyAction.FLAMER.duration() ? ActionOutcome.success() : ActionOutcome.running();
    }

    @Override
    void end(MisterHandyEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        cooldowns.set(
            MisterHandyBrain.FLAMER_COOLDOWN,
            AtomicDawn.CONFIG.server.entities.robots.misterHandy.combat.FLAMER_COOLDOWN_TICKS + agent.getRandom()
                .nextInt(60)
        );
        finishAttack(cooldowns);
    }
}

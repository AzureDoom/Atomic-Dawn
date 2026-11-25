package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;

import mod.azure.atomicdawn.entity.scorched.ScorchedAction;
import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;

final class ScorchedMeleeAttackTask extends ScorchedTask {

    private static final double BASH_RANGE_SQR = 2.4 * 2.4;

    private int ticks;

    private int windup;

    private int duration;

    private boolean left;

    ScorchedMeleeAttackTask() {
        super(60, false);
    }

    static boolean ready(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (
            !hunting(agent, blackboard) || !visible(blackboard) || !cooldowns.ready(ScorchedBrain.MELEE_COOLDOWN)
                || !agent.inMeleeRange(target)
        ) {
            return false;
        }
        return ScorchedWeapons.kind(agent) != ScorchedWeapons.Kind.GUN || agent.distanceToSqr(target) <= BASH_RANGE_SQR;
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        blackboard.remove(ScorchedBlackboardKeys.NEW_TARGET);
        state(blackboard, ScorchedCombatState.MELEE);
        switch (ScorchedWeapons.kind(agent)) {
            case FISTS -> {
                left = !left;
                windup = 5;
                duration = 9;
                agent.playAction(left ? ScorchedAction.PUNCH_LEFT : ScorchedAction.PUNCH_RIGHT);
            }
            case MELEE -> {
                windup = 7;
                duration = 12;
                agent.playAction(ScorchedAction.SWING);
            }
            case GUN -> {
                windup = 6;
                duration = 10;
                agent.playAction(ScorchedAction.BASH);
            }
        }
        agent.attackCry();
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target == null) {
            cooldowns.set(ScorchedBrain.MELEE_COOLDOWN, 10);
            return ActionOutcome.failed();
        }
        agent.getLookControl().setLookAt(target, 30F, 30F);
        ticks++;
        if (ticks == windup) {
            cooldowns.set(ScorchedBrain.MELEE_COOLDOWN, duration + 4 + agent.getRandom().nextInt(6));
            if (
                agent.level() instanceof ServerLevel level && agent.inMeleeRange(target) && agent.getSensing()
                    .hasLineOfSight(target)
            ) {
                agent.meleeHit(level, target);
            }
        }
        return ticks >= duration ? ActionOutcome.success() : ActionOutcome.running();
    }
}

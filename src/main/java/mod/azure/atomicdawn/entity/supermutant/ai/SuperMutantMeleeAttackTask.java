package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.server.level.ServerLevel;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantAction;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantWeapons;

final class SuperMutantMeleeAttackTask extends SuperMutantTask {

    private static final double GUN_BASH_EXTRA = -0.4;

    private static final double SWING_GRACE = 0.8;

    private SuperMutantAction attack = SuperMutantAction.PUNCH_RIGHT;

    private int ticks;

    private boolean left;

    SuperMutantMeleeAttackTask() {
        super(60, false);
    }

    static boolean ready(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (!hunting(blackboard) || !visible(blackboard) || !cooldowns.ready(SuperMutantBrain.MELEE_COOLDOWN)) {
            return false;
        }
        var gun = SuperMutantWeapons.kind(agent) == SuperMutantWeapons.Kind.GUN;
        return agent.inReach(target, gun ? GUN_BASH_EXTRA : 0.0);
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        blackboard.remove(SuperMutantBlackboardKeys.NEW_TARGET);
        state(blackboard, SuperMutantCombatState.MELEE);
        attack = choose(agent, cooldowns);
        agent.playAction(attack);
        agent.attackGrunt();
    }

    private SuperMutantAction choose(SuperMutantEntity agent, CooldownTracker cooldowns) {
        var kind = SuperMutantWeapons.kind(agent);
        var config = AtomicDawn.CONFIG.server.entities.mutants.superMutants.combat;
        if (
            kind != SuperMutantWeapons.Kind.GUN && cooldowns.ready(SuperMutantBrain.SLAM_COOLDOWN) && agent.getRandom()
                .nextDouble() < config.SLAM_CHANCE
        ) {
            cooldowns.set(SuperMutantBrain.SLAM_COOLDOWN, 100 + agent.getRandom().nextInt(80));
            return SuperMutantAction.SLAM;
        }
        return switch (kind) {
            case MELEE -> SuperMutantAction.PUNCH_RIGHT;
            case GUN -> SuperMutantAction.PUNCH_LEFT;
            case FISTS -> {
                left = !left;
                yield left ? SuperMutantAction.PUNCH_LEFT : SuperMutantAction.PUNCH_RIGHT;
            }
        };
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = target(blackboard);
        if (target != null && ticks < attack.impactTick()) {
            agent.getLookControl().setLookAt(target, 30F, 30F);
            agent.setYRot(agent.getYHeadRot());
            agent.yBodyRot = agent.getYHeadRot();
        }
        ticks++;
        if (ticks == attack.impactTick() && agent.level() instanceof ServerLevel level) {
            if (attack == SuperMutantAction.SLAM) {
                agent.slam(level);
            } else if (
                target != null && agent.inReach(target, SWING_GRACE) && agent.getSensing().hasLineOfSight(target)
            ) {
                agent.meleeHit(level, target);
            }
        }
        if (ticks >= attack.duration()) {
            var rest = attack == SuperMutantAction.SLAM ? 6 : 4 + agent.getRandom().nextInt(8);
            cooldowns.set(SuperMutantBrain.MELEE_COOLDOWN, rest);
            return ActionOutcome.success();
        }
        return ActionOutcome.running();
    }
}

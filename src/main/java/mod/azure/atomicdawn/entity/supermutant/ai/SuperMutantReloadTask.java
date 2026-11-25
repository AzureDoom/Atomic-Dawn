package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.util.Mth;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantAction;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantWeapons;
import mod.azure.atomicdawn.weapon.combat.MobGunfire;

final class SuperMutantReloadTask extends SuperMutantTask {

    private int ticks;

    private int duration;

    SuperMutantReloadTask() {
        super(58, false);
    }

    static boolean ready(SuperMutantEntity agent) {
        if (SuperMutantWeapons.kind(agent) != SuperMutantWeapons.Kind.GUN) {
            return false;
        }
        var definition = SuperMutantWeapons.definition(agent);
        return definition != null && MobGunfire.needsReload(agent.getMainHandItem(), definition);
    }

    @Override
    public void start(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        var definition = SuperMutantWeapons.definition(agent);
        if (definition == null) {
            duration = 0;
            return;
        }
        duration = Mth.clamp(MobGunfire.reloadTicks(agent.getMainHandItem(), definition), 20, 100);
        agent.getNavigation().stop();
        agent.playAction(SuperMutantWeapons.reloadAction(definition));
        MobGunfire.startReload(agent, agent.getMainHandItem(), definition);
        state(blackboard, SuperMutantCombatState.RELOADING);
    }

    @Override
    public ActionOutcome<Void> tick(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var definition = SuperMutantWeapons.definition(agent);
        if (definition == null || SuperMutantWeapons.kind(agent) != SuperMutantWeapons.Kind.GUN) {
            return ActionOutcome.failed();
        }
        var target = target(blackboard);
        if (target != null) {
            agent.getLookControl().setLookAt(target, 30F, 30F);
        }
        if (++ticks < duration) {
            return ActionOutcome.running();
        }
        MobGunfire.finishReload(agent.getMainHandItem(), definition);
        return ActionOutcome.success();
    }

    @Override
    public void stop(SuperMutantEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.playAction(SuperMutantAction.STOP);
    }
}

package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.util.Mth;

import mod.azure.atomicdawn.entity.scorched.ScorchedAction;
import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;
import mod.azure.atomicdawn.entity.scorched.ScorchedWeapons;
import mod.azure.atomicdawn.weapon.combat.MobGunfire;

final class ScorchedReloadTask extends ScorchedTask {

    private int ticks;

    private int duration;

    ScorchedReloadTask() {
        super(58, false);
    }

    static boolean ready(ScorchedEntity agent) {
        if (agent.isDormant() || ScorchedWeapons.kind(agent) != ScorchedWeapons.Kind.GUN) {
            return false;
        }
        var definition = ScorchedWeapons.definition(agent);
        return definition != null && MobGunfire.needsReload(agent.getMainHandItem(), definition);
    }

    @Override
    public void start(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        var definition = ScorchedWeapons.definition(agent);
        if (definition == null) {
            duration = 0;
            return;
        }
        duration = Mth.clamp(MobGunfire.reloadTicks(agent.getMainHandItem(), definition), 20, 100);
        agent.getNavigation().stop();
        agent.setStance(ScorchedWeapons.stance(agent, false));
        agent.playAction(ScorchedWeapons.reloadAction(definition));
        MobGunfire.startReload(agent, agent.getMainHandItem(), definition);
        state(blackboard, ScorchedCombatState.RELOADING);
    }

    @Override
    public ActionOutcome<Void> tick(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var definition = ScorchedWeapons.definition(agent);
        if (definition == null || ScorchedWeapons.kind(agent) != ScorchedWeapons.Kind.GUN) {
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
    public void stop(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.playAction(ScorchedAction.STOP);
    }
}

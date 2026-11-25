package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;
import mod.azure.atomicdawn.entity.scorched.ScorchedEntity;

abstract class ScorchedTask implements Action<ScorchedEntity, Void> {

    private final int priority;

    private final boolean interruptible;

    ScorchedTask(int priority, boolean interruptible) {
        this.priority = priority;
        this.interruptible = interruptible;
    }

    @Nullable
    static LivingEntity target(Blackboard blackboard) {
        LivingEntity target = blackboard.get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    static boolean visible(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(ScorchedBlackboardKeys.TARGET_VISIBLE));
    }

    static boolean shared(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(ScorchedBlackboardKeys.SHARED_TARGET));
    }

    static boolean hunting(ScorchedEntity agent, Blackboard blackboard) {
        return !agent.isDormant() && target(blackboard) != null;
    }

    static boolean investigating(Blackboard blackboard) {
        return blackboard.has(ScorchedBlackboardKeys.INVESTIGATE_UNTIL) && blackboard.has(
            ScorchedBlackboardKeys.LAST_KNOWN_POS
        );
    }

    static boolean returning(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(ScorchedBlackboardKeys.RETURNING));
    }

    static void state(Blackboard blackboard, ScorchedCombatState state) {
        blackboard.set(ScorchedBlackboardKeys.COMBAT_STATE, state);
    }

    @Override
    public void stop(ScorchedEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {}

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public int priority() {
        return priority;
    }
}

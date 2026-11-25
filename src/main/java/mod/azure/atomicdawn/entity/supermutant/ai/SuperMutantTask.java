package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;
import mod.azure.atomicdawn.entity.supermutant.SuperMutantEntity;

abstract class SuperMutantTask implements Action<SuperMutantEntity, Void> {

    private final int priority;

    private final boolean interruptible;

    SuperMutantTask(int priority, boolean interruptible) {
        this.priority = priority;
        this.interruptible = interruptible;
    }

    @Nullable
    static LivingEntity target(Blackboard blackboard) {
        LivingEntity target = blackboard.get(CommonBlackboardKeys.TARGET);
        return target != null && target.isAlive() ? target : null;
    }

    static boolean visible(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(SuperMutantBlackboardKeys.TARGET_VISIBLE));
    }

    static boolean shared(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(SuperMutantBlackboardKeys.SHARED_TARGET));
    }

    static boolean hunting(Blackboard blackboard) {
        return target(blackboard) != null;
    }

    static boolean investigating(Blackboard blackboard) {
        return blackboard.has(SuperMutantBlackboardKeys.INVESTIGATE_UNTIL) && blackboard.has(
            SuperMutantBlackboardKeys.LAST_KNOWN_POS
        );
    }

    static boolean returning(Blackboard blackboard) {
        return Boolean.TRUE.equals(blackboard.get(SuperMutantBlackboardKeys.RETURNING));
    }

    static void state(Blackboard blackboard, SuperMutantCombatState state) {
        blackboard.set(SuperMutantBlackboardKeys.COMBAT_STATE, state);
    }

    @Override
    public void stop(
        SuperMutantEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns,
        ActionStatus reason
    ) {}

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    @Override
    public int priority() {
        return priority;
    }
}

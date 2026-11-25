package mod.azure.atomicdawn.entity.supermutant.ai;

import com.azure.azurecortex.api.blackboard.BlackboardKey;
import net.minecraft.core.BlockPos;

import mod.azure.atomicdawn.entity.supermutant.SuperMutantCombatState;

public final class SuperMutantBlackboardKeys {

    public static final BlackboardKey<Boolean> SHARED_TARGET = BlackboardKey.of(
        "atomicdawn:super_mutant/shared_target",
        Boolean.class
    );

    public static final BlackboardKey<Boolean> TARGET_VISIBLE = BlackboardKey.of(
        "atomicdawn:super_mutant/target_visible",
        Boolean.class
    );

    public static final BlackboardKey<Boolean> NEW_TARGET = BlackboardKey.of(
        "atomicdawn:super_mutant/new_target",
        Boolean.class
    );

    public static final BlackboardKey<BlockPos> ALERT_ORIGIN = BlackboardKey.of(
        "atomicdawn:super_mutant/alert_origin",
        BlockPos.class
    );

    public static final BlackboardKey<BlockPos> LAST_KNOWN_POS = BlackboardKey.of(
        "atomicdawn:super_mutant/last_known_pos",
        BlockPos.class
    );

    public static final BlackboardKey<Long> INVESTIGATE_UNTIL = BlackboardKey.of(
        "atomicdawn:super_mutant/investigate_until",
        Long.class
    );

    public static final BlackboardKey<Boolean> RETURNING = BlackboardKey.of(
        "atomicdawn:super_mutant/returning",
        Boolean.class
    );

    public static final BlackboardKey<SuperMutantCombatState> COMBAT_STATE = BlackboardKey.of(
        "atomicdawn:super_mutant/combat_state",
        SuperMutantCombatState.class
    );

    private SuperMutantBlackboardKeys() {}
}

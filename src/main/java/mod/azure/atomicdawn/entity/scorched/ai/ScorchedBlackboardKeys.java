package mod.azure.atomicdawn.entity.scorched.ai;

import com.azure.azurecortex.api.blackboard.BlackboardKey;
import net.minecraft.core.BlockPos;

import mod.azure.atomicdawn.entity.scorched.ScorchedCombatState;

public final class ScorchedBlackboardKeys {

    public static final BlackboardKey<Boolean> DORMANT = BlackboardKey.of("atomicdawn:scorched/dormant", Boolean.class);

    public static final BlackboardKey<Boolean> SHARED_TARGET = BlackboardKey.of(
        "atomicdawn:scorched/shared_target",
        Boolean.class
    );

    public static final BlackboardKey<Boolean> TARGET_VISIBLE = BlackboardKey.of(
        "atomicdawn:scorched/target_visible",
        Boolean.class
    );

    public static final BlackboardKey<Boolean> NEW_TARGET = BlackboardKey.of(
        "atomicdawn:scorched/new_target",
        Boolean.class
    );

    public static final BlackboardKey<BlockPos> ALERT_ORIGIN = BlackboardKey.of(
        "atomicdawn:scorched/alert_origin",
        BlockPos.class
    );

    public static final BlackboardKey<BlockPos> FISSURE_ORIGIN = BlackboardKey.of(
        "atomicdawn:scorched/fissure_origin",
        BlockPos.class
    );

    public static final BlackboardKey<BlockPos> LAST_KNOWN_POS = BlackboardKey.of(
        "atomicdawn:scorched/last_known_pos",
        BlockPos.class
    );

    public static final BlackboardKey<Long> INVESTIGATE_UNTIL = BlackboardKey.of(
        "atomicdawn:scorched/investigate_until",
        Long.class
    );

    public static final BlackboardKey<Boolean> RETURNING = BlackboardKey.of(
        "atomicdawn:scorched/returning",
        Boolean.class
    );

    public static final BlackboardKey<ScorchedCombatState> COMBAT_STATE = BlackboardKey.of(
        "atomicdawn:scorched/combat_state",
        ScorchedCombatState.class
    );

    private ScorchedBlackboardKeys() {}
}

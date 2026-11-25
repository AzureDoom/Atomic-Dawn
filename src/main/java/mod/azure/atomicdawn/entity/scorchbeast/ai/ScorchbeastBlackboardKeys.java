package mod.azure.atomicdawn.entity.scorchbeast.ai;

import com.azure.azurecortex.api.blackboard.BlackboardKey;

public final class ScorchbeastBlackboardKeys {

    public static final BlackboardKey<Boolean> TARGET_VISIBLE = BlackboardKey.of(
        "atomicdawn:scorchbeast/target_visible",
        Boolean.class
    );

    public static final BlackboardKey<Integer> GROUND_STUCK = BlackboardKey.of(
        "atomicdawn:scorchbeast/ground_stuck",
        Integer.class
    );

    public static final BlackboardKey<Boolean> LEFT_SWING = BlackboardKey.of(
        "atomicdawn:scorchbeast/left_swing",
        Boolean.class
    );

    private ScorchbeastBlackboardKeys() {}
}

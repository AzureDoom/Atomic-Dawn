package mod.azure.atomicdawn.entity.misterhandy.ai;

import com.azure.azurecortex.api.blackboard.BlackboardKey;
import net.minecraft.core.BlockPos;

public final class MisterHandyBlackboardKeys {

    public static final BlackboardKey<Boolean> TARGET_VISIBLE = BlackboardKey.of(
        "atomicdawn:mister_handy/target_visible",
        Boolean.class
    );

    public static final BlackboardKey<BlockPos> LAST_KNOWN_POS = BlackboardKey.of(
        "atomicdawn:mister_handy/last_known_pos",
        BlockPos.class
    );

    public static final BlackboardKey<Long> INVESTIGATE_UNTIL = BlackboardKey.of(
        "atomicdawn:mister_handy/investigate_until",
        Long.class
    );

    private MisterHandyBlackboardKeys() {}
}

package mod.azure.atomicdawn.vault;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class VaultDoorAnimations {

    public static final String BASE = "base";

    private static final AzCommand OPEN = AzCommand.create(BASE, "open", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand CLOSE = AzCommand.create(BASE, "close", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand OPENED = AzCommand.create(BASE, "opened", AzPlayBehaviors.LOOP);

    private static final AzCommand CLOSED = AzCommand.create(BASE, "closed", AzPlayBehaviors.LOOP);

    private VaultDoorAnimations() {}

    public static void open(BlockEntity door) {
        OPEN.sendForBlockEntity(door);
    }

    public static void close(BlockEntity door) {
        CLOSE.sendForBlockEntity(door);
    }

    public static void snap(BlockEntity door, boolean open) {
        (open ? OPENED : CLOSED).sendForBlockEntity(door);
    }
}

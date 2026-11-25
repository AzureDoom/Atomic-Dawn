package mod.azure.atomicdawn.entity.brahmin;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.Map;

public final class BrahminAnimations {

    public static final String BASE = "base";

    public static final String ACTION = "action";

    private static final Map<BrahminState, AzCommand> STATES = new EnumMap<>(BrahminState.class);

    private static final Map<BrahminAction, AzCommand> ACTIONS = new EnumMap<>(BrahminAction.class);

    static {
        for (var state : BrahminState.values()) {
            STATES.put(
                state,
                AzCommand.create(
                    BASE,
                    state.animation(),
                    state == BrahminState.DEAD ? AzPlayBehaviors.HOLD_ON_LAST_FRAME : AzPlayBehaviors.LOOP
                )
            );
        }
        for (var action : BrahminAction.values()) {
            if (action == BrahminAction.NONE) {
                continue;
            }
            ACTIONS.put(
                action,
                AzCommand.controllerBuilder()
                    .cancel(ACTION)
                    .playSequence(ACTION, AzSequence.builder().play(action.animation()).build())
                    .build()
            );
        }
    }

    private BrahminAnimations() {}

    public static void state(Entity entity, BrahminState state) {
        STATES.get(state).sendForEntity(entity);
    }

    public static void action(Entity entity, BrahminAction action) {
        var command = ACTIONS.get(action);
        if (command != null) {
            command.sendForEntity(entity);
        }
    }
}

package mod.azure.atomicdawn.entity.ghoul;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.Map;

public final class FeralGhoulAnimations {

    public static final String BASE = "base";

    public static final String ACTION = "action";

    private static final Map<FeralGhoulState, AzCommand> STATES = new EnumMap<>(FeralGhoulState.class);

    private static final Map<FeralGhoulAction, AzCommand> ACTIONS = new EnumMap<>(FeralGhoulAction.class);

    static {
        for (var state : FeralGhoulState.values()) {
            STATES.put(
                state,
                AzCommand.create(
                    BASE,
                    state.animation(),
                    state == FeralGhoulState.DEAD ? AzPlayBehaviors.HOLD_ON_LAST_FRAME : AzPlayBehaviors.LOOP
                )
            );
        }
        for (var action : FeralGhoulAction.values()) {
            if (action == FeralGhoulAction.NONE) {
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

    private FeralGhoulAnimations() {}

    public static void state(Entity entity, FeralGhoulState state) {
        STATES.get(state).sendForEntity(entity);
    }

    public static void action(Entity entity, FeralGhoulAction action) {
        var command = ACTIONS.get(action);
        if (command != null) {
            command.sendForEntity(entity);
        }
    }
}

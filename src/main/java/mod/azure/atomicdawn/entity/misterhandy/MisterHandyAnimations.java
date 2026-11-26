package mod.azure.atomicdawn.entity.misterhandy;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.Map;

public final class MisterHandyAnimations {

    public static final String BASE = "base";

    public static final String ACTION = "action";

    private static final Map<MisterHandyState, AzCommand> STATES = new EnumMap<>(MisterHandyState.class);

    private static final Map<MisterHandyAction, AzCommand> ACTIONS = new EnumMap<>(MisterHandyAction.class);

    static {
        for (var state : MisterHandyState.values()) {
            STATES.put(
                state,
                AzCommand.create(
                    BASE,
                    state.animation(),
                    state == MisterHandyState.DEAD ? AzPlayBehaviors.HOLD_ON_LAST_FRAME : AzPlayBehaviors.LOOP
                )
            );
        }
        for (var action : MisterHandyAction.values()) {
            if (action == MisterHandyAction.STOP) {
                ACTIONS.put(action, AzCommand.controllerBuilder().cancel(ACTION).build());
                continue;
            }
            if (action.animation().isEmpty()) {
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

    private MisterHandyAnimations() {}

    public static void state(Entity entity, MisterHandyState state) {
        STATES.get(state).sendForEntity(entity);
    }

    public static void action(Entity entity, MisterHandyAction action) {
        var command = ACTIONS.get(action);
        if (command != null) {
            command.sendForEntity(entity);
        }
    }
}

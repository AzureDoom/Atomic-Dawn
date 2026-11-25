package mod.azure.atomicdawn.entity.scorchbeast;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.Map;

public final class ScorchbeastAnimations {

    public static final String BASE = "base";

    public static final String ACTION = "action";

    private static final Map<ScorchbeastFlight, AzCommand> FLIGHT = new EnumMap<>(ScorchbeastFlight.class);

    private static final Map<ScorchbeastAction, AzCommand> ACTIONS = new EnumMap<>(ScorchbeastAction.class);

    private static final AzCommand WALK = AzCommand.create(
        BASE,
        "animation.scorchbeast.ground_walk",
        AzPlayBehaviors.LOOP
    );

    static {
        for (var state : ScorchbeastFlight.values()) {
            FLIGHT.put(
                state,
                AzCommand.create(
                    BASE,
                    state.animation(),
                    state.loop() ? AzPlayBehaviors.LOOP : AzPlayBehaviors.HOLD_ON_LAST_FRAME
                )
            );
        }
        for (var action : ScorchbeastAction.values()) {
            if (action == ScorchbeastAction.STOP) {
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

    private ScorchbeastAnimations() {}

    public static void flight(Entity entity, ScorchbeastFlight state) {
        FLIGHT.get(state).sendForEntity(entity);
    }

    public static void walk(Entity entity) {
        WALK.sendForEntity(entity);
    }

    public static void action(Entity entity, ScorchbeastAction action) {
        var command = ACTIONS.get(action);
        if (command != null) {
            command.sendForEntity(entity);
        }
    }
}

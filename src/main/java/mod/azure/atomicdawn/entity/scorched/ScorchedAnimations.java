package mod.azure.atomicdawn.entity.scorched;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class ScorchedAnimations {

    public static final String BASE = "base";

    public static final String ARMS = "arms";

    public static final String ACTION = "action";

    private static final Map<String, AzCommand> STATES = new HashMap<>();

    private static final Map<ScorchedStance, AzCommand> STANCES = new EnumMap<>(ScorchedStance.class);

    private static final Map<ScorchedAction, AzCommand> ACTIONS = new EnumMap<>(ScorchedAction.class);

    static {
        for (var state : ScorchedState.values()) {
            var poses = state == ScorchedState.DORMANT ? ScorchedState.DORMANT_POSES : 1;
            for (var pose = 0; pose < poses; pose++) {
                var animation = state.animation(pose);
                STATES.put(
                    animation,
                    AzCommand.create(
                        BASE,
                        animation,
                        state == ScorchedState.DEAD ? AzPlayBehaviors.HOLD_ON_LAST_FRAME : AzPlayBehaviors.LOOP
                    )
                );
            }
        }
        for (var stance : ScorchedStance.values()) {
            STANCES.put(stance, AzCommand.create(ARMS, stance.animation(), AzPlayBehaviors.LOOP));
        }
        for (var action : ScorchedAction.values()) {
            if (action == ScorchedAction.STOP) {
                ACTIONS.put(action, AzCommand.controllerBuilder().cancel(ACTION).build());
                continue;
            }
            if (action.animation().isEmpty()) {
                continue;
            }
            if (action.looping()) {
                ACTIONS.put(action, AzCommand.create(ACTION, action.animation(), AzPlayBehaviors.LOOP));
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

    private ScorchedAnimations() {}

    public static void state(Entity entity, ScorchedState state, int pose) {
        var command = STATES.get(state.animation(pose));
        if (command != null) {
            command.sendForEntity(entity);
        }
    }

    public static void stance(Entity entity, ScorchedStance stance) {
        STANCES.get(stance).sendForEntity(entity);
    }

    public static void action(Entity entity, ScorchedAction action) {
        var command = ACTIONS.get(action);
        if (command != null) {
            command.sendForEntity(entity);
        }
    }
}

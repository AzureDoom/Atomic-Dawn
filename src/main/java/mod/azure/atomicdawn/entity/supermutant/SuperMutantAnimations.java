package mod.azure.atomicdawn.entity.supermutant;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.Map;

public final class SuperMutantAnimations {

    public static final String BASE = "base";

    public static final String ARMS = "arms";

    public static final String ACTION = "action";

    private static final Map<SuperMutantState, AzCommand> STATES = new EnumMap<>(SuperMutantState.class);

    private static final Map<SuperMutantStance, AzCommand> STANCES = new EnumMap<>(SuperMutantStance.class);

    private static final Map<SuperMutantAction, AzCommand> ACTIONS = new EnumMap<>(SuperMutantAction.class);

    static {
        for (var state : SuperMutantState.values()) {
            STATES.put(
                state,
                AzCommand.create(
                    BASE,
                    state.animation(),
                    state == SuperMutantState.DEAD ? AzPlayBehaviors.HOLD_ON_LAST_FRAME : AzPlayBehaviors.LOOP
                )
            );
        }
        for (var stance : SuperMutantStance.values()) {
            STANCES.put(
                stance,
                stance.animation().isEmpty()
                    ? AzCommand.controllerBuilder().cancel(ARMS).build()
                    : AzCommand.create(ARMS, stance.animation(), AzPlayBehaviors.LOOP)
            );
        }
        for (var action : SuperMutantAction.values()) {
            if (action == SuperMutantAction.STOP) {
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

    private SuperMutantAnimations() {}

    public static void state(Entity entity, SuperMutantState state) {
        STATES.get(state).sendForEntity(entity);
    }

    public static void stance(Entity entity, SuperMutantStance stance) {
        STANCES.get(stance).sendForEntity(entity);
    }

    public static void action(Entity entity, SuperMutantAction action) {
        var command = ACTIONS.get(action);
        if (command != null) {
            command.sendForEntity(entity);
        }
    }
}

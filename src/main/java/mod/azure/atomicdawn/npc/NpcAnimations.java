package mod.azure.atomicdawn.npc;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcAnimations {

    public static final String BASE = "base";

    public static final String ACTION = "action";

    private static final Map<String, AzCommand> LOOPS = new ConcurrentHashMap<>();

    private static final Map<String, AzCommand> ONE_SHOTS = new ConcurrentHashMap<>();

    private NpcAnimations() {}

    public static void loop(Entity entity, String animation) {
        if (animation == null || animation.isEmpty()) {
            return;
        }
        LOOPS.computeIfAbsent(animation, name -> AzCommand.create(BASE, name, AzPlayBehaviors.LOOP))
            .sendForEntity(entity);
    }

    public static void play(Entity entity, String animation) {
        if (animation == null || animation.isEmpty()) {
            return;
        }
        ONE_SHOTS.computeIfAbsent(
            animation,
            name -> AzCommand.controllerBuilder()
                .cancel(ACTION)
                .playSequence(ACTION, AzSequence.builder().play(name).build())
                .build()
        ).sendForEntity(entity);
    }
}

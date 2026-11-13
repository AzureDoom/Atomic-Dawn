package mod.azure.atomicdawn.powerarmor;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

public final class PowerArmorAnimations {

    public static final String BASE = "base";

    public static final String ACTION = "action";

    private static final AzCommand IDLE = AzCommand.create(BASE, "idle", AzPlayBehaviors.LOOP);

    private static final AzCommand WALK = AzCommand.create(BASE, "walk", AzPlayBehaviors.LOOP);

    private static final AzCommand EMPTY = AzCommand.create(BASE, "empty", AzPlayBehaviors.LOOP);

    private static final AzCommand LANDING = AzCommand.controllerBuilder()
        .cancel(ACTION)
        .playSequence(ACTION, AzSequence.builder().play("landing").build())
        .build();

    private static final AzCommand ENTER = AzCommand.controllerBuilder()
        .cancel(ACTION)
        .playSequence(ACTION, AzSequence.builder().play("enter").build())
        .build();

    private static final AzCommand EXIT = AzCommand.controllerBuilder()
        .cancel(ACTION)
        .playSequence(ACTION, AzSequence.builder().play("exit").build())
        .build();

    private PowerArmorAnimations() {}

    public static void enter(Entity entity) {
        ENTER.sendForEntity(entity);
    }

    public static void exit(Entity entity) {
        EXIT.sendForEntity(entity);
    }

    public static void idle(Entity entity) {
        IDLE.sendForEntity(entity);
    }

    public static void walk(Entity entity) {
        WALK.sendForEntity(entity);
    }

    public static void empty(Entity entity) {
        EMPTY.sendForEntity(entity);
    }

    public static void landing(Entity entity) {
        LANDING.sendForEntity(entity);
    }
}

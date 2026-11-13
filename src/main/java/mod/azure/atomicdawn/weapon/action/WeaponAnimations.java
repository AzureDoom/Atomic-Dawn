package mod.azure.atomicdawn.weapon.action;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.dispatch.command.sequence.AzSequence;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class WeaponAnimations {

    public static final String CONTROLLER = "weapon";

    private static final AzCommand DRAW = sequence("draw", "idle");

    private static final AzCommand IDLE = AzCommand.create(CONTROLLER, "idle", AzPlayBehaviors.LOOP);

    private static final AzCommand AIM = AzCommand.create(CONTROLLER, "aim", AzPlayBehaviors.LOOP);

    private static final AzCommand FIRE = sequence("fire", "idle");

    private static final AzCommand AIM_FIRE = sequence("aim_fire", "aim");

    private static final AzCommand RELOAD = sequence("reload", "idle");

    private static final AzCommand RELOAD_EMPTY = sequence("reload_empty", "idle");

    private static final AzCommand INSPECT = sequence("inspect", "idle");

    private static final AzCommand RELOAD_START = hold("reload_start");

    private static final AzCommand RELOAD_INSERT = hold("reload_insert");

    private static final AzCommand RELOAD_END = sequence("reload_end", "idle");

    private static final AzCommand CHARGE = hold("charge");

    private static final AzCommand SPIN_UP = hold("spin_up");

    private static final AzCommand SPIN_DOWN = sequence("spin_down", "idle");

    private static final AzCommand TP_IDLE = AzCommand.create(CONTROLLER, "tp_idle", AzPlayBehaviors.LOOP);

    private static final AzCommand TP_FIRE = sequence("tp_fire", "tp_idle");

    private static final AzCommand TP_RELOAD = sequence("tp_reload", "tp_idle");

    private WeaponAnimations() {}

    public static void draw(Entity holder, ItemStack stack) {
        DRAW.sendForItemFirstPerson(holder, stack);
        TP_IDLE.sendForItemThirdPerson(holder, stack);
    }

    public static void aim(Entity holder, ItemStack stack, boolean aiming) {
        (aiming ? AIM : IDLE).sendForItemFirstPerson(holder, stack);
    }

    public static void fire(Entity holder, ItemStack stack, boolean aiming) {
        (aiming ? AIM_FIRE : FIRE).sendForItemFirstPerson(holder, stack);
        TP_FIRE.sendForItemThirdPerson(holder, stack);
    }

    public static void reload(Entity holder, ItemStack stack, boolean empty) {
        (empty ? RELOAD_EMPTY : RELOAD).sendForItemFirstPerson(holder, stack);
        TP_RELOAD.sendForItemThirdPerson(holder, stack);
    }

    public static void reloadStart(Entity holder, ItemStack stack) {
        RELOAD_START.sendForItemFirstPerson(holder, stack);
        TP_RELOAD.sendForItemThirdPerson(holder, stack);
    }

    public static void reloadInsert(Entity holder, ItemStack stack) {
        RELOAD_INSERT.sendForItemFirstPerson(holder, stack);
    }

    public static void reloadEnd(Entity holder, ItemStack stack) {
        RELOAD_END.sendForItemFirstPerson(holder, stack);
    }

    public static void charge(Entity holder, ItemStack stack) {
        CHARGE.sendForItemFirstPerson(holder, stack);
    }

    public static void spinUp(Entity holder, ItemStack stack) {
        SPIN_UP.sendForItemFirstPerson(holder, stack);
    }

    public static void spinDown(Entity holder, ItemStack stack) {
        SPIN_DOWN.sendForItemFirstPerson(holder, stack);
    }

    public static void inspect(Entity holder, ItemStack stack) {
        INSPECT.sendForItemFirstPerson(holder, stack);
    }

    public static void cancelToIdle(Entity holder, ItemStack stack, boolean aiming) {
        aim(holder, stack, aiming);
        TP_IDLE.sendForItemThirdPerson(holder, stack);
    }

    private static AzCommand sequence(String action, String loopAfter) {
        return restart(AzSequence.builder().play(action).then(loopAfter, AzPlayBehaviors.LOOP).build());
    }

    private static AzCommand hold(String action) {
        return restart(AzSequence.builder().hold(action).build());
    }

    private static AzCommand restart(AzSequence sequence) {
        return AzCommand.controllerBuilder().cancel(CONTROLLER).playSequence(CONTROLLER, sequence).build();
    }
}

package mod.azure.atomicdawn.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import mod.azure.atomicdawn.AtomicDawn;

public final class ADKeyMappings {

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(AtomicDawn.id("weapons"));

    public static final KeyMapping RELOAD = key("reload", InputConstants.KEY_R);

    public static final KeyMapping FIRE_MODE = key("fire_mode", InputConstants.KEY_B);

    public static final KeyMapping INSPECT = key("inspect", InputConstants.KEY_H);

    public static final KeyMapping VATS = key("vats", InputConstants.KEY_V);

    public static final KeyMapping CRITICAL = key("critical", InputConstants.KEY_G);

    public static final KeyMapping PIPBOY = key("pipboy", InputConstants.KEY_P);

    public static final KeyMapping PIPBOY_LIGHT = key("pipboy_light", InputConstants.KEY_J);

    public static final KeyMapping EXIT_POWER_ARMOR = key("exit_power_armor", InputConstants.KEY_Z);

    public static final KeyMapping WAYPOINT_HUD = key("waypoint_hud", InputConstants.UNKNOWN.getValue());

    public static final KeyMapping HUD_EDITOR = key("hud_editor", InputConstants.UNKNOWN.getValue());

    public static final KeyMapping WORKSHOP = key("workshop", InputConstants.KEY_N);

    public static final KeyMapping WORKSHOP_CATALOG = key("workshop_catalog", InputConstants.KEY_Y);

    public static final KeyMapping WORKSHOP_ROTATE = key("workshop_rotate", InputConstants.KEY_COMMA);

    public static final KeyMapping WORKSHOP_MODE = key("workshop_mode", InputConstants.KEY_PERIOD);

    private ADKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(RELOAD);
        event.register(FIRE_MODE);
        event.register(INSPECT);
        event.register(VATS);
        event.register(CRITICAL);
        event.register(PIPBOY);
        event.register(PIPBOY_LIGHT);
        event.register(WAYPOINT_HUD);
        event.register(HUD_EDITOR);
        event.register(EXIT_POWER_ARMOR);
        event.register(WORKSHOP);
        event.register(WORKSHOP_CATALOG);
        event.register(WORKSHOP_ROTATE);
        event.register(WORKSHOP_MODE);
    }

    private static KeyMapping key(String name, int glfwKey) {
        return new KeyMapping(
            "key.atomicdawn." + name,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            glfwKey,
            CATEGORY
        );
    }
}

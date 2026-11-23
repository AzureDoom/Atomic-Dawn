package mod.azure.atomicdawn.client.hud.layout;

import net.minecraft.client.Minecraft;

import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.pipboy.Rect;

@SuppressWarnings("unused")
public enum HudElement {

    WEAPON((w, h) -> new Rect(w - 60, h - 39, 52, 31)),
    VATS_METERS((w, h) -> new Rect(w - 88, h - 69, 80, 26)),
    RADIATION((w, h) -> new Rect(w / 2 - 131, h - 19, 36, 17)),
    THIRST((w, h) -> new Rect(w / 2 + 95, h - 19, 36, 17)),
    LIMBS((w, h) -> new Rect(w / 2 - 197, h - 38, 20, 36)),
    GHOUL((w, h) -> new Rect(w / 2 - 173, h - 47, 78, 24)),
    POWER_ARMOR((w, h) -> new Rect(6, h - 92, 150, 86)),
    POWER_ARMOR_AP((w, h) -> new Rect(6, h - 110, 150, 15)),
    COMPASS((w, h) -> {
        var width = Math.min(AtomicDawn.CONFIG.clientPipboy.WAYPOINT_HUD_WIDTH, w - 8);
        return new Rect((w - width) / 2, AtomicDawn.CONFIG.clientPipboy.WAYPOINT_HUD_Y, width, 12);
    }),
    PARTY((w, h) -> new Rect(2, AtomicDawn.CONFIG.clientParty.PARTY_HUD_Y - 2, 96, 46)),
    QUEST_TRACKER((w, h) -> new Rect(4, 4, 140, 25)),
    NOTIFICATIONS((w, h) -> new Rect(4, 32, 140, 30));

    @FunctionalInterface
    public interface DefaultRect {

        Rect at(int screenWidth, int screenHeight);
    }

    private final DefaultRect fallback;

    HudElement(DefaultRect fallback) {
        this.fallback = fallback;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "hud_editor.atomicdawn.element." + id();
    }

    public Rect fallback() {
        var window = Minecraft.getInstance().getWindow();
        return fallback.at(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    public static HudElement byId(String id) {
        for (var element : values()) {
            if (element.id().equals(id)) {
                return element;
            }
        }
        return null;
    }
}

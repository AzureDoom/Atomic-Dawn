package mod.azure.atomicdawn.client.hud.layout;

import java.util.Locale;

public enum HudAnchor {

    TOP_LEFT(0F, 0F),
    TOP(0.5F, 0F),
    TOP_RIGHT(1F, 0F),
    LEFT(0F, 0.5F),
    CENTER(0.5F, 0.5F),
    RIGHT(1F, 0.5F),
    BOTTOM_LEFT(0F, 1F),
    BOTTOM(0.5F, 1F),
    BOTTOM_RIGHT(1F, 1F);

    public final float fx;

    public final float fy;

    HudAnchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }

    public static HudAnchor nearest(float centerX, float centerY, int screenWidth, int screenHeight) {
        var column = centerX < screenWidth / 3F ? 0 : centerX < screenWidth * 2F / 3F ? 1 : 2;
        var row = centerY < screenHeight / 3F ? 0 : centerY < screenHeight * 2F / 3F ? 1 : 2;
        return values()[row * 3 + column];
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HudAnchor byId(String id) {
        for (var anchor : values()) {
            if (anchor.id().equals(id)) {
                return anchor;
            }
        }
        return TOP_LEFT;
    }
}

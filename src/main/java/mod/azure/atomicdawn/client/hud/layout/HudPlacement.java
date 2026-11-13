package mod.azure.atomicdawn.client.hud.layout;

public record HudPlacement(
    HudAnchor anchor,
    int offsetX,
    int offsetY,
    float scale,
    boolean hidden
) {

    public static final float MIN_SCALE = 0.5F;

    public static final float MAX_SCALE = 2.5F;

    public HudPlacement {
        scale = Float.isFinite(scale) ? Math.clamp(scale, MIN_SCALE, MAX_SCALE) : 1F;
    }

    public HudPlacement withScale(float value) {
        return new HudPlacement(anchor, offsetX, offsetY, value, hidden);
    }

    public HudPlacement withHidden(boolean value) {
        return new HudPlacement(anchor, offsetX, offsetY, scale, value);
    }
}

package mod.azure.atomicdawn.client.pipboy;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.config.ADColors;

@SuppressWarnings("unused")
public final class PipBoyTheme {

    private PipBoyTheme() {}

    public static int rgb() {
        return AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST
            ? 0xFFFFFF
            : ADColors.rgb(AtomicDawn.CONFIG.clientPipboy.PIPBOY_COLOR, 0x1AFF80);
    }

    public static int scaled(int rgb, float factor, int alpha) {
        var r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        var g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        var b = Math.min(255, Math.round((rgb & 0xFF) * factor));
        return (Math.clamp(alpha, 0, 255) << 24) | (r << 16) | (g << 8) | b;
    }

    public static int phosphor(float brightness, int alpha) {
        return scaled(rgb(), brightness, alpha);
    }

    public static int withAlpha(int argb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0xFFFFFF);
    }

    public static int text() {
        return scaled(rgb(), 1F, 0xFF);
    }

    public static int dim() {
        return scaled(rgb(), AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0.75F : 0.6F, 0xFF);
    }

    public static int disabled() {
        return scaled(rgb(), AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0.55F : 0.4F, 0xFF);
    }

    public static int faint() {
        return scaled(rgb(), 0.3F, 0xFF);
    }

    public static int border() {
        return scaled(rgb(), 0.7F, 0xFF);
    }

    public static int highlight() {
        return scaled(rgb(), 0.25F, 0xC0);
    }

    public static int selection() {
        return scaled(rgb(), AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0.4F : 0.32F, 0xFF);
    }

    public static int ink() {
        return AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0xFF000000 : scaled(rgb(), 0.07F, 0xFF);
    }

    public static int background() {
        return AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0xF8000000 : scaled(rgb(), 0.06F, 0xF0);
    }

    public static int glass() {
        return AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0xFF000000 : scaled(rgb(), 0.045F, 0xFF);
    }

    public static int panel() {
        return AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0xFF000000 : scaled(rgb(), 0.1F, 0xFF);
    }

    public static int frame() {
        return 0xFF2A2B22;
    }

    public static int frameEdge() {
        return 0xFF14150F;
    }

    public static int warn() {
        return AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0xFFFFFF00 : 0xFFFFB13C;
    }

    public static int bad() {
        return AtomicDawn.CONFIG.clientPipboy.PIPBOY_HIGH_CONTRAST ? 0xFFFF5555 : 0xFFFF6C5A;
    }

    public static int scanline() {
        var intensity = (float) AtomicDawn.CONFIG.clientPipboy.PIPBOY_SCANLINES;
        return Math.round(0x50 * intensity) << 24;
    }
}

package mod.azure.atomicdawn.client.pipboy;

import net.minecraft.network.chat.Component;

import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.config.ADConfig.ClientPipBoy.PipBoyLayout;

public final class PipBoyModel {

    public record Palette(
        int outline,
        int body,
        int bodyLight,
        int bodyShade,
        int bodyDark,
        int recess,
        int recessDeep,
        int trim,
        int trimLight,
        int trimDark,
        int plateText,
        int plateTextHi,
        int labelOff,
        int labelOn,
        int key,
        int keyHover,
        int lampOn,
        int lampOff
    ) {}

    private static final Palette FALLOUT_76 = new Palette(
        0xFF1E2016,
        0xFF5F664A,
        0xFF7F8763,
        0xFF434833,
        0xFF353927,
        0xFF24271B,
        0xFF141610,
        0xFFB48D3E,
        0xFFDDB865,
        0xFF6E5420,
        0xFF2C3021,
        0xFF98A07A,
        0xFF9BA285,
        0xFFF0B84E,
        0xFF3D4130,
        0xFF4A4F38,
        0xE3271E,
        0x4C1210
    );

    private static final Palette FALLOUT_4 = new Palette(
        0xFF15180F,
        0xFF4E5A3A,
        0xFF6B7A50,
        0xFF3A4429,
        0xFF2C3420,
        0xFF1C1E18,
        0xFF0C0D0A,
        0xFF33332E,
        0xFF55554C,
        0xFF1A1A17,
        0xFF1F2417,
        0xFF8C9A6C,
        0xFFC9C3A4,
        0xFFF2E3A6,
        0xFF2F3826,
        0xFF3C4730,
        0xE0402A,
        0x5A1A12
    );

    private static final Palette FALLOUT_3 = new Palette(
        0xFF161613,
        0xFF5E5E57,
        0xFF85857C,
        0xFF45453F,
        0xFF33332E,
        0xFF1E1E1B,
        0xFF0D0D0C,
        0xFF8C9898,
        0xFFB7C3C2,
        0xFF5D6969,
        0xFF1E1E1B,
        0xFFB8B8AE,
        0xFFC8C8BC,
        0xFFF2F2E6,
        0xFF3A3A35,
        0xFF4A4A44,
        0xFFC8A0,
        0xC07058
    );

    private static final Palette NEW_VEGAS = new Palette(
        0xFF1F1A10,
        0xFF8A7446,
        0xFFAE9662,
        0xFF6A5832,
        0xFF4E4124,
        0xFF2A2416,
        0xFF14110A,
        0xFFB48D3E,
        0xFFDDB865,
        0xFF6E5420,
        0xFF2A2214,
        0xFFC9B587,
        0xFFD8B850,
        0xFFFFD45A,
        0xFF5A4A2C,
        0xFF6A5836,
        0xFFC040,
        0xB0601C
    );

    private static final Palette FALLOUT_1_2 = new Palette(
        0xFF181814,
        0xFF6E6A5E,
        0xFF928D7E,
        0xFF524F46,
        0xFF3E3C35,
        0xFF25241F,
        0xFF121210,
        0xFF8A6A3A,
        0xFFB08B52,
        0xFF5A4322,
        0xFF26251F,
        0xFFA7A291,
        0xFFAAA593,
        0xFFE8D27A,
        0xFF3A3832,
        0xFF48463E,
        0xE83A28,
        0x7A1A14
    );

    private PipBoyModel() {}

    public static PipBoyLayout layout() {
        var layout = AtomicDawn.CONFIG.clientPipboy.PIPBOY_LAYOUT;
        return layout == null ? PipBoyLayout.FALLOUT_76 : layout;
    }

    public static Palette palette() {
        return switch (layout()) {
            case FALLOUT_76 -> FALLOUT_76;
            case FALLOUT_4 -> FALLOUT_4;
            case NEW_VEGAS -> NEW_VEGAS;
            case FALLOUT_3 -> FALLOUT_3;
            case FALLOUT_1_2 -> FALLOUT_1_2;
        };
    }

    public static Component name() {
        return Component.translatable("pipboy.atomicdawn.model." + layout().name().toLowerCase(Locale.ROOT));
    }
}

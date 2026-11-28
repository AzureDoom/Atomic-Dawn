package mod.azure.atomicdawn.client.hud.fallout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.client.powerarmor.PowerArmorClient;
import mod.azure.atomicdawn.client.powerarmor.PowerArmorGauges;
import mod.azure.atomicdawn.config.ADColors;
import mod.azure.atomicdawn.config.ADConfig.Client.Hud.HudStyle;
import mod.azure.atomicdawn.config.ADConfig.Client.Hud.Layout;
import mod.azure.atomicdawn.thirst.ThirstService;

public final class FalloutHud {

    public static final int MARGIN = 6;

    public static final int ROW = 9;

    public static final int BAR_HEIGHT = 5;

    public static final int LABEL_WIDTH = 13;

    public static final int LABEL_GAP = 3;

    public static final int HEALTH_HEIGHT = 20;

    public static final int AP_HEIGHT = 12;

    public static final int SURVIVAL_HEIGHT = ROW;

    public static final int WEAPON_HEIGHT = 14;

    public static final int STACK_GAP = 3;

    public static final int COMPASS_HEIGHT = 12;

    public static final int HOTBAR_HALF = 91;

    private static final int MIN_BAR = 56;

    private static final int MAX_BAR = 120;

    private static final int COMPACT_BAR = 70;

    private static final int ABOVE_XP_LEVEL = 36;

    private static final int COMPASS_BOTTOM = 42;

    private static final int VANILLA_ROW_TOP = 39;

    public static final String[] ICON_FOOD = {
        "#.#.#.##",
        "#.#.#.##",
        "#####.##",
        ".###..##",
        "..#....#",
        "..#....#",
        "..#....#" };

    public static final String[] ICON_WATER = {
        "...#...",
        "..###..",
        ".#####.",
        "#######",
        "#######",
        ".#####.",
        "..###.." };

    public static final String[] ICON_ARMOR = {
        "#######",
        "#######",
        "#######",
        ".#####.",
        ".#####.",
        "..###..",
        "...#..." };

    public static final String[] ICON_AIR = {
        ".###.",
        "#..##",
        "#...#",
        "#...#",
        ".###." };

    private FalloutHud() {}

    private static Layout layout() {
        return AtomicDawn.CONFIG.client.hud.layout;
    }

    private static boolean is(HudStyle style) {
        return style == HudStyle.FALLOUT_76;
    }

    public static boolean health() {
        return is(layout().HEALTH_STYLE);
    }

    public static boolean radiation() {
        return health() && is(layout().RADIATION_STYLE);
    }

    public static boolean armor() {
        return health() && is(layout().ARMOR_STYLE);
    }

    public static boolean air() {
        return health() && is(layout().AIR_STYLE);
    }

    public static boolean food() {
        return is(layout().FOOD_STYLE);
    }

    public static boolean thirst() {
        return is(layout().THIRST_STYLE);
    }

    public static boolean actionPoints() {
        return is(layout().ACTION_POINTS_STYLE);
    }

    public static boolean weapon() {
        return is(layout().WEAPON_STYLE);
    }

    public static boolean compass() {
        return is(layout().COMPASS_STYLE);
    }

    public static boolean healthDrawn(@Nullable LocalPlayer player) {
        return health() && survivalMode(player) && !PowerArmorClient.inPowerArmor();
    }

    public static boolean survivalVisible(@Nullable LocalPlayer player) {
        if (!survivalMode(player) || PowerArmorGauges.active()) {
            return false;
        }
        return food() || (thirst() && ThirstService.enabled());
    }

    public static boolean survivalMode(@Nullable LocalPlayer player) {
        return player != null && !player.isCreative() && !player.isSpectator();
    }

    public static int primary() {
        return layout().HUD_USE_PIPBOY_COLOR ? PipBoyTheme.text() : ADColors.argb(layout().HUD_COLOR, 0xFFB9F27C);
    }

    public static int dim() {
        return scale(primary(), 0.62F, 0xFF);
    }

    public static int faint() {
        return scale(primary(), 0.45F, 0x70);
    }

    public static int rads() {
        return ADColors.argb(layout().HUD_RADS_COLOR, 0xFFD8352A);
    }

    public static int warn() {
        return ADColors.argb(layout().HUD_WARN_COLOR, 0xFFFFB347);
    }

    public static int shadow() {
        return ADColors.argb(layout().HUD_SHADOW_COLOR, 0x90000000);
    }

    public static int scale(int argb, float factor, int alpha) {
        var r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * factor));
        var g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * factor));
        var b = Math.min(255, Math.round((argb & 0xFF) * factor));
        return (Math.clamp(alpha, 0, 255) << 24) | (r << 16) | (g << 8) | b;
    }

    public static boolean blink(@Nullable LocalPlayer player) {
        return player != null && (player.tickCount / 6) % 2 == 0;
    }

    private static int sideSpace(int screenWidth) {
        return screenWidth / 2 - HOTBAR_HALF - MARGIN * 2 - LABEL_WIDTH - LABEL_GAP;
    }

    public static boolean compact(int screenWidth) {
        return sideSpace(screenWidth) < MIN_BAR;
    }

    public static int barWidth(int screenWidth) {
        return compact(screenWidth) ? COMPACT_BAR : Math.min(MAX_BAR, sideSpace(screenWidth));
    }

    public static int blockWidth(int screenWidth) {
        return LABEL_WIDTH + LABEL_GAP + barWidth(screenWidth);
    }

    private static int bottom(int screenWidth, int screenHeight) {
        return compact(screenWidth) ? screenHeight - ABOVE_XP_LEVEL : screenHeight - MARGIN;
    }

    public static Rect healthRect(int screenWidth, int screenHeight) {
        var width = blockWidth(screenWidth);
        var x = compact(screenWidth) ? screenWidth / 2 - HOTBAR_HALF : MARGIN;
        return new Rect(x, bottom(screenWidth, screenHeight) - HEALTH_HEIGHT, width, HEALTH_HEIGHT);
    }

    public static Rect actionPointsRect(int screenWidth, int screenHeight) {
        var width = blockWidth(screenWidth);
        var x = compact(screenWidth) ? screenWidth / 2 + HOTBAR_HALF - width : screenWidth - MARGIN - width;
        return new Rect(x, bottom(screenWidth, screenHeight) - AP_HEIGHT, width, AP_HEIGHT);
    }

    public static Rect survivalRect(int screenWidth, int screenHeight) {
        var ap = actionPointsRect(screenWidth, screenHeight);
        return new Rect(ap.x(), ap.y() - STACK_GAP - SURVIVAL_HEIGHT, ap.width(), SURVIVAL_HEIGHT);
    }

    public static Rect weaponRect(int screenWidth, int screenHeight, int width, @Nullable LocalPlayer player) {
        var ap = actionPointsRect(screenWidth, screenHeight);
        var below = survivalVisible(player) ? survivalRect(screenWidth, screenHeight).y() : ap.y();
        return new Rect(ap.right() - width, below - STACK_GAP - WEAPON_HEIGHT, width, WEAPON_HEIGHT);
    }

    public static int leftStackTop(int screenWidth, int screenHeight) {
        return healthRect(screenWidth, screenHeight).y() - 4;
    }

    public static Rect compassRect(int screenWidth, int screenHeight, int configuredWidth) {
        var width = Math.min(Math.min(configuredWidth, HOTBAR_HALF * 2), screenWidth - 8);
        var bottomY = screenHeight - COMPASS_BOTTOM;
        if (compact(screenWidth)) {
            bottomY = Math.min(bottomY, healthRect(screenWidth, screenHeight).y() - 2);
        }
        var stackTop = vanillaStackTop(Minecraft.getInstance().player, screenHeight);
        if (stackTop < screenHeight) {
            bottomY = Math.min(bottomY, stackTop - 2);
        }
        return new Rect((screenWidth - width) / 2, bottomY - COMPASS_HEIGHT, width, COMPASS_HEIGHT);
    }

    private static int vanillaStackTop(@Nullable LocalPlayer player, int screenHeight) {
        if (!survivalMode(player) || PowerArmorClient.inPowerArmor()) {
            return screenHeight;
        }
        var left = 0;
        if (!health()) {
            var total = player.getMaxHealth() + player.getAbsorptionAmount();
            var rows = Math.max(1, Mth.ceil(total / 2F / 10F));
            var rowHeight = Math.max(10 - (rows - 2), 3);
            left = 10 + (rows - 1) * rowHeight;
            if (player.getArmorValue() > 0) {
                left += 10;
            }
        } else if (!armor() && player.getArmorValue() > 0) {
            left = 10;
        }
        var right = 0;
        if (player.getVehicle() instanceof LivingEntity mount && mount.showVehicleHealth()) {
            right = 10 * Math.max(1, Mth.ceil(mount.getMaxHealth() / 2F / 10F));
        } else if (!food()) {
            right = 10;
        }
        if (!air() && (player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply())) {
            right += 10;
        }
        var stack = Math.max(left, right);
        return stack == 0 ? screenHeight : screenHeight - VANILLA_ROW_TOP - (stack - 10);
    }

    public static void bar(
        ADGuiGraphics graphics,
        int x,
        int y,
        int width,
        float fraction,
        int color,
        boolean fromRight
    ) {
        frame(graphics, x, y, width, BAR_HEIGHT);
        var fill = Math.round(width * Math.clamp(fraction, 0F, 1F));
        if (fill <= 0) {
            return;
        }
        if (fromRight) {
            graphics.fill(x + width - fill, y, x + width, y + BAR_HEIGHT, color);
        } else {
            graphics.fill(x, y, x + fill, y + BAR_HEIGHT, color);
        }
    }

    public static void frame(ADGuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, shadow());
        graphics.fill(x, y, x + width, y + height, faint());
        var edge = dim();
        graphics.fill(x - 3, y - 2, x - 2, y + height + 2, edge);
        graphics.fill(x - 3, y - 2, x - 1, y - 1, edge);
        graphics.fill(x - 3, y + height + 1, x - 1, y + height + 2, edge);
        graphics.fill(x + width + 2, y - 2, x + width + 3, y + height + 2, edge);
        graphics.fill(x + width + 1, y - 2, x + width + 3, y - 1, edge);
        graphics.fill(x + width + 1, y + height + 1, x + width + 3, y + height + 2, edge);
    }

    public static void segment(ADGuiGraphics graphics, int x, int y, int from, int to, int color) {
        if (to > from) {
            graphics.fill(x + from, y, x + to, y + BAR_HEIGHT, color);
        }
    }

    public static void thin(ADGuiGraphics graphics, int x, int y, int width, float fraction, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 2, shadow());
        graphics.fill(x, y, x + width, y + 1, faint());
        graphics.fill(x, y, x + Math.round(width * Math.clamp(fraction, 0F, 1F)), y + 1, color);
    }

    public static void mini(ADGuiGraphics graphics, int x, int y, int width, float fraction, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 4, shadow());
        graphics.fill(x, y, x + width, y + 3, faint());
        graphics.fill(x, y, x + Math.round(width * Math.clamp(fraction, 0F, 1F)), y + 3, color);
    }

    public static void icon(ADGuiGraphics graphics, String[] rows, int x, int y, int color) {
        paint(graphics, rows, x + 1, y + 1, shadow());
        paint(graphics, rows, x, y, color);
    }

    public static int iconWidth(String[] rows) {
        return rows[0].length();
    }

    private static void paint(ADGuiGraphics graphics, String[] rows, int x, int y, int color) {
        for (var row = 0; row < rows.length; row++) {
            var line = rows[row];
            var start = -1;
            for (var column = 0; column <= line.length(); column++) {
                var solid = column < line.length() && line.charAt(column) == '#';
                if (solid && start < 0) {
                    start = column;
                } else if (!solid && start >= 0) {
                    graphics.fill(x + start, y + row, x + column, y + row + 1, color);
                    start = -1;
                }
            }
        }
    }

    public static void text(ADGuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, true);
    }

    public static void textRight(ADGuiGraphics graphics, Font font, Component text, int right, int y, int color) {
        graphics.text(font, text, right - font.width(text), y, color, true);
    }
}

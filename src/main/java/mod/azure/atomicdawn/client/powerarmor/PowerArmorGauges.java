package mod.azure.atomicdawn.client.powerarmor;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ammunition.AmmoInventory;
import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiRenderUtil;
import mod.azure.atomicdawn.client.helper.ADHudLayer;
import mod.azure.atomicdawn.client.hud.fallout.FalloutHud;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.client.pipboy.Rect;
import mod.azure.atomicdawn.client.vats.VatsClient;
import mod.azure.atomicdawn.config.ADColors;
import mod.azure.atomicdawn.config.ADConfig.Client.Hud.PowerArmorHudStyle;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorSlot;
import mod.azure.atomicdawn.powerarmor.PowerCoreItem;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.thirst.Thirst;
import mod.azure.atomicdawn.thirst.ThirstService;
import mod.azure.atomicdawn.vats.ActionPoints;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class PowerArmorGauges implements ADHudLayer {

    public static final int CLUSTER_WIDTH = 128;

    public static final int CLUSTER_HEIGHT = 58;

    private static final int MARGIN = 6;

    private static final int BIG = 24;

    private static final int SMALL = 15;

    private static final int BEZEL = 2;

    private static final int PANEL_WIDTH = 34;

    private static final int PANEL_HEIGHT = 44;

    private static final float START = (float) Math.toRadians(135.0);

    private static final float SWEEP = (float) Math.toRadians(270.0);

    private static final int FACE = 0x0E0B07;

    private static final int RIM = 0x3C352C;

    private static final int RIM_LIGHT = 0x6A5F50;

    private static final int CRITICAL = 0xFFFFE066;

    private static final int DIGIT_WIDTH = 3;

    private static final int DIGIT_HEIGHT = 5;

    private static final String[] GLYPH_EIGHT = { "###", "#.#", "###", "#.#", "###" };

    private static final int[][] SILHOUETTE = {
        { 12, 0, 8, 7 },
        { 10, 8, 12, 13 },
        { 23, 8, 6, 12 },
        { 3, 8, 6, 12 },
        { 17, 22, 6, 14 },
        { 9, 22, 6, 14 } };

    public static boolean styleSelected() {
        return AtomicDawn.CONFIG.client.hud.powerArmor.POWER_ARMOR_HUD_STYLE == PowerArmorHudStyle.GAUGES;
    }

    public static boolean active() {
        return styleSelected() && PowerArmorClient.inPowerArmor();
    }

    public static float opacity() {
        return Math.clamp(AtomicDawn.CONFIG.client.hud.powerArmor.POWER_ARMOR_HUD_OPACITY / 100F, 0F, 1F);
    }

    public static int backgroundAlpha(int fullAlpha) {
        return Math.round(fullAlpha * opacity());
    }

    private static int color() {
        return ADColors.argb(AtomicDawn.CONFIG.client.hud.powerArmor.POWER_ARMOR_HUD_COLOR, 0xFFFFB547);
    }

    private static int warn() {
        return ADColors.argb(AtomicDawn.CONFIG.client.hud.powerArmor.POWER_ARMOR_HUD_WARN_COLOR, 0xFFFF4A3A);
    }

    private static int dim() {
        return FalloutHud.scale(color(), 0.55F, 0xFF);
    }

    private static int background(int rgb) {
        return (backgroundAlpha(0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    public static float scale(int screenWidth) {
        var space = screenWidth / 2 - FalloutHud.HOTBAR_HALF - MARGIN - 4;
        return Math.clamp(space / (float) CLUSTER_WIDTH, 0.55F, 1F);
    }

    public static Rect leftRect(int screenWidth, int screenHeight) {
        var scale = scale(screenWidth);
        var width = Math.round(CLUSTER_WIDTH * scale);
        var height = Math.round(CLUSTER_HEIGHT * scale);
        return new Rect(MARGIN, screenHeight - MARGIN - height, width, height);
    }

    public static Rect rightRect(int screenWidth, int screenHeight) {
        var scale = scale(screenWidth);
        var width = Math.round(CLUSTER_WIDTH * scale);
        var height = Math.round(CLUSTER_HEIGHT * scale);
        return new Rect(screenWidth - MARGIN - width, screenHeight - MARGIN - height, width, height);
    }

    @Override
    public void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var armor = PowerArmorClient.current();
        if (player == null || armor == null || ADGuiRenderUtil.isHudHidden() || !active()) {
            return;
        }
        var width = ADGuiRenderUtil.guiWidth();
        var height = ADGuiRenderUtil.guiHeight();
        var scale = scale(width);
        var font = minecraft.font;
        var blink = (player.tickCount / 5) % 2 == 0;

        var left = leftRect(width, height);
        if (HudLayout.push(graphics, HudElement.POWER_ARMOR, left.x(), left.y(), left.width(), left.height())) {
            graphics.push();
            graphics.translate(left.x(), left.y());
            graphics.scale(scale, scale);
            drawLeft(graphics, font, player, armor, blink);
            graphics.pop();
            HudLayout.pop(graphics);
        }

        var right = rightRect(width, height);
        if (
            HudLayout.push(graphics, HudElement.POWER_ARMOR_GAUGES, right.x(), right.y(), right.width(), right.height())
        ) {
            graphics.push();
            graphics.translate(right.x(), right.y());
            graphics.scale(scale, scale);
            drawRight(graphics, font, minecraft, player, armor, blink);
            graphics.pop();
            HudLayout.pop(graphics);
        }
    }

    private static void drawLeft(
        ADGuiGraphics graphics,
        Font font,
        LocalPlayer player,
        PowerArmorEntity armor,
        boolean blink
    ) {
        panel(graphics, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        var missing = 0;
        var broken = 0;
        for (var slot : PowerArmorSlot.all()) {
            var box = SILHOUETTE[slot.ordinal()];
            var piece = armor.getPiece(slot);
            int fill;
            if (piece.isEmpty()) {
                fill = 0x602A2A2A;
                missing++;
            } else if (PowerArmorPieceItem.isBroken(piece)) {
                fill = blink ? warn() : FalloutHud.scale(warn(), 0.4F, 0xFF);
                broken++;
            } else {
                var condition = PowerArmorPieceItem.condition(piece);
                fill = condition > 0.6F ? color() : condition > 0.25F ? dim() : warn();
            }
            graphics.fill(1 + box[0], 4 + box[1], 1 + box[0] + box[2], 4 + box[1] + box[3], fill);
        }

        var health = player.getHealth();
        var max = Math.max(1F, player.getMaxHealth());
        var low = health / max <= 0.25F;
        var hpX = PANEL_WIDTH + 4 + BIG + BEZEL;
        var hpY = CLUSTER_HEIGHT - BIG - BEZEL - 1;
        dial(
            graphics,
            font,
            hpX,
            hpY,
            BIG,
            health / max,
            Component.translatable("power_armor.atomicdawn.hud.hp_percent"),
            low && blink ? warn() : color(),
            true
        );

        var rads = player.getData(ADAttachments.RADIATION.get()).rads();
        var radX = hpX + BIG + BEZEL + 4 + SMALL + BEZEL;
        var radY = CLUSTER_HEIGHT - SMALL - BEZEL - 1;
        dial(
            graphics,
            font,
            radX,
            radY,
            SMALL,
            rads / RadiationService.maxRads(),
            Component.translatable("power_armor.atomicdawn.hud.rads"),
            warn(),
            false
        );
        if (rads >= 0.5F) {
            small(graphics, font, Component.literal(Integer.toString(Math.round(rads))), radX, radY + 5, warn());
        }

        Component status = null;
        if (armor.getCore().isEmpty() || !armor.isFunctioning()) {
            status = Component.translatable("power_armor.atomicdawn.hud.no_power");
        } else if (armor.landingTicks() > 0) {
            status = Component.translatable("power_armor.atomicdawn.hud.landing");
        } else if (missing > 0 || broken > 0) {
            status = Component.translatable("power_armor.atomicdawn.hud.pieces", missing, broken);
        }
        if (status != null && (blink || armor.isFunctioning())) {
            graphics.text(font, status, 0, -font.lineHeight - 2, warn(), true);
        }
    }

    private static void drawRight(
        ADGuiGraphics graphics,
        Font font,
        Minecraft minecraft,
        LocalPlayer player,
        PowerArmorEntity armor,
        boolean blink
    ) {
        var apX = CLUSTER_WIDTH - BIG - BEZEL;
        var apY = CLUSTER_HEIGHT - BIG - BEZEL - 1;
        var coreX = apX - BIG - BEZEL - 4 - SMALL - BEZEL;
        var coreY = CLUSTER_HEIGHT - SMALL - BEZEL - 1;
        var core = armor.getCore();
        var coreFraction = core.isEmpty() ? 0F : PowerCoreItem.fraction(core);
        var coreLow = core.isEmpty() || coreFraction < 0.15F;
        dial(
            graphics,
            font,
            coreX,
            coreY,
            SMALL,
            coreFraction,
            Component.translatable("power_armor.atomicdawn.hud.core"),
            coreLow && blink ? warn() : color(),
            false
        );
        digital(
            graphics,
            String.format(Locale.ROOT, "%02d", Math.min(99, spareCores(player))),
            coreX,
            coreY + 4,
            coreLow ? warn() : color()
        );

        var survivalX = coreX - 22;
        var survivalY = 2;
        if (FalloutHud.survivalMode(player)) {
            var food = player.getFoodData().getFoodLevel();
            meter(
                graphics,
                FalloutHud.ICON_FOOD,
                survivalX,
                survivalY,
                food / 20F,
                food <= 6 && blink ? warn() : color()
            );
            survivalY += 10;
            if (ThirstService.enabled()) {
                var thirst = ThirstService.thirst(player);
                meter(
                    graphics,
                    FalloutHud.ICON_WATER,
                    survivalX,
                    survivalY,
                    thirst / Thirst.MAX,
                    thirst <= 6F && blink ? warn() : color()
                );
            }
        }

        var ap = VatsClient.actionPoints(player);
        var maxAp = Math.max(1F, VatsActions.maxActionPoints(player));
        var definition = minecraft.level == null ? null : Weapons.definition(minecraft.level, player.getMainHandItem());
        var cost = definition == null ? 0F : VatsActions.apCost(player, definition);
        var lacking = definition != null && !player.hasInfiniteMaterials() && ap.points() < cost;
        dial(
            graphics,
            font,
            apX,
            apY,
            BIG,
            ap.points() / maxAp,
            Component.translatable("power_armor.atomicdawn.hud.ap_percent"),
            lacking && blink ? warn() : color(),
            true
        );
        if (definition != null && ap.critical() > 0F) {
            var critical = ap.criticalReady() ? CRITICAL : FalloutHud.scale(CRITICAL, 0.55F, 0xFF);
            arc(graphics, apX, apY, BIG - 11, ap.critical() / ActionPoints.CRITICAL_FULL, critical);
        }
        if (definition != null && definition.usesAmmo()) {
            var stack = player.getMainHandItem();
            var loaded = Weapons.state(stack, definition).loadedAmmo();
            var reserve = AmmoInventory.hasInfiniteAmmo(player)
                ? "---"
                : String.format(
                    Locale.ROOT,
                    "%03d",
                    Math.min(999, AmmoInventory.count(player, definition.ammo().item()))
                );
            var lowAmmo = loaded <= Math.max(1, definition.ammo().magazineSize() / 4);
            digital(
                graphics,
                String.format(Locale.ROOT, "%03d", Math.min(999, loaded)) + "/" + reserve,
                apX,
                apY + 7,
                lowAmmo && blink ? warn() : color()
            );
        }
    }

    private static int spareCores(LocalPlayer player) {
        var inventory = player.getInventory();
        var count = 0;
        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.getItem() instanceof PowerCoreItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void meter(ADGuiGraphics graphics, String[] icon, int x, int y, float fraction, int color) {
        graphics.fill(x - 1, y - 1, x + 44, y + 8, background(FACE));
        FalloutHud.icon(graphics, icon, x + 1, y, color);
        var barX = x + 11;
        graphics.fill(barX, y + 2, barX + 30, y + 5, FalloutHud.scale(color, 0.3F, 0xFF));
        graphics.fill(barX, y + 2, barX + Math.round(30 * Math.clamp(fraction, 0F, 1F)), y + 5, color);
    }

    private static void panel(ADGuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, background(RIM));
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, background(RIM_LIGHT));
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, background(FACE));
    }

    private static void dial(
        ADGuiGraphics graphics,
        Font font,
        int cx,
        int cy,
        int radius,
        float fraction,
        Component label,
        int needle,
        boolean numbers
    ) {
        ring(graphics, cx, cy, radius, radius + BEZEL, background(RIM));
        ring(graphics, cx, cy, radius + BEZEL - 1, radius + BEZEL, background(RIM_LIGHT));
        disc(graphics, cx, cy, radius, background(FACE));
        var ticks = numbers ? 20 : 10;
        for (var i = 0; i <= ticks; i++) {
            var major = i % (ticks / 5) == 0;
            var angle = START + SWEEP * i / ticks;
            line(graphics, cx, cy, angle, radius - (major ? 4 : 2), radius - 1, major ? color() : dim());
            if (numbers && major) {
                var value = Component.literal(Integer.toString(i * 100 / ticks));
                var tx = cx + (int) Math.round(Math.cos(angle) * (radius - 8));
                var ty = cy + (int) Math.round(Math.sin(angle) * (radius - 8));
                small(graphics, font, value, tx, ty - 1, dim());
            }
        }
        small(graphics, font, label, cx, cy - (radius >= 20 ? 10 : 8), color());
        var clamped = Math.clamp(fraction, 0F, 1F);
        var angle = START + SWEEP * clamped;
        line(graphics, cx, cy, angle, 0, radius - 3, 0xFF000000 | (needle & 0xFFFFFF));
        line(graphics, cx + 1, cy, angle, 0, radius - 4, FalloutHud.scale(needle, 0.6F, 0xFF));
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, RIM_LIGHT | 0xFF000000);
        graphics.fill(cx, cy, cx + 1, cy + 1, needle);
    }

    private static void arc(ADGuiGraphics graphics, int cx, int cy, int radius, float fraction, int color) {
        var end = START + SWEEP * Math.clamp(fraction, 0F, 1F);
        var step = 1F / Math.max(8F, radius);
        for (var angle = START; angle <= end; angle += step) {
            var x = cx + (int) Math.round(Math.cos(angle) * radius);
            var y = cy + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void line(
        ADGuiGraphics graphics,
        int cx,
        int cy,
        float angle,
        int from,
        int to,
        int color
    ) {
        var cos = Math.cos(angle);
        var sin = Math.sin(angle);
        var lastX = Integer.MIN_VALUE;
        var lastY = Integer.MIN_VALUE;
        for (var step = from * 2; step <= to * 2; step++) {
            var distance = step / 2.0;
            var x = cx + (int) Math.round(cos * distance);
            var y = cy + (int) Math.round(sin * distance);
            if (x != lastX || y != lastY) {
                graphics.fill(x, y, x + 1, y + 1, color);
                lastX = x;
                lastY = y;
            }
        }
    }

    private static void disc(ADGuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (var dy = -radius; dy <= radius; dy++) {
            var dx = (int) Math.floor(Math.sqrt(radius * radius - dy * dy + radius * 0.8));
            graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static void ring(ADGuiGraphics graphics, int cx, int cy, int inner, int outer, int color) {
        for (var dy = -outer; dy <= outer; dy++) {
            var dxOuter = (int) Math.floor(Math.sqrt(outer * outer - dy * dy + outer * 0.8));
            if (Math.abs(dy) > inner) {
                graphics.fill(cx - dxOuter, cy + dy, cx + dxOuter + 1, cy + dy + 1, color);
                continue;
            }
            var dxInner = (int) Math.floor(Math.sqrt(inner * inner - dy * dy + inner * 0.8));
            graphics.fill(cx - dxOuter, cy + dy, cx - dxInner, cy + dy + 1, color);
            graphics.fill(cx + dxInner + 1, cy + dy, cx + dxOuter + 1, cy + dy + 1, color);
        }
    }

    private static void digital(ADGuiGraphics graphics, String text, int cx, int y, int color) {
        var width = text.length() * (DIGIT_WIDTH + 1) - 1;
        var left = cx - width / 2 - 2;
        graphics.fill(left - 1, y - 1, left + width + 5, y + DIGIT_HEIGHT + 5, background(RIM));
        graphics.fill(
            left,
            y,
            left + width + 4,
            y + DIGIT_HEIGHT + 4,
            (Math.max(backgroundAlpha(0xFF), 0x80) << 24) | FACE
        );
        var dim = FalloutHud.scale(color, 0.22F, 0xFF);
        var x = left + 2;
        for (var i = 0; i < text.length(); i++) {
            var glyph = glyph(text.charAt(i));
            if (Character.isDigit(text.charAt(i))) {
                paintGlyph(graphics, GLYPH_EIGHT, x, y + 2, dim);
            }
            paintGlyph(graphics, glyph, x, y + 2, color);
            x += DIGIT_WIDTH + 1;
        }
    }

    private static String[] glyph(char character) {
        return switch (character) {
            case '0' -> new String[] { "###", "#.#", "#.#", "#.#", "###" };
            case '1' -> new String[] { "..#", "..#", "..#", "..#", "..#" };
            case '2' -> new String[] { "###", "..#", "###", "#..", "###" };
            case '3' -> new String[] { "###", "..#", "###", "..#", "###" };
            case '4' -> new String[] { "#.#", "#.#", "###", "..#", "..#" };
            case '5' -> new String[] { "###", "#..", "###", "..#", "###" };
            case '6' -> new String[] { "###", "#..", "###", "#.#", "###" };
            case '7' -> new String[] { "###", "..#", "..#", "..#", "..#" };
            case '8' -> GLYPH_EIGHT;
            case '9' -> new String[] { "###", "#.#", "###", "..#", "###" };
            case '/' -> new String[] { "..#", "..#", ".#.", "#..", "#.." };
            case '-' -> new String[] { "...", "...", "###", "...", "..." };
            default -> new String[] { "...", "...", "...", "...", "..." };
        };
    }

    private static void paintGlyph(ADGuiGraphics graphics, String[] rows, int x, int y, int color) {
        for (var row = 0; row < rows.length; row++) {
            var line = rows[row];
            for (var column = 0; column < line.length(); column++) {
                if (line.charAt(column) == '#') {
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }
    }

    private static void small(ADGuiGraphics graphics, Font font, Component text, int cx, int y, int color) {
        graphics.push();
        graphics.translate(cx, y);
        graphics.scale(0.5F, 0.5F);
        graphics.text(font, text, -font.width(text) / 2, 0, color, false);
        graphics.pop();
    }
}

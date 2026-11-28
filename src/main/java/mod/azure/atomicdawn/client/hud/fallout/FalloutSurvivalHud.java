package mod.azure.atomicdawn.client.hud.fallout;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiRenderUtil;
import mod.azure.atomicdawn.client.helper.ADHudLayer;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.thirst.Thirst;
import mod.azure.atomicdawn.thirst.ThirstService;

public final class FalloutSurvivalHud implements ADHudLayer {

    private static final int COLOR_HUNGER = 0xFF8DB33F;

    private static final int COLOR_SATURATION = 0x60FFFFFF;

    private static final int ITEM_GAP = 8;

    private static final int MAX_METER = 30;

    private static final float LOW = 6F;

    @Override
    public void draw(ADGuiGraphics graphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || ADGuiRenderUtil.isHudHidden() || !FalloutHud.survivalVisible(player)) {
            return;
        }
        var showFood = FalloutHud.food();
        var showWater = FalloutHud.thirst() && ThirstService.enabled();
        var rect = FalloutHud.survivalRect(ADGuiRenderUtil.guiWidth(), ADGuiRenderUtil.guiHeight());
        if (!HudLayout.push(graphics, HudElement.SURVIVAL, rect.x(), rect.y(), rect.width(), rect.height())) {
            return;
        }
        var count = (showFood ? 1 : 0) + (showWater ? 1 : 0);
        var iconWidth = 8;
        var available = (rect.width() - FalloutHud.LABEL_WIDTH - FalloutHud.LABEL_GAP - ITEM_GAP * (count - 1)) / count
            - iconWidth - 3;
        var meter = Math.clamp(available, 12, MAX_METER);
        var itemWidth = iconWidth + 3 + meter;
        var right = rect.right() - FalloutHud.LABEL_WIDTH - FalloutHud.LABEL_GAP;
        var x = right - count * itemWidth - (count - 1) * ITEM_GAP;
        var y = rect.y();
        var blink = FalloutHud.blink(player);
        if (showFood) {
            var food = player.getFoodData();
            var level = food.getFoodLevel();
            var hungry = player.hasEffect(MobEffects.HUNGER);
            var color = hungry ? COLOR_HUNGER : level <= LOW && blink ? FalloutHud.warn() : FalloutHud.primary();
            FalloutHud.icon(graphics, FalloutHud.ICON_FOOD, x, y + 1, color);
            var meterX = x + iconWidth + 3;
            var meterY = y + 3;
            FalloutHud.mini(graphics, meterX, meterY, meter, level / 20F, color);
            var saturation = Math.round(meter * Math.clamp(food.getSaturationLevel() / 20F, 0F, 1F));
            if (saturation > 0) {
                graphics.fill(meterX, meterY, meterX + saturation, meterY + 1, COLOR_SATURATION);
            }
            x += itemWidth + ITEM_GAP;
        }
        if (showWater) {
            var thirst = ThirstService.thirst(player);
            var color = thirst <= LOW && blink ? FalloutHud.warn() : FalloutHud.primary();
            FalloutHud.icon(graphics, FalloutHud.ICON_WATER, x + 1, y + 1, color);
            FalloutHud.mini(graphics, x + iconWidth + 3, y + 3, meter, thirst / Thirst.MAX, color);
        }
        HudLayout.pop(graphics);
    }
}

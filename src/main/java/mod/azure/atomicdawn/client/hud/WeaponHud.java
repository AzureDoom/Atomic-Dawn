package mod.azure.atomicdawn.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.ammunition.AmmoInventory;
import mod.azure.atomicdawn.client.ClientWeaponState;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.client.weapon.ScopeClient;
import mod.azure.atomicdawn.weapon.action.WeaponActions;
import mod.azure.atomicdawn.weapon.combat.SpreadModel;
import mod.azure.atomicdawn.weapon.instance.Weapons;

public final class WeaponHud implements GuiLayer {

    private static final int COLOR = 0xFF7CFC9A;

    private static final int COLOR_DIM = 0xB07CFC9A;

    private static final int COLOR_WARN = 0xFFFFB347;

    private static final int COLOR_BACKGROUND = 0x80000000;

    private static final int COLOR_BAR_BG = 0x60FFFFFF;

    private static final int COLOR_BROKEN = 0xFFFF5050;

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        if (player == null || minecraft.level == null || hudHidden || player.isSpectator()) {
            return;
        }
        var stack = player.getMainHandItem();
        var definition = Weapons.definition(minecraft.level, stack);
        if (definition == null) {
            return;
        }

        var window = minecraft.getWindow();
        var width = window.getGuiScaledWidth();
        var height = window.getGuiScaledHeight();
        var font = minecraft.font;
        var state = Weapons.state(stack, definition);

        var modeText = Component.translatable(state.fireMode().translationKey());
        Component ammoText = Component.empty();
        Component reserveText = Component.empty();
        var lowAmmo = false;
        if (definition.usesAmmo()) {
            var loaded = state.loadedAmmo();
            var reserve = AmmoInventory.hasInfiniteAmmo(player)
                ? "\u221E"
                : Integer.toString(AmmoInventory.count(player, definition.ammo().item()));
            lowAmmo = loaded <= Math.max(1, definition.ammo().magazineSize() / 4);
            ammoText = Component.literal(Integer.toString(loaded));
            reserveText = Component.literal(" / " + reserve);
        }
        var condition = Weapons.condition(stack, definition);
        var showCondition = definition.condition().enabled();

        var ammoWidth = font.width(ammoText) + font.width(reserveText);
        var blockWidth = Math.max(Math.max(ammoWidth, font.width(modeText)), showCondition ? 40 : 0) + 8;
        var lines = definition.usesAmmo() ? 2 : 1;
        var blockHeight = font.lineHeight * lines + 8 + (showCondition ? 5 : 0);
        var x = width - blockWidth - 8;
        var y = height - blockHeight - 8;

        if (HudLayout.push(graphics, HudElement.WEAPON, x, y, blockWidth, blockHeight)) {
            graphics.fill(x, y, x + blockWidth, y + blockHeight, COLOR_BACKGROUND);
            var lineY = y + 4;
            if (definition.usesAmmo()) {
                var textX = x + blockWidth - 4 - ammoWidth;
                graphics.text(font, ammoText, textX, lineY, lowAmmo ? COLOR_WARN : COLOR, true);
                graphics.text(font, reserveText, textX + font.width(ammoText), lineY, COLOR_DIM, true);
                lineY += font.lineHeight + 2;
            }
            graphics.text(font, modeText, x + blockWidth - 4 - font.width(modeText), lineY, COLOR_DIM, true);
            if (showCondition) {
                var barY = y + blockHeight - 6;
                var barWidth = blockWidth - 8;
                var fill = Math.round(barWidth * condition.fraction());
                var color = condition.isBroken() ? COLOR_BROKEN : condition.fraction() < 0.25F ? COLOR_WARN : COLOR;
                graphics.fill(x + 4, barY, x + 4 + barWidth, barY + 2, COLOR_BAR_BG);
                graphics.fill(x + 4, barY, x + 4 + fill, barY + 2, color);
            }
            HudLayout.pop(graphics);
        }

        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        var centerX = width / 2;
        var centerY = height / 2;

        var progress = ClientWeaponState.reloadProgress();
        if (progress >= 0F) {
            var barWidth = 40;
            var barX = centerX - barWidth / 2;
            var barY = centerY + 12;
            graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 3, COLOR_BACKGROUND);
            graphics.fill(barX, barY, barX + Math.round(barWidth * progress), barY + 2, COLOR);
            return;
        }

        var held = ClientWeaponState.triggerHeldTicks(ClientWeaponState.now());
        if (held > 0) {
            var total = switch (state.fireMode()) {
                case CHARGED -> definition.firing().charge().ticks();
                case SPIN_UP -> Math.max(1, definition.firing().spinUpTicks());
                default -> 1;
            };
            var fraction = (float) Math.min(1.0, held / total);
            var barWidth = 40;
            var barX = centerX - barWidth / 2;
            var barY = centerY + 12;
            graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 3, COLOR_BACKGROUND);
            graphics.fill(
                barX,
                barY,
                barX + Math.round(barWidth * fraction),
                barY + 2,
                fraction >= 1F ? COLOR_WARN : COLOR
            );
        }

        if (definition.isMelee() || ScopeClient.scoped()) {
            return;
        }

        var aiming = WeaponActions.isAiming(player, stack);
        var cone = SpreadModel.coneDegrees(player, definition, ClientWeaponState.bloom(), aiming);
        var fov = Math.toRadians(minecraft.options.fov().get());
        var gap = (int) Math.round((height / 2.0) * Math.tan(Math.toRadians(cone)) / Math.tan(fov / 2.0));
        gap = Math.clamp(gap, 3, height / 4);
        drawBrackets(graphics, centerX, centerY, gap);
    }

    private static void drawBrackets(GuiGraphicsExtractor graphics, int cx, int cy, int gap) {
        var length = 3;
        graphics.fill(cx - gap - length, cy, cx - gap, cy + 1, COLOR_DIM);
        graphics.fill(cx + gap + 1, cy, cx + gap + 1 + length, cy + 1, COLOR_DIM);
        graphics.fill(cx, cy - gap - length, cx + 1, cy - gap, COLOR_DIM);
        graphics.fill(cx, cy + gap + 1, cx + 1, cy + gap + 1 + length, COLOR_DIM);
    }
}

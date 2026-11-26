package mod.azure.atomicdawn.client.limb;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.hud.layout.HudElement;
import mod.azure.atomicdawn.client.hud.layout.HudLayout;
import mod.azure.atomicdawn.limb.LimbService;
import mod.azure.atomicdawn.limb.LimbState;
import mod.azure.atomicdawn.vats.BodyPart;

public final class LimbHud implements GuiLayer {

    private static final int UNIT = 2;

    private static final int WIDTH = 8 * UNIT + 4;

    private static final int HEIGHT = 16 * UNIT + 4;

    private static final int BACKGROUND = 0x80000000;

    @Override
    public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var hudHidden = minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        if (
            player == null || hudHidden || player.isSpectator() || !AtomicDawn.CONFIG.client.hud.limbs.LIMB_HUD
                || !AtomicDawn.CONFIG.common.combat.limbs.general.LIMBS_ENABLED
        ) {
            return;
        }
        var state = LimbService.state(player);
        if (state.healthy() && !AtomicDawn.CONFIG.client.hud.limbs.LIMB_HUD_ALWAYS) {
            return;
        }
        var width = minecraft.getWindow().getGuiScaledWidth();
        var height = minecraft.getWindow().getGuiScaledHeight();
        var x = width / 2 - 173 - 4 - WIDTH;
        var y = height - HEIGHT - 2;
        if (!HudLayout.push(graphics, HudElement.LIMBS, x, y, WIDTH, HEIGHT)) {
            return;
        }
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, BACKGROUND);
        var ox = x + 2;
        var oy = y + 2;
        part(graphics, state, BodyPart.HEAD, ox, oy, 2, 0, 4, 4);
        part(graphics, state, BodyPart.TORSO, ox, oy, 2, 4, 4, 6);
        part(graphics, state, BodyPart.RIGHT_ARM, ox, oy, 0, 4, 2, 6);
        part(graphics, state, BodyPart.LEFT_ARM, ox, oy, 6, 4, 2, 6);
        part(graphics, state, BodyPart.RIGHT_LEG, ox, oy, 2, 10, 2, 6);
        part(graphics, state, BodyPart.LEFT_LEG, ox, oy, 4, 10, 2, 6);
        HudLayout.pop(graphics);
    }

    private static void part(
        GuiGraphicsExtractor graphics,
        LimbState state,
        BodyPart part,
        int ox,
        int oy,
        int x,
        int y,
        int w,
        int h
    ) {
        var left = ox + x * UNIT;
        var top = oy + y * UNIT;
        graphics.fill(left, top, left + w * UNIT, top + h * UNIT, color(state.get(part)));
        graphics.fill(left, top, left + w * UNIT, top + 1, 0x40FFFFFF);
    }

    public static int color(float condition) {
        if (condition <= 0F) {
            var pulse = (System.currentTimeMillis() / 400L) % 2L == 0L;
            return pulse ? 0xFFE03A2A : 0xFF8A1E16;
        }
        if (condition < 0.35F) {
            return 0xFFE0782A;
        }
        if (condition < 0.7F) {
            return 0xFFE0D02A;
        }
        return 0xFF5ADC4A;
    }
}

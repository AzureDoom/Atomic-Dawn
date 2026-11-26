package mod.azure.atomicdawn.client.lock;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import mod.azure.atomicdawn.client.helper.ADGuiGraphics;
import mod.azure.atomicdawn.client.helper.ADGuiInput;
import mod.azure.atomicdawn.client.helper.ADScreen;
import mod.azure.atomicdawn.client.pipboy.PipBoyDraw;
import mod.azure.atomicdawn.client.pipboy.PipBoyTheme;
import mod.azure.atomicdawn.lock.BobbyPins;
import mod.azure.atomicdawn.lock.LockLevel;
import mod.azure.atomicdawn.network.LockpickActionPayload;
import mod.azure.atomicdawn.network.LockpickStatePayload;

public final class LockpickScreen extends ADScreen {

    private static final float TURN_SPEED = 0.08F;

    private static final float RETURN_SPEED = 0.15F;

    private static final float OPEN_ANGLE = 90F;

    private static final float SCREWDRIVER_ANGLE = 150F;

    private static final int STRAIN_INTERVAL = 4;

    private static final int BROKEN_TICKS = 20;

    private static final int FINISH_TICKS = 16;

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private static final int PLATE = 0xFF1E1D1A;

    private static final int PLATE_EDGE = 0xFF4E4B43;

    private static final int FACE = 0xFF3A3832;

    private static final int FACE_EDGE = 0xFF6B675C;

    private static final int CYLINDER = 0xFFA8873A;

    private static final int CYLINDER_EDGE = 0xFF6A5222;

    private static final int KEYHOLE = 0xFF141210;

    private static final int SHAFT = 0xFFB9BEC4;

    private static final int HANDLE = 0xFF8E2A20;

    private static final int HANDLE_SHINE = 0xFFB5392B;

    private static final int PIN = 0xD9B65A;

    private static final int PIN_WORN = 0xD9472B;

    private final BlockPos pos;

    private final RandomSource random = RandomSource.create();

    private int pins;

    private float pinHealth = 1F;

    private float pinAngle;

    private float turn;

    private float prevTurn;

    private float maxTurn = -1F;

    private boolean turning;

    private boolean openSent;

    private boolean straining;

    private boolean finished;

    private boolean closed;

    private int ticks;

    private int strainTicks;

    private int brokenTicks;

    private int finishTicks;

    public LockpickScreen(BlockPos pos, LockLevel level, int pins) {
        super(Component.translatable("screen.atomicdawn.lockpick.title", level.component()));
        this.pos = pos;
        this.pins = pins;
    }

    public void onState(LockpickStatePayload payload) {
        if (finished || closed) {
            return;
        }
        pins = payload.pins();
        pinHealth = payload.pinHealth();
        switch (payload.status()) {
            case PROBE -> {
                if (turning && maxTurn < 0F) {
                    maxTurn = payload.maxTurn();
                }
            }
            case STRAIN -> {}
            case PIN_BROKEN -> {
                release();
                brokenTicks = BROKEN_TICKS;
                turn = 0F;
                prevTurn = 0F;
            }
            case REJECTED -> release();
            case SUCCESS -> {
                finished = true;
                turning = false;
                straining = false;
            }
            case FAILED -> {
                closed = true;
                onClose();
            }
        }
    }

    private void release() {
        turning = false;
        straining = false;
        openSent = false;
        maxTurn = -1F;
        strainTicks = 0;
    }

    private int centerX() {
        return width / 2;
    }

    private int centerY() {
        return 20 + (height - 70) / 2;
    }

    private int radius() {
        return Math.clamp(Math.min(width / 5, (height - 120) / 4), 36, 80);
    }

    private float angleFrom(double mouseX, double mouseY) {
        var dx = mouseX - centerX();
        var dy = mouseY - centerY();
        if (dx * dx + dy * dy < 4.0) {
            return pinAngle;
        }
        return Math.clamp((float) Math.toDegrees(Math.atan2(dx, -dy)), -90F, 90F);
    }

    private static float[] point(float x, float y, float degrees, float length) {
        var radians = degrees * Mth.DEG_TO_RAD;
        return new float[] { x + Mth.sin(radians) * length, y - Mth.cos(radians) * length };
    }

    private void playUi(SoundEvent sound, float pitch, float volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        prevTurn = turn;
        if (minecraft == null || minecraft.player == null) {
            onClose();
            return;
        }
        if (minecraft.player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) {
            onClose();
            return;
        }
        if (brokenTicks > 0) {
            brokenTicks--;
        }
        if (finished) {
            turn = Math.min(1F, turn + TURN_SPEED);
            if (++finishTicks >= FINISH_TICKS) {
                onClose();
            }
            return;
        }
        if (turning && maxTurn >= 0F) {
            if (turn < maxTurn) {
                straining = false;
                turn = Math.min(maxTurn, turn + TURN_SPEED);
                if (ticks % 3 == 0) {
                    playUi(SoundEvents.TRIPWIRE_CLICK_ON, 1.8F + random.nextFloat() * 0.2F, 0.25F);
                }
            } else if (maxTurn >= 1F) {
                straining = false;
                if (!openSent) {
                    openSent = true;
                    LockpickClient.send(LockpickActionPayload.Action.OPEN, pinAngle);
                }
            } else {
                straining = true;
                if (strainTicks % STRAIN_INTERVAL == 0) {
                    LockpickClient.send(LockpickActionPayload.Action.STRAIN, pinAngle);
                }
                if (strainTicks % 6 == 0) {
                    playUi(SoundEvents.CHEST_LOCKED, 1.7F, 0.2F);
                }
                strainTicks++;
            }
        } else if (!turning) {
            straining = false;
            strainTicks = 0;
            turn = Math.max(0F, turn - RETURN_SPEED);
        }
    }

    @Override
    public boolean mouseClicked(ADGuiInput.Mouse event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (!turning && !finished && brokenTicks == 0 && turn <= 0.05F) {
                pinAngle = angleFrom(event.x(), event.y());
                turning = true;
                openSent = false;
                maxTurn = -1F;
                strainTicks = 0;
                LockpickClient.send(LockpickActionPayload.Action.PROBE, pinAngle);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(ADGuiInput.Mouse event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (turning && !openSent) {
                release();
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        if (!finished && !closed) {
            closed = true;
            LockpickClient.send(LockpickActionPayload.Action.CANCEL, 0F);
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void drawBackground(ADGuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.drawBackground(graphics, mouseX, mouseY, partialTick);
        if (!turning && !finished && brokenTicks == 0) {
            pinAngle = angleFrom(mouseX, mouseY);
        }
        var cx = centerX();
        var cy = centerY();
        var r = radius();
        var progress = Mth.lerp(partialTick, prevTurn, turn);
        var shake = straining ? Mth.sin((ticks + partialTick) * 2.6F) * 2.2F : 0F;
        var rotation = progress * OPEN_ANGLE + shake;

        drawLock(graphics, cx, cy, r, rotation);
        drawScrewdriver(graphics, cx, cy, r, rotation);
        if (brokenTicks == 0) {
            drawPin(graphics, cx, cy, r, rotation);
        }
        drawText(graphics, cx, cy, r);
    }

    private void drawLock(ADGuiGraphics graphics, int cx, int cy, int r, float rotation) {
        PipBoyDraw.disc(graphics, cx, cy, r + 12, PLATE);
        PipBoyDraw.ring(graphics, cx, cy, r + 12, 2, PLATE_EDGE);
        for (var corner = 0; corner < 4; corner++) {
            var screw = point(cx, cy, 45F + corner * 90F, r + 6F);
            PipBoyDraw.screw(graphics, Math.round(screw[0]) - 2, Math.round(screw[1]) - 2);
        }
        PipBoyDraw.disc(graphics, cx, cy, r, FACE);
        PipBoyDraw.ring(graphics, cx, cy, r, 3, FACE_EDGE);
        var cylinder = Math.round(r * 0.62F);
        PipBoyDraw.disc(graphics, cx, cy, cylinder, CYLINDER);
        PipBoyDraw.ring(graphics, cx, cy, cylinder, 2, CYLINDER_EDGE);
        graphics.push();
        graphics.translate(cx, cy);
        graphics.rotate(rotation * Mth.DEG_TO_RAD);
        var slotHalf = Math.max(2, cylinder / 9);
        graphics.fill(-slotHalf, -Math.round(cylinder * 0.15F), slotHalf, Math.round(cylinder * 0.6F), KEYHOLE);
        PipBoyDraw.disc(graphics, 0, -Math.round(cylinder * 0.2F), Math.max(3, cylinder / 5), KEYHOLE);
        graphics.pop();
    }

    private void drawScrewdriver(ADGuiGraphics graphics, int cx, int cy, int r, float rotation) {
        var angle = SCREWDRIVER_ANGLE + rotation;
        var shaft = point(cx, cy, angle, r * 1.05F);
        var handle = point(cx, cy, angle, r * 1.6F);
        PipBoyDraw.line(graphics, cx, cy, shaft[0], shaft[1], 3, SHAFT);
        PipBoyDraw.line(graphics, shaft[0], shaft[1], handle[0], handle[1], 9, HANDLE);
        PipBoyDraw.line(graphics, shaft[0], shaft[1], handle[0], handle[1], 3, HANDLE_SHINE);
    }

    private void drawPin(ADGuiGraphics graphics, int cx, int cy, int r, float rotation) {
        var jitter = straining ? (random.nextFloat() - 0.5F) * 3F : 0F;
        var angle = pinAngle + rotation + jitter;
        var color = 0xFF000000 | PipBoyDraw.blend(PIN, PIN_WORN, Math.clamp(1F - pinHealth, 0F, 1F));
        var shadow = 0xFF000000 | PipBoyDraw.blend(color, 0x000000, 0.45F);
        var tip = point(cx, cy, angle, r * 1.5F);
        var strand = point(cx, cy, angle - 3.5F, r * 1.38F);
        PipBoyDraw.line(graphics, cx, cy, strand[0], strand[1], 1, shadow);
        PipBoyDraw.line(graphics, cx, cy, tip[0], tip[1], 2, color);
        var bend = point(tip[0], tip[1], angle - 150F, r * 0.14F);
        PipBoyDraw.line(graphics, tip[0], tip[1], bend[0], bend[1], 2, color);
        var head = point(tip[0], tip[1], angle + 90F, 3F);
        PipBoyDraw.line(graphics, tip[0], tip[1], head[0], head[1], 2, color);
    }

    private void drawText(ADGuiGraphics graphics, int cx, int cy, int r) {
        centered(graphics, title, cy - r - 30, PipBoyTheme.text());
        if (finished) {
            centered(
                graphics,
                Component.translatable("screen.atomicdawn.lockpick.unlocked"),
                cy - r - 18,
                PipBoyTheme.text()
            );
        } else if (brokenTicks > 0) {
            centered(
                graphics,
                Component.translatable("screen.atomicdawn.lockpick.broken"),
                cy - r - 18,
                PipBoyTheme.bad()
            );
        }

        var pinsText = Component.literal(pins == BobbyPins.INFINITE ? "\u221E" : Integer.toString(pins));
        centered(
            graphics,
            Component.translatable("screen.atomicdawn.lockpick.pins", pinsText),
            height - 58,
            PipBoyTheme.text()
        );
        var barWidth = 80;
        var barLeft = cx - barWidth / 2;
        var barTop = height - 46;
        graphics.fill(barLeft - 1, barTop - 1, barLeft + barWidth + 1, barTop + 4, PipBoyTheme.border());
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 3, PipBoyTheme.panel());
        var filled = Math.round(barWidth * Math.clamp(pinHealth, 0F, 1F));
        graphics.fill(
            barLeft,
            barTop,
            barLeft + filled,
            barTop + 3,
            pinHealth < 0.35F ? PipBoyTheme.bad() : PipBoyTheme.text()
        );

        hint(graphics, "pin", height - 34);
        hint(graphics, "turn", height - 24);
        hint(graphics, "exit", height - 14);
    }

    private void hint(ADGuiGraphics graphics, String key, int y) {
        centered(graphics, Component.translatable("screen.atomicdawn.lockpick.hint." + key), y, PipBoyTheme.dim());
    }

    private void centered(ADGuiGraphics graphics, Component text, int y, int color) {
        graphics.text(font, text, width / 2 - font.width(text) / 2, y, color, false);
    }
}

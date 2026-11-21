package mod.azure.atomicdawn.client.weapon;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.item.WeaponItem;
import mod.azure.atomicdawn.weapon.action.WeaponActions;
import mod.azure.atomicdawn.weapon.instance.Weapons;
import mod.azure.atomicdawn.weapon.modification.ModificationResolver;
import mod.azure.atomicdawn.weapon.modification.WeaponScope;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class ScopeClient {

    private static final int BLACK = 0xFF000000;

    private static final int RETICLE = 0xE0000000;

    @Nullable
    private static WeaponScope active;

    private static float scale = 0.5F;

    private ScopeClient() {}

    public static boolean scoped() {
        return active != null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var next = resolve().orElse(null);
        var player = Minecraft.getInstance().player;
        if (player != null && (next == null) != (active == null)) {
            player.playSound(next != null ? SoundEvents.SPYGLASS_USE : SoundEvents.SPYGLASS_STOP_USING, 1F, 1F);
        }
        if (next == null) {
            scale = 0.5F;
        }
        active = next;
    }

    private static Optional<WeaponScope> resolve() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return Optional.empty();
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponItem weapon) || !WeaponActions.isAiming(player, stack)) {
            return Optional.empty();
        }
        var base = Weapons.baseDefinition(level, stack);
        if (base == null) {
            return Optional.empty();
        }
        var installed = ModificationResolver.resolve(
            level.registryAccess(),
            weapon.definitionKey(),
            base,
            ModificationResolver.installed(stack, base)
        );
        return ModificationResolver.scope(installed);
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (active != null) {
            event.setNewFovModifier(event.getNewFovModifier() * active.zoom());
        }
    }

    @SubscribeEvent
    public static void onPlayerTurn(CalculatePlayerTurnEvent event) {
        if (active != null) {
            event.setMouseSensitivity(event.getMouseSensitivity() * active.sensitivity());
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (active != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (active != null && event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        }
    }

    public static final class Overlay implements GuiLayer {

        @Override
        public void render(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
            var scope = active;
            if (scope == null) {
                return;
            }
            var minecraft = Minecraft.getInstance();
            if (minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden) {
                return;
            }
            scale = Mth.lerp(0.5F * deltaTracker.getRealtimeDeltaTicks(), scale, 1.125F);
            var window = minecraft.getWindow();
            var width = window.getGuiScaledWidth();
            var height = window.getGuiScaledHeight();
            var shortest = (float) Math.min(width, height);
            var factor = Math.min(width / shortest, height / shortest) * scale;
            var size = Mth.floor(shortest * factor);
            var left = (width - size) / 2;
            var top = (height - size) / 2;
            var right = left + size;
            var bottom = top + size;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                scope.overlayTexture(),
                left,
                top,
                0F,
                0F,
                size,
                size,
                size,
                size
            );
            graphics.fill(0, bottom, width, height, BLACK);
            graphics.fill(0, 0, width, top, BLACK);
            graphics.fill(0, top, left, bottom, BLACK);
            graphics.fill(right, top, width, bottom, BLACK);
            if (scope.reticle()) {
                var cx = width / 2;
                var cy = height / 2;
                var reach = size / 2 - 4;
                var gap = 3;
                graphics.fill(cx - reach, cy, cx - gap, cy + 1, RETICLE);
                graphics.fill(cx + gap + 1, cy, cx + reach, cy + 1, RETICLE);
                graphics.fill(cx, cy - reach, cx + 1, cy - gap, RETICLE);
                graphics.fill(cx, cy + gap + 1, cx + 1, cy + reach, RETICLE);
                graphics.fill(cx, cy, cx + 1, cy + 1, RETICLE);
            }
        }
    }
}

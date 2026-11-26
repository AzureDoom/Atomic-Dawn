package mod.azure.atomicdawn.client.vats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.network.VatsShotPayload;
import mod.azure.atomicdawn.network.VatsStatePayload;
import mod.azure.atomicdawn.network.WeaponActionPayload;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.vats.ActionPoints;
import mod.azure.atomicdawn.vats.BodyPart;
import mod.azure.atomicdawn.vats.PartPoint;
import mod.azure.atomicdawn.vats.VatsTargeting;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class VatsClient {

    private static boolean active;

    private static int targetId = -1;

    private static BodyPart part = BodyPart.TORSO;

    private static boolean criticalArmed;

    private static boolean dirty;

    @Nullable
    private static VatsShotPayload lastShot;

    private static long lastShotTime;

    private static float lastFov = 70F;

    private static float lastPartialTick;

    private VatsClient() {}

    public static boolean isActive() {
        return active;
    }

    public static boolean isCriticalArmed() {
        return criticalArmed;
    }

    public static BodyPart part() {
        return part;
    }

    public static @Nullable LivingEntity target() {
        var level = Minecraft.getInstance().level;
        return level != null && targetId >= 0 && level.getEntity(targetId) instanceof LivingEntity living
            ? living
            : null;
    }

    public static ActionPoints actionPoints(LocalPlayer player) {
        return player.getData(ADAttachments.ACTION_POINTS.get());
    }

    public static @Nullable VatsShotPayload lastShot(long now) {
        return lastShot != null && now - lastShotTime < 30 ? lastShot : null;
    }

    public static float fov() {
        return lastFov;
    }

    public static float partialTick() {
        return lastPartialTick;
    }

    public static void onShot(VatsShotPayload payload) {
        lastShot = payload;
        var level = Minecraft.getInstance().level;
        lastShotTime = level != null ? level.getGameTime() : 0L;
        if (payload.critical()) {
            criticalArmed = false;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            active = false;
            targetId = -1;
            criticalArmed = false;
            return;
        }
        var definition = Weapons.definition(minecraft.level, player.getMainHandItem());
        var inGame = minecraft.gui.screen() == null && !player.isSpectator();

        while (ADKeyMappings.VATS.consumeClick()) {
            if (!inGame) {
                continue;
            }
            if (active) {
                deactivate();
            } else if (definition != null) {
                active = true;
                criticalArmed = false;
                dirty = true;
                player.playSound(ADSounds.ENTER_VATS.get(), 1.0F, 1.0F);

                retarget(player, definition, true);
                if (targetId < 0) {
                    player.sendOverlayMessage(Component.translatable("message.atomicdawn.vats_no_target"));
                }
            }
        }
        while (ADKeyMappings.CRITICAL.consumeClick()) {
            if (
                inGame && active && definition != null
                    && (actionPoints(player).criticalReady() || player.hasInfiniteMaterials())
            ) {
                criticalArmed = !criticalArmed;
                player.playSound(ADSounds.VATS_SELECT_PART.get(), 1.0F, 1.0F);
                ClientPacketDistributor.sendToServer(new WeaponActionPayload(WeaponActionPayload.Action.ARM_CRITICAL));
            }
        }

        if (active && (definition == null || !player.isAlive())) {
            deactivate();
        }
        if (active && definition != null) {
            retarget(player, definition, false);
        }
        if (dirty) {
            dirty = false;
            ClientPacketDistributor.sendToServer(new VatsStatePayload(active, targetId, part));
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        var minecraft = Minecraft.getInstance();
        if (!active || minecraft.gui.screen() != null) {
            return;
        }
        var target = target();
        if (target == null || event.getScrollDeltaY() == 0) {
            return;
        }
        event.setCanceled(true);
        var available = availableParts(target);
        if (available.isEmpty()) {
            return;
        }
        var index = available.indexOf(part);
        var step = event.getScrollDeltaY() > 0 ? -1 : 1;
        var next = available.get(Math.floorMod((Math.max(index, 0)) + step, available.size()));
        if (next != part) {
            var player = minecraft.player;
            if (player != null) {
                player.playSound(ADSounds.VATS_DESELECT.get(), 1.0F, 1.0F);
            }
            part = next;
            if (player != null) {
                player.playSound(ADSounds.VATS_SELECT.get(), 1.0F, 1.0F);
            }
            dirty = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        lastFov = event.getFOV();
        lastPartialTick = event.getPartialTick();
    }

    public static List<BodyPart> availableParts(LivingEntity target) {
        return new ArrayList<>(VatsTargeting.parts(target).keySet());
    }

    public static Map<BodyPart, PartPoint> parts(LivingEntity target) {
        return VatsTargeting.parts(target);
    }

    private static void retarget(LocalPlayer player, WeaponDefinition definition, boolean force) {
        double cone = AtomicDawn.CONFIG.common.combat.vats.VATS_TARGET_CONE;
        var current = target();
        var currentValid = current != null && current.isAlive()
            && VatsTargeting.isValidTarget(player, current, definition, cone);
        var best = VatsTargeting.acquire(player, definition, cone);

        LivingEntity chosen;
        if (!currentValid) {
            chosen = best;
        } else if (best != null && best != current && !force) {
            var currentAngle = VatsTargeting.angleTo(player, current.getBoundingBox().getCenter());
            var bestAngle = VatsTargeting.angleTo(player, best.getBoundingBox().getCenter());
            chosen = bestAngle + 4.0D < currentAngle ? best : current;
        } else {
            chosen = force && best != null ? best : current;
        }

        var id = chosen == null ? -1 : chosen.getId();
        if (id != targetId) {
            targetId = id;
            if (chosen != null && !VatsTargeting.parts(chosen).containsKey(part)) {
                part = BodyPart.TORSO;
            }
            dirty = true;
        }
    }

    private static void deactivate() {
        if (!active) {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(ADSounds.ENTER_VATS.get(), 1.0F, 0.85F);
        }

        active = false;
        targetId = -1;
        criticalArmed = false;
        dirty = true;
    }
}

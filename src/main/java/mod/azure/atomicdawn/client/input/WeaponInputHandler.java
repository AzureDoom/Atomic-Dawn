package mod.azure.atomicdawn.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.ClientWeaponState;
import mod.azure.atomicdawn.client.camera.RecoilHandler;
import mod.azure.atomicdawn.client.vats.VatsClient;
import mod.azure.atomicdawn.network.WeaponActionPayload;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.action.WeaponActions;
import mod.azure.atomicdawn.weapon.definition.FireMode;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;
import mod.azure.atomicdawn.weapon.instance.Weapons;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class WeaponInputHandler {

    private static int lastSelectedSlot = -1;

    @Nullable
    private static Identifier lastDrawSound;

    @Nullable
    private static Item lastHeldItem;

    private WeaponInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            ClientWeaponState.reset();
            lastSelectedSlot = -1;
            lastHeldItem = null;
            lastDrawSound = null;
            return;
        }

        var stack = player.getMainHandItem();
        var definition = Weapons.definition(minecraft.level, stack);
        var now = ClientWeaponState.now();
        trackDraw(player, stack, definition, now);

        var inGame = minecraft.gui.screen() == null && minecraft.mouseHandler.isMouseGrabbed() && !player.isSpectator();
        handleActionKeys(definition != null && inGame);

        if (definition == null) {
            return;
        }
        ClientWeaponState.tickBloom(now, definition.accuracy());

        var attackKey = minecraft.options.keyAttack;
        var clicks = 0;
        if (inGame) {
            while (attackKey.consumeClick()) {
                clicks++;
            }
        }
        var down = inGame && attackKey.isDown();
        var state = Weapons.state(stack, definition);
        var mode = state.fireMode();
        var hasRounds = !definition.usesAmmo() || state.loadedAmmo() >= definition.ammo().perShot();
        var interruptReload = definition.reload().type().perRound() && hasRounds;

        if (mode.tracksTrigger()) {
            var pressed = down || clicks > 0;
            if (pressed && !ClientWeaponState.isTriggerDown() && ClientWeaponState.canShootAt(now, interruptReload)) {
                ClientWeaponState.pressTrigger(now);
                send(WeaponActionPayload.Action.TRIGGER_PRESS);
            } else if (!down && ClientWeaponState.isTriggerDown()) {
                var held = ClientWeaponState.releaseTrigger(now);
                send(WeaponActionPayload.Action.TRIGGER_RELEASE);
                if (mode == FireMode.CHARGED) {
                    var fraction = (float) Math.min(1.0, held / definition.firing().charge().ticks());
                    if (definition.firing().charge().damageFor(fraction) > 0F) {
                        predictShot(player, stack, definition, hasRounds, now);
                    }
                }
            }
        }

        if (!inGame) {
            return;
        }

        switch (mode) {
            case CHARGED -> {

            }
            case SPIN_UP -> {
                var spunUp = ClientWeaponState.triggerHeldTicks(now) >= definition.firing().spinUpTicks();
                if (ClientWeaponState.isTriggerDown() && spunUp && ClientWeaponState.canShootAt(now, interruptReload)) {
                    requestShot(player, stack, definition, hasRounds, now);
                }
            }
            case AUTOMATIC -> {
                if ((clicks > 0 || down) && ClientWeaponState.canShootAt(now, interruptReload)) {
                    requestShot(player, stack, definition, hasRounds, now);
                }
            }
            default -> {
                if (clicks > 0) {
                    ClientWeaponState.bufferShot(now);
                }
                if (ClientWeaponState.canShootAt(now, interruptReload) && ClientWeaponState.consumeBufferedShot(now)) {
                    requestShot(player, stack, definition, hasRounds, now);
                    if (mode == FireMode.BURST && hasRounds) {
                        ClientWeaponState.startBurst(definition);
                    }
                }
            }
        }

        if (ClientWeaponState.pollBurstKick(now, definition)) {
            RecoilHandler.kick(definition.recoil(), WeaponActions.isAiming(player, stack), player.getRandom());
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (event.isAttack() && player != null && Weapons.isWeapon(player.getMainHandItem())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static void requestShot(
        LocalPlayer player,
        ItemStack stack,
        WeaponDefinition definition,
        boolean hasRounds,
        double now
    ) {
        predictShot(player, stack, definition, hasRounds, now);
        send(WeaponActionPayload.Action.FIRE);
    }

    private static void predictShot(
        LocalPlayer player,
        ItemStack stack,
        WeaponDefinition definition,
        boolean hasRounds,
        double now
    ) {
        if (
            VatsClient.isActive() && VatsClient.target() != null && !player.hasInfiniteMaterials()
                && VatsClient.actionPoints(player).points() < VatsActions.apCost(player, definition)
        ) {
            return;
        }
        ClientWeaponState.onShotRequested(now, definition, hasRounds);
        if (hasRounds && !definition.isMelee()) {
            RecoilHandler.kick(definition.recoil(), WeaponActions.isAiming(player, stack), player.getRandom());
        }
    }

    private static void handleActionKeys(boolean active) {
        while (ADKeyMappings.RELOAD.consumeClick()) {
            if (active && !ClientWeaponState.isReloading()) {
                send(WeaponActionPayload.Action.RELOAD);
            }
        }
        while (ADKeyMappings.FIRE_MODE.consumeClick()) {
            if (active) {
                send(WeaponActionPayload.Action.CYCLE_FIRE_MODE);
            }
        }
        while (ADKeyMappings.INSPECT.consumeClick()) {
            if (active) {
                send(WeaponActionPayload.Action.INSPECT);
            }
        }
    }

    private static void trackDraw(
        LocalPlayer player,
        ItemStack stack,
        @Nullable WeaponDefinition definition,
        double now
    ) {
        var slot = player.getInventory().getSelectedSlot();
        var item = stack.getItem();
        if (slot == lastSelectedSlot && item == lastHeldItem) {
            return;
        }
        if (lastDrawSound != null) {
            Minecraft.getInstance().getSoundManager().stop(lastDrawSound, SoundSource.PLAYERS);
        }
        lastDrawSound = definition == null
            ? null
            : definition.sounds().draw().map(holder -> holder.value().location()).orElse(null);
        lastSelectedSlot = slot;
        lastHeldItem = item;
        RecoilHandler.clear();
        if (ClientWeaponState.isTriggerDown()) {
            ClientWeaponState.releaseTrigger(now);
            send(WeaponActionPayload.Action.TRIGGER_RELEASE);
        }
        if (definition != null) {
            ClientWeaponState.onDraw(now, definition);
        } else {
            ClientWeaponState.onReloadStatus(0);
        }
    }

    private static void send(WeaponActionPayload.Action action) {
        ClientPacketDistributor.sendToServer(new WeaponActionPayload(action));
    }
}

package mod.azure.atomicdawn.client.powerarmor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.client.render.powerarmor.PowerArmorTextures;
import mod.azure.atomicdawn.network.PowerArmorActionPayload;
import mod.azure.atomicdawn.network.PowerArmorLandingPayload;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorPieceItem;
import mod.azure.atomicdawn.powerarmor.PowerCoreItem;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorBonuses;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorEffectType;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorLegendary;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorMod;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgrades;
import mod.azure.atomicdawn.registry.ADItems;
import mod.azure.atomicdawn.weapon.action.VatsActions;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class PowerArmorClient {

    private static final int JUMP_COOLDOWN = 12;

    private static final double JET_PACK_MAX_RISE = 0.35;

    private static int shakeTicks;

    private static int jumpCooldown;

    private static int shakeTotal;

    private static float shakeIntensity;

    private static int flightToggleWindow;

    private static boolean jumpWasDown;

    private static long lastJetPackTime = Long.MIN_VALUE / 2;

    @Nullable
    private static ItemStack fist;

    private static final ItemStackRenderState FIST_RENDER_STATE =
        new ItemStackRenderState();

    private PowerArmorClient() {}

    @Nullable
    public static PowerArmorEntity current() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getVehicle() instanceof PowerArmorEntity armor ? armor : null;
    }

    public static boolean inPowerArmor() {
        return current() != null;
    }

    public static void onLanding(PowerArmorLandingPayload payload) {
        shakeIntensity = payload.intensity();
        shakeTotal = Math.max(8, payload.ticks());
        shakeTicks = shakeTotal;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (shakeTicks > 0) {
            shakeTicks--;
        }
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
        if (flightToggleWindow > 0) {
            flightToggleWindow--;
        }
        var minecraft = Minecraft.getInstance();
        var armor = current();
        while (ADKeyMappings.EXIT_POWER_ARMOR.consumeClick()) {
            if (armor != null) {
                ClientPacketDistributor.sendToServer(
                    PowerArmorActionPayload.slot(PowerArmorActionPayload.Action.EXIT, armor.getId(), -1)
                );
            }
        }
        if (armor == null || minecraft.gui.screen() != null) {
            jumpWasDown = false;
            return;
        }
        var jumpDown = minecraft.options.keyJump.isDown();
        var jumpPressed = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;
        if (tickFlight(minecraft, armor, jumpPressed)) {
            return;
        }

        if (
            jumpDown && !armor.onGround() && jumpCooldown <= JUMP_COOLDOWN - PowerArmorEntity.JET_PACK_AP_DELAY
                && armor.canJetPack()
        ) {
            lastJetPackTime = armor.level().getGameTime();
            if (hasJetPackAp(minecraft)) {
                var motion = armor.getDeltaMovement();
                var thrust = PowerArmorBonuses.of(armor).get(PowerArmorEffectType.JET_PACK);
                armor.setDeltaMovement(motion.x, Math.min(JET_PACK_MAX_RISE, motion.y + thrust), motion.z);
                armor.resetFallDistance();
                return;
            }
        }
        if (jumpCooldown > 0) {
            return;
        }
        if (jumpDown && armor.onGround() && armor.canMove()) {
            var motion = armor.getDeltaMovement();
            var boost = (float) Math.sqrt(1F + PowerArmorBonuses.of(armor).get(PowerArmorEffectType.JUMP));
            armor.setDeltaMovement(motion.x, PowerArmorEntity.JUMP_VELOCITY * boost, motion.z);
            armor.setOnGround(false);
            jumpCooldown = JUMP_COOLDOWN;
        }
    }

    public static long lastJetPackTime() {
        return lastJetPackTime;
    }

    private static boolean hasJetPackAp(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null) {
            return false;
        }
        var cost = (float) AtomicDawn.CONFIG.common.equipment.powerArmor.movement.POWER_ARMOR_JETPACK_AP_COST;
        return player.hasInfiniteMaterials() || cost <= 0F || VatsActions.actionPoints(player).points() >= cost;
    }

    private static boolean tickFlight(Minecraft minecraft, PowerArmorEntity armor, boolean jumpPressed) {
        if (jumpPressed && armor.canFly()) {
            if (flightToggleWindow > 0) {
                flightToggleWindow = 0;
                var start = !armor.isFlying();
                setFlying(armor, start);
                if (!start) {
                    return false;
                }
                var motion = armor.getDeltaMovement();
                armor.setDeltaMovement(motion.x, PowerArmorEntity.FLY_VERTICAL_SPEED, motion.z);
                return true;
            }
            flightToggleWindow = 7;
        }
        if (!armor.isFlying()) {
            return false;
        }
        var up = minecraft.options.keyJump.isDown();
        var down = minecraft.options.keyShift.isDown();
        if (down && !up && armor.onGround()) {
            setFlying(armor, false);
            return false;
        }
        var motion = armor.getDeltaMovement();
        var speed = PowerArmorEntity.FLY_VERTICAL_SPEED;
        var vertical = up == down ? motion.y * 0.6 : up ? speed : -speed;
        armor.setDeltaMovement(motion.x, vertical, motion.z);
        return true;
    }

    private static void setFlying(PowerArmorEntity armor, boolean flying) {
        armor.setFlying(flying);
        ClientPacketDistributor.sendToServer(PowerArmorActionPayload.flying(armor.getId(), flying));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (!player.level().isClientSide() || !(player.getVehicle() instanceof PowerArmorEntity armor)) {
            return;
        }
        var from = armor.riderWalk();
        var to = player.walkAnimation;
        to.speedOld = from.speedOld;
        to.speed = from.speed;
        to.position = from.position;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0) {
            return;
        }
        var progress = (shakeTicks - event.getPartialTick()) / shakeTotal;
        var amplitude = shakeIntensity * Math.max(0F, progress) * progress;
        var time = (shakeTotal - shakeTicks + event.getPartialTick()) * 2.7F;
        event.setPitch(event.getPitch() + Mth.sin(time * 3.1F) * amplitude * 6F + amplitude * 4F);
        event.setYaw(event.getYaw() + Mth.sin(time * 2.3F) * amplitude * 2F);
        event.setRoll(event.getRoll() + Mth.cos(time * 2.9F) * amplitude * 3F);
    }

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!inPowerArmor()) {
            return;
        }
        var name = event.getName();
        if (
            name.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || name.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || name.equals(VanillaGuiLayers.VEHICLE_HEALTH)
                || name.equals(VanillaGuiLayers.FOOD_LEVEL)
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().isEmpty() || !inPowerArmor()) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }
        event.setCanceled(true);
        if (fist == null) {
            fist = new ItemStack(ADItems.POWER_ARMOR_FIST.get());
        }
        var pose = event.getPoseStack();
        pose.pushPose();
        var right = player.getMainArm() == HumanoidArm.RIGHT;
        var side = right ? 1F : -1F;
        var swing = event.getSwingProgress();
        var root = Mth.sqrt(swing);
        var dx = -0.3F * Mth.sin(root * Mth.PI);
        var dy = 0.4F * Mth.sin(root * Mth.PI * 2F);
        var dz = -0.4F * Mth.sin(swing * Mth.PI);
        pose.translate(side * (dx + 0.56F), dy - 0.52F + event.getEquipProgress() * -0.6F, dz - 0.72F);
        minecraft.getItemModelResolver()
            .updateForLiving(
                FIST_RENDER_STATE,
                fist,
                right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                player
            );

        FIST_RENDER_STATE.submit(
            pose,
            event.getSubmitNodeCollector(),
            event.getPackedLight(),
            OverlayTexture.NO_OVERLAY,
            0
        );
        pose.popPose();
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.getItem() instanceof PowerCoreItem) {
            event.getToolTip()
                .add(
                    Component.translatable(
                        "power_armor.atomicdawn.tooltip.charge",
                        Math.round(PowerCoreItem.fraction(stack) * 100F)
                    )
                        .withStyle(ChatFormatting.GREEN)
                );
        } else if (stack.getItem() instanceof PowerArmorPieceItem piece) {
            event.getToolTip()
                .add(
                    Component.translatable(
                        "power_armor.atomicdawn.tooltip.slot",
                        Component.translatable(piece.slot().translationKey())
                    )
                        .withStyle(ChatFormatting.GRAY)
                );
            event.getToolTip()
                .add(
                    Component.translatable(
                        "power_armor.atomicdawn.tooltip.condition",
                        Math.round(PowerArmorPieceItem.condition(stack) * 100F)
                    ).withStyle(PowerArmorPieceItem.isBroken(stack) ? ChatFormatting.RED : ChatFormatting.GRAY)
                );
            addUpgradeLines(event, stack);
            var paint = PowerArmorPieceItem.paint(stack);
            if (paint != PowerArmorPieceItem.NO_PAINT) {
                event.getToolTip()
                    .add(
                        Component.translatable("power_armor.atomicdawn.tooltip.painted")
                            .withStyle(style -> style.withColor(paint))
                    );
            }
        }
    }

    private static void addUpgradeLines(ItemTooltipEvent event, ItemStack stack) {
        var upgrades = PowerArmorUpgrades.of(stack);
        var level = Minecraft.getInstance().level;
        if (upgrades.isEmpty() || level == null) {
            return;
        }
        var access = level.registryAccess();
        var tooltip = event.getToolTip();
        for (var star = 1; star <= 3; star++) {
            var id = upgrades.legendary(access, star);
            if (id != null) {
                tooltip.add(
                    Component.literal("★".repeat(star) + " ")
                        .append(Component.translatable(PowerArmorLegendary.translationKey(id)))
                        .withStyle(ChatFormatting.GOLD)
                );
            }
        }
        for (var id : upgrades.mods().values()) {
            tooltip.add(
                Component.literal("+ ")
                    .append(Component.translatable(PowerArmorMod.translationKey(id)))
                    .withStyle(ChatFormatting.AQUA)
            );
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        shakeTicks = 0;
        fist = null;
        PowerArmorTextures.clear();
    }
}

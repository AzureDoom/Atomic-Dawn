package mod.azure.atomicdawn.client.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.ArmorPiece;
import mod.azure.atomicdawn.armor.ArmorPieceItem;
import mod.azure.atomicdawn.armor.upgrade.ArmorBonuses;
import mod.azure.atomicdawn.armor.upgrade.ArmorEffectType;
import mod.azure.atomicdawn.armor.upgrade.ArmorLegendary;
import mod.azure.atomicdawn.armor.upgrade.ArmorMod;
import mod.azure.atomicdawn.armor.upgrade.ArmorUpgrades;
import mod.azure.atomicdawn.network.ArmorBenchActionPayload;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.weapon.action.VatsActions;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class ArmorClient {

    private static final double JET_PACK_MAX_RISE = 0.3;

    private static final int JET_PACK_DELAY = 6;

    private static int airTicks;

    private ArmorClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (
            player == null || player.onGround() || player.isInWater() || player.getAbilities().flying
                || player.getVehicle() instanceof PowerArmorEntity
        ) {
            airTicks = 0;
            return;
        }
        airTicks++;
        if (minecraft.gui.screen() != null || !minecraft.options.keyJump.isDown() || airTicks < JET_PACK_DELAY) {
            return;
        }
        var thrust = ArmorBonuses.of(player).get(ArmorEffectType.JET_PACK);
        if (thrust <= 0F) {
            return;
        }
        var cost = (float) AtomicDawn.CONFIG.armor.ARMOR_JETPACK_AP_COST;
        if (!player.hasInfiniteMaterials() && VatsActions.actionPoints(player).points() < cost) {
            return;
        }
        var motion = player.getDeltaMovement();
        var rise = thrust * AtomicDawn.CONFIG.armor.ARMOR_JETPACK_THRUST;
        player.setDeltaMovement(motion.x, Math.min(JET_PACK_MAX_RISE, motion.y + rise), motion.z);
        player.resetFallDistance();
        ClientPacketDistributor.sendToServer(ArmorBenchActionPayload.of(ArmorBenchActionPayload.Action.JET_PACK));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var piece = ArmorPiece.of(stack);
        if (piece == null) {
            return;
        }
        var tooltip = event.getToolTip();
        if (stack.getItem() instanceof ArmorPieceItem item) {
            tooltip.add(
                Component.translatable(
                    "armor.atomicdawn.tooltip.weight",
                    Component.translatable(item.set().weight().translationKey())
                ).withStyle(ChatFormatting.GRAY)
            );
        }
        var upgrades = ArmorUpgrades.of(stack);
        var level = Minecraft.getInstance().level;
        if (upgrades.isEmpty() || level == null) {
            return;
        }
        var access = level.registryAccess();
        for (var star = 1; star <= 3; star++) {
            var id = upgrades.legendary(access, star);
            if (id != null) {
                tooltip.add(
                    Component.literal("★".repeat(star) + " ")
                        .append(Component.translatable(ArmorLegendary.translationKey(id)))
                        .withStyle(ChatFormatting.GOLD)
                );
            }
        }
        for (var id : upgrades.mods()) {
            tooltip.add(
                Component.literal("+ ")
                    .append(Component.translatable(ArmorMod.translationKey(id)))
                    .withStyle(ChatFormatting.AQUA)
            );
        }
    }
}

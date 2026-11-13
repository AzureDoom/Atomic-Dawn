package mod.azure.atomicdawn.ghoul;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.GhoulPromptPayload;

public final class GhoulTreatmentService {

    public enum Kind {

        GHOULIFY,
        RESTORE;

        private static final Kind[] VALUES = values();

        public static Kind byIndex(int index) {
            return VALUES[Math.floorMod(index, VALUES.length)];
        }
    }

    private static final long PROMPT_TICKS = 1200L;

    private record Pending(
        Kind kind,
        InteractionHand hand,
        Item item,
        long expiresAt
    ) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private GhoulTreatmentService() {}

    public static void prompt(ServerPlayer player, InteractionHand hand, Kind kind) {
        var check = check(player, kind, TransformationContext.quest());
        if (!check.success()) {
            report(player, check);
            return;
        }
        var stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }
        PENDING.put(
            player.getUUID(),
            new Pending(kind, hand, stack.getItem(), player.level().getGameTime() + PROMPT_TICKS)
        );
        ADNetwork.sendTo(player, new GhoulPromptPayload(kind));
    }

    public static void confirm(ServerPlayer player, Kind kind) {
        var pending = PENDING.remove(player.getUUID());
        if (pending == null || pending.kind() != kind || player.level().getGameTime() > pending.expiresAt()) {
            report(player, TransformationResult.EXPIRED);
            return;
        }
        var hand = pending.hand();
        var item = pending.item();
        var context = TransformationContext.treatment(() -> {
            var stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !stack.is(item)) {
                return false;
            }
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
            return true;
        });
        var result = kind == Kind.GHOULIFY
            ? GhoulService.transform(player, context)
            : GhoulService.revert(player, context);
        if (!result.success()) {
            report(player, result);
        }
    }

    public static void cancel(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void forget(UUID id) {
        PENDING.remove(id);
    }

    private static TransformationResult check(ServerPlayer player, Kind kind, TransformationContext context) {
        return kind == Kind.GHOULIFY
            ? GhoulService.canTransform(player, context)
            : GhoulService.canRevert(player, context);
    }

    public static void report(ServerPlayer player, TransformationResult result) {
        if (result == TransformationResult.ON_COOLDOWN) {
            var remaining = GhoulService.cooldownRemainingMillis(player, System.currentTimeMillis());
            player.sendSystemMessage(
                Component.translatable(result.translationKey(), formatDuration(remaining))
                    .withStyle(ChatFormatting.RED),
                true
            );
            return;
        }
        player.sendSystemMessage(Component.translatable(result.translationKey()).withStyle(ChatFormatting.RED), true);
    }

    public static String formatDuration(long millis) {
        var minutes = Math.max(1L, (millis + 59_999L) / 60_000L);
        var hours = minutes / 60L;
        return hours > 0L ? hours + "h " + (minutes % 60L) + "m" : minutes + "m";
    }
}

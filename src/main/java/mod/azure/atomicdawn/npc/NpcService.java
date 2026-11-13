package mod.azure.atomicdawn.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.Comparator;

import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.NpcEditPayload;
import mod.azure.atomicdawn.network.NpcEditorPayload;
import mod.azure.atomicdawn.network.NpcPickerPayload;
import mod.azure.atomicdawn.registry.ADEntities;

public final class NpcService {

    public static final double EDIT_RANGE = 32.0;

    private static final double EDIT_RANGE_SQR = EDIT_RANGE * EDIT_RANGE;

    private NpcService() {}

    public static boolean canEdit(ServerPlayer player) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }

    public static boolean requireEditor(ServerPlayer player) {
        if (canEdit(player)) {
            return true;
        }
        player.sendSystemMessage(
            Component.translatable("npc.atomicdawn.no_permission").withStyle(ChatFormatting.RED),
            true
        );
        return false;
    }

    public static NpcEntity spawn(ServerLevel level, double x, double y, double z, float yaw, NpcConfig config) {
        var npc = ADEntities.NPC.get().create(level, EntitySpawnReason.COMMAND);
        if (npc == null) {
            return null;
        }
        npc.setPos(x, y, z);
        npc.setYRot(yaw);
        npc.setYHeadRot(yaw);
        npc.yBodyRot = yaw;
        npc.applyConfig(config);
        npc.setHome(BlockPos.containing(x, y, z), yaw);
        level.addFreshEntity(npc);
        return npc;
    }

    public static void openEditor(ServerPlayer player, NpcEntity npc) {
        if (!requireEditor(player)) {
            return;
        }
        NpcDialogueService.close(player);
        ADNetwork.sendTo(player, new NpcEditorPayload(npc.getId(), npc.config()));
    }

    /** Lists every NPC in edit range, including downed ones waiting to respawn (invisible and not pickable). */
    public static void openPicker(ServerPlayer player) {
        if (!requireEditor(player)) {
            return;
        }
        var entries = player.level()
            .getEntitiesOfClass(
                NpcEntity.class,
                player.getBoundingBox().inflate(EDIT_RANGE),
                npc -> npc.isAlive() && npc.distanceToSqr(player) <= EDIT_RANGE_SQR
            )
            .stream()
            .sorted(Comparator.comparingDouble(npc -> npc.distanceToSqr(player)))
            .limit(NpcPickerPayload.MAX_ENTRIES)
            .map(
                npc -> new NpcPickerPayload.Entry(
                    npc.getId(),
                    truncate(npc.getDisplayName().getString()),
                    Math.round(npc.distanceTo(player)),
                    npc.respawnTicks() > 0 ? Math.max(1, (npc.respawnTicks() + 19) / 20) : 0
                )
            )
            .toList();
        if (entries.isEmpty()) {
            player.sendSystemMessage(Component.translatable("npc.atomicdawn.picker.none"), true);
            return;
        }
        NpcDialogueService.close(player);
        ADNetwork.sendTo(player, new NpcPickerPayload(entries));
    }

    public static void handlePick(ServerPlayer player, int entityId) {
        if (
            canEdit(player) && player.level().getEntity(entityId) instanceof NpcEntity npc
                && npc.distanceToSqr(player) <= EDIT_RANGE_SQR
        ) {
            openEditor(player, npc);
        }
    }

    private static String truncate(String name) {
        return name.length() > 256 ? name.substring(0, 256) : name;
    }

    public static void handleEdit(ServerPlayer player, NpcEditPayload payload) {
        if (!canEdit(player)) {
            return;
        }
        if (
            !(player.level().getEntity(payload.entityId()) instanceof NpcEntity npc)
                || npc.distanceToSqr(player) > EDIT_RANGE_SQR
        ) {
            return;
        }
        switch (payload.action()) {
            case SAVE -> npc.applyConfig(payload.config());
            case DELETE -> {
                NpcDialogueService.close(player);
                npc.remove(Entity.RemovalReason.DISCARDED);
            }
            case MOVE_HERE -> {
                npc.getNavigation().stop();
                npc.teleportTo(player.getX(), player.getY(), player.getZ());
                npc.setYRot(player.getYRot());
                npc.setYHeadRot(player.getYRot());
                npc.yBodyRot = player.getYRot();
                npc.setHome(player.blockPosition(), player.getYRot());
            }
            case SET_HOME -> npc.setHome(npc.blockPosition(), npc.getYRot());
            case CLEAR_EQUIPMENT -> npc.clearEquipment();
            case EDIT_SHOP -> {
                npc.applyConfig(payload.config());
                NpcShops.openEditor(player, npc);
            }
            case RELEASE_COMPANION -> npc.setOwner(null, -1L);
            case EDIT_GEAR -> {
                npc.applyConfig(payload.config());
                NpcGear.openEditor(player, npc);
            }
            case RESPAWN_NOW -> {
                npc.applyConfig(payload.config());
                npc.respawnNow();
            }
        }
    }
}

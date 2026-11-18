package mod.azure.atomicdawn.client.camp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mod.azure.atomicdawn.camp.CampModuleBlock;
import mod.azure.atomicdawn.camp.CampModuleFacing;
import mod.azure.atomicdawn.camp.CampSlotService;
import mod.azure.atomicdawn.client.input.ADKeyMappings;
import mod.azure.atomicdawn.network.CampActionPayload;
import mod.azure.atomicdawn.network.CampSyncPayload;

public final class CampModulePlacementClient {

    private static final UUID NONE = new UUID(0L, 0L);

    public record GhostPart(
        BlockPos pos,
        BlockState state,
        boolean valid
    ) {}

    private static boolean holding;

    private static Direction facing = Direction.NORTH;

    private CampModulePlacementClient() {}

    public static Direction facing() {
        return facing;
    }

    public static boolean holdingModule(LocalPlayer player) {
        return isModule(player.getMainHandItem().getItem()) || isModule(player.getOffhandItem().getItem());
    }

    public static boolean isModule(Item item) {
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof CampModuleBlock;
    }

    public static void reset() {
        holding = false;
    }

    public static void tick(LocalPlayer player) {
        if (!holdingModule(player)) {
            holding = false;
            return;
        }
        if (!holding) {
            holding = true;
            apply(player, player.getDirection().getOpposite());
            if (!blueprint().isEmpty()) {
                announce();
            }
        }
        while (ADKeyMappings.WORKSHOP_ROTATE.consumeClick()) {
            apply(player, player.isShiftKeyDown() ? facing.getCounterClockWise() : facing.getClockWise());
            announce();
        }
    }

    private static void apply(LocalPlayer player, Direction value) {
        facing = value;
        CampModuleFacing.set(player.getUUID(), value);
        CampClientState.send(CampActionPayload.Action.MODULE_FACING, NONE, value.get2DDataValue());
    }

    private static void announce() {
        CampWorkshopClient.toast(
            Component.translatable(
                "camp.atomicdawn.module.facing",
                Component.translatable("camp.atomicdawn.facing." + facing.getSerializedName()),
                ADKeyMappings.WORKSHOP_ROTATE.getTranslatedKeyMessage()
            )
        );
    }

    public static List<CampSyncPayload.BlueprintPart> blueprint() {
        var state = CampClientState.get();
        return state == null ? List.of() : state.blueprint();
    }

    public static List<GhostPart> ghosts(BlockPos origin, boolean deployable) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var state = CampClientState.get();
        if (level == null || state == null || state.blueprint().isEmpty()) {
            return List.of();
        }
        var rotation = CampSlotService.rotation(Direction.NORTH, facing);
        var radius = (long) state.radius();
        var result = new ArrayList<GhostPart>(state.blueprint().size());
        for (var part : state.blueprint()) {
            var pos = origin.offset(part.offset().rotate(rotation));
            var dy = pos.getY() - origin.getY();
            long dx = pos.getX() - origin.getX();
            long dz = pos.getZ() - origin.getZ();
            var valid = deployable && !pos.equals(origin) && dy <= state.up() && dy >= -state.down()
                && dx * dx + dz * dz <= radius * radius && !level.isOutsideBuildHeight(pos)
                && level.getBlockState(pos).canBeReplaced();
            result.add(new GhostPart(pos, part.state().rotate(rotation), valid));
        }
        return result;
    }
}

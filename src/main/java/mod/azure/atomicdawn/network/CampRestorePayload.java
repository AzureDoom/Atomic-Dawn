package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.camp.CampSlotService;

public record CampRestorePayload(
    boolean open,
    UUID campId,
    int ready,
    int blocked,
    List<CampSlotService.Blocker> blockers
) implements CustomPacketPayload {

    public static final int MAX_BLOCKERS = 2048;

    private static final UUID NONE = new UUID(0L, 0L);

    public static final Type<CampRestorePayload> TYPE = new Type<>(AtomicDawn.id("camp_restore"));

    public static final StreamCodec<ByteBuf, CampRestorePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ByteBufCodecs.BOOL.encode(buf, payload.open());
            UUIDUtil.STREAM_CODEC.encode(buf, payload.campId());
            ByteBufCodecs.VAR_INT.encode(buf, payload.ready());
            ByteBufCodecs.VAR_INT.encode(buf, payload.blocked());
            var list = payload.blockers().subList(0, Math.min(payload.blockers().size(), MAX_BLOCKERS));
            ByteBufCodecs.VAR_INT.encode(buf, list.size());
            for (var blocker : list) {
                BlockPos.STREAM_CODEC.encode(buf, blocker.pos());
                ByteBufCodecs.VAR_INT.encode(buf, Block.getId(blocker.state()));
                ByteBufCodecs.VAR_INT.encode(buf, blocker.reason().ordinal());
            }
        },
        buf -> {
            var open = ByteBufCodecs.BOOL.decode(buf);
            var campId = UUIDUtil.STREAM_CODEC.decode(buf);
            var ready = ByteBufCodecs.VAR_INT.decode(buf);
            var blocked = ByteBufCodecs.VAR_INT.decode(buf);
            var count = ByteBufCodecs.VAR_INT.decode(buf);
            if (count < 0 || count > MAX_BLOCKERS) {
                throw new IllegalArgumentException("C.A.M.P. restore list too large: " + count);
            }
            var list = new ArrayList<CampSlotService.Blocker>(count);
            for (var i = 0; i < count; i++) {
                var pos = BlockPos.STREAM_CODEC.decode(buf);
                var state = Block.stateById(ByteBufCodecs.VAR_INT.decode(buf));
                var reason = CampSlotService.Reason.byIndex(ByteBufCodecs.VAR_INT.decode(buf));
                list.add(new CampSlotService.Blocker(pos, state, reason));
            }
            return new CampRestorePayload(open, campId, ready, blocked, list);
        }
    );

    public CampRestorePayload {
        blockers = List.copyOf(blockers);
    }

    public static CampRestorePayload open(UUID campId, CampSlotService.RestorePlan plan) {
        return new CampRestorePayload(true, campId, plan.ready(), plan.blocked(), plan.blockers());
    }

    public static CampRestorePayload closed() {
        return new CampRestorePayload(false, NONE, 0, 0, List.of());
    }

    public boolean clearable() {
        return blocked > 0 && !blockers.isEmpty()
            && blockers.stream().allMatch(blocker -> blocker.reason() == CampSlotService.Reason.TERRAIN);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

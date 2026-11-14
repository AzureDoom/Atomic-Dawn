package mod.azure.atomicdawn.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import mod.azure.atomicdawn.AtomicDawn;

public record CampSyncPayload(
    boolean enabled,
    Optional<CampView> camp,
    int slotsUsed,
    int slotsMax,
    int radius,
    int up,
    int down,
    int spacing,
    boolean dimensionAllowed,
    List<CampView> nearby,
    List<SlotView> slots,
    int selectedSlot,
    Optional<AccessView> access
) implements CustomPacketPayload {

    public static final int MAX_NEARBY = 64;

    public static final int MAX_OBJECTS = 2048;

    public static final int MAX_PARTS = 32;

    public static final int MAX_SLOTS = 10;

    public static final int MAX_TRUSTED = 32;

    private static <T> void writeList(ByteBuf buf, List<T> list, int max, StreamCodec<ByteBuf, T> codec) {
        var count = Math.min(list.size(), max);
        ByteBufCodecs.VAR_INT.encode(buf, count);
        for (var i = 0; i < count; i++) {
            codec.encode(buf, list.get(i));
        }
    }

    private static <T> List<T> readList(ByteBuf buf, int max, Function<ByteBuf, T> reader) {
        var count = ByteBufCodecs.VAR_INT.decode(buf);
        if (count < 0 || count > max) {
            throw new IllegalArgumentException("C.A.M.P. sync list too large: " + count);
        }
        var list = new ArrayList<T>(count);
        for (var i = 0; i < count; i++) {
            list.add(reader.apply(buf));
        }
        return List.copyOf(list);
    }

    public record ObjectView(
        Identifier definition,
        BlockPos pos,
        List<BlockPos> parts
    ) {

        static final StreamCodec<ByteBuf, ObjectView> STREAM_CODEC = StreamCodec.of(
            (buf, view) -> {
                Identifier.STREAM_CODEC.encode(buf, view.definition());
                BlockPos.STREAM_CODEC.encode(buf, view.pos());
                writeList(buf, view.parts(), MAX_PARTS, BlockPos.STREAM_CODEC);
            },
            buf -> new ObjectView(
                Identifier.STREAM_CODEC.decode(buf),
                BlockPos.STREAM_CODEC.decode(buf),
                readList(buf, MAX_PARTS, BlockPos.STREAM_CODEC::decode)
            )
        );

        public boolean occupies(BlockPos target) {
            return parts.contains(target);
        }
    }

    public record CampView(
        UUID campId,
        String ownerName,
        boolean own,
        boolean canBuild,
        boolean mapIcon,
        String dimension,
        BlockPos origin,
        int slotIndex,
        int budgetUsed,
        int budgetLimit,
        List<ObjectView> objects
    ) {

        static final StreamCodec<ByteBuf, CampView> STREAM_CODEC = StreamCodec.of(
            (buf, view) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, view.campId());
                ByteBufCodecs.STRING_UTF8.encode(buf, view.ownerName());
                ByteBufCodecs.BOOL.encode(buf, view.own());
                ByteBufCodecs.BOOL.encode(buf, view.canBuild());
                ByteBufCodecs.BOOL.encode(buf, view.mapIcon());
                ByteBufCodecs.STRING_UTF8.encode(buf, view.dimension());
                BlockPos.STREAM_CODEC.encode(buf, view.origin());
                ByteBufCodecs.VAR_INT.encode(buf, view.slotIndex());
                ByteBufCodecs.VAR_INT.encode(buf, view.budgetUsed());
                ByteBufCodecs.VAR_INT.encode(buf, view.budgetLimit());
                writeList(buf, view.objects(), MAX_OBJECTS, ObjectView.STREAM_CODEC);
            },
            buf -> new CampView(
                UUIDUtil.STREAM_CODEC.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                BlockPos.STREAM_CODEC.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                readList(buf, MAX_OBJECTS, ObjectView.STREAM_CODEC::decode)
            )
        );

        public int count(Identifier definition) {
            var total = 0;
            for (var object : objects) {
                if (object.definition().equals(definition)) {
                    total++;
                }
            }
            return total;
        }

        public Optional<ObjectView> objectAt(BlockPos pos) {
            for (var object : objects) {
                if (object.occupies(pos)) {
                    return Optional.of(object);
                }
            }
            return Optional.empty();
        }
    }

    public record SlotView(
        UUID campId,
        int slotIndex,
        boolean deployed,
        String dimension,
        BlockPos origin,
        int budgetUsed,
        int budgetLimit,
        int objects,
        int stored
    ) {

        static final StreamCodec<ByteBuf, SlotView> STREAM_CODEC = StreamCodec.of(
            (buf, view) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, view.campId());
                ByteBufCodecs.VAR_INT.encode(buf, view.slotIndex());
                ByteBufCodecs.BOOL.encode(buf, view.deployed());
                ByteBufCodecs.STRING_UTF8.encode(buf, view.dimension());
                BlockPos.STREAM_CODEC.encode(buf, view.origin());
                ByteBufCodecs.VAR_INT.encode(buf, view.budgetUsed());
                ByteBufCodecs.VAR_INT.encode(buf, view.budgetLimit());
                ByteBufCodecs.VAR_INT.encode(buf, view.objects());
                ByteBufCodecs.VAR_INT.encode(buf, view.stored());
            },
            buf -> new SlotView(
                UUIDUtil.STREAM_CODEC.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                BlockPos.STREAM_CODEC.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf)
            )
        );
    }

    public record TrustedView(
        UUID id,
        String name
    ) {

        static final StreamCodec<ByteBuf, TrustedView> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            TrustedView::id,
            ByteBufCodecs.STRING_UTF8,
            TrustedView::name,
            TrustedView::new
        );
    }

    public record AccessView(
        List<TrustedView> trusted,
        int flags
    ) {

        static final StreamCodec<ByteBuf, AccessView> STREAM_CODEC = StreamCodec.of(
            (buf, view) -> {
                writeList(buf, view.trusted(), MAX_TRUSTED, TrustedView.STREAM_CODEC);
                ByteBufCodecs.VAR_INT.encode(buf, view.flags());
            },
            buf -> new AccessView(
                readList(buf, MAX_TRUSTED, TrustedView.STREAM_CODEC::decode),
                ByteBufCodecs.VAR_INT.decode(buf)
            )
        );

        public boolean flag(int index) {
            return (flags & (1 << index)) != 0;
        }
    }

    public static final Type<CampSyncPayload> TYPE = new Type<>(AtomicDawn.id("camp_sync"));

    public static final StreamCodec<ByteBuf, CampSyncPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ByteBufCodecs.BOOL.encode(buf, payload.enabled());
            ByteBufCodecs.BOOL.encode(buf, payload.camp().isPresent());
            payload.camp().ifPresent(view -> CampView.STREAM_CODEC.encode(buf, view));
            ByteBufCodecs.VAR_INT.encode(buf, payload.slotsUsed());
            ByteBufCodecs.VAR_INT.encode(buf, payload.slotsMax());
            ByteBufCodecs.VAR_INT.encode(buf, payload.radius());
            ByteBufCodecs.VAR_INT.encode(buf, payload.up());
            ByteBufCodecs.VAR_INT.encode(buf, payload.down());
            ByteBufCodecs.VAR_INT.encode(buf, payload.spacing());
            ByteBufCodecs.BOOL.encode(buf, payload.dimensionAllowed());
            writeList(buf, payload.nearby(), MAX_NEARBY, CampView.STREAM_CODEC);
            writeList(buf, payload.slots(), MAX_SLOTS, SlotView.STREAM_CODEC);
            ByteBufCodecs.VAR_INT.encode(buf, payload.selectedSlot());
            ByteBufCodecs.BOOL.encode(buf, payload.access().isPresent());
            payload.access().ifPresent(view -> AccessView.STREAM_CODEC.encode(buf, view));
        },
        buf -> {
            var enabled = ByteBufCodecs.BOOL.decode(buf);
            var camp = ByteBufCodecs.BOOL.decode(buf)
                ? Optional.of(CampView.STREAM_CODEC.decode(buf))
                : Optional.<CampView>empty();
            var slotsUsed = ByteBufCodecs.VAR_INT.decode(buf);
            var slotsMax = ByteBufCodecs.VAR_INT.decode(buf);
            var radius = ByteBufCodecs.VAR_INT.decode(buf);
            var up = ByteBufCodecs.VAR_INT.decode(buf);
            var down = ByteBufCodecs.VAR_INT.decode(buf);
            var spacing = ByteBufCodecs.VAR_INT.decode(buf);
            var allowed = ByteBufCodecs.BOOL.decode(buf);
            var nearby = readList(buf, MAX_NEARBY, CampView.STREAM_CODEC::decode);
            var slots = readList(buf, MAX_SLOTS, SlotView.STREAM_CODEC::decode);
            var selected = ByteBufCodecs.VAR_INT.decode(buf);
            var access = ByteBufCodecs.BOOL.decode(buf)
                ? Optional.of(AccessView.STREAM_CODEC.decode(buf))
                : Optional.<AccessView>empty();
            return new CampSyncPayload(
                enabled,
                camp,
                slotsUsed,
                slotsMax,
                radius,
                up,
                down,
                spacing,
                allowed,
                nearby,
                slots,
                selected,
                access
            );
        }
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

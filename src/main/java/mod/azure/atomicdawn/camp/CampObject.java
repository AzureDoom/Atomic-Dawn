package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

public record CampObject(
    UUID objectId,
    Identifier definition,
    Identifier block,
    BlockPos pos,
    Direction facing,
    PlacementMode mode,
    int budget,
    List<BlockPos> parts,
    boolean stored,
    List<PartSnapshot> snapshot,
    boolean manual
) {

    public static final Codec<CampObject> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(CampObject::objectId),
            Identifier.CODEC.fieldOf("definition").forGetter(CampObject::definition),
            Identifier.CODEC.fieldOf("block").forGetter(CampObject::block),
            BlockPos.CODEC.fieldOf("pos").forGetter(CampObject::pos),
            Direction.CODEC.optionalFieldOf("facing", Direction.NORTH).forGetter(CampObject::facing),
            PlacementMode.CODEC.optionalFieldOf("mode", PlacementMode.COLLISION).forGetter(CampObject::mode),
            Codec.INT.optionalFieldOf("budget", 0).forGetter(CampObject::budget),
            BlockPos.CODEC.listOf().optionalFieldOf("parts", List.of()).forGetter(CampObject::parts),
            Codec.BOOL.optionalFieldOf("stored", false).forGetter(CampObject::stored),
            PartSnapshot.CODEC.listOf().optionalFieldOf("snapshot", List.of()).forGetter(CampObject::snapshot),
            Codec.BOOL.optionalFieldOf("manual", false).forGetter(CampObject::manual)
        ).apply(instance, CampObject::new)
    );

    public CampObject {
        pos = pos.immutable();
        parts = parts.isEmpty() ? List.of(pos) : parts.stream().map(BlockPos::immutable).toList();
        snapshot = List.copyOf(snapshot);
    }

    public static CampObject placed(
        UUID objectId,
        Identifier definition,
        Identifier block,
        BlockPos pos,
        Direction facing,
        PlacementMode mode,
        int budget,
        List<BlockPos> parts
    ) {
        return new CampObject(objectId, definition, block, pos, facing, mode, budget, parts, false, List.of(), false);
    }

    public CampObject asManual() {
        return new CampObject(objectId, definition, block, pos, facing, mode, budget, parts, stored, snapshot, true);
    }

    public boolean deployed() {
        return !stored;
    }

    public boolean occupies(BlockPos target) {
        return !stored && parts.contains(target);
    }

    public CampObject asStored(BlockPos relative, Direction relativeFacing, List<PartSnapshot> parts) {
        return new CampObject(
            objectId,
            definition,
            block,
            relative,
            relativeFacing,
            mode,
            budget,
            List.of(),
            true,
            parts,
            manual
        );
    }

    public CampObject asDeployed(BlockPos world, Direction worldFacing, List<BlockPos> worldParts) {
        return new CampObject(
            objectId,
            definition,
            block,
            world,
            worldFacing,
            mode,
            budget,
            worldParts,
            false,
            List.of(),
            manual
        );
    }
}

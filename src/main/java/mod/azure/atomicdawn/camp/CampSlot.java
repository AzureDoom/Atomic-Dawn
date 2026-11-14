package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record CampSlot(
    UUID campId,
    UUID owner,
    String ownerName,
    int slotIndex,
    String name,
    ResourceKey<Level> dimension,
    BlockPos origin,
    Direction facing,
    boolean deployed,
    int budgetLimit,
    long createdAt,
    int version,
    List<CampObject> objects,
    CampAccess access
) {

    public static final int CURRENT_VERSION = 3;

    public static final int MAX_NAME_LENGTH = 32;

    public static final Codec<CampSlot> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("camp_id").forGetter(CampSlot::campId),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(CampSlot::owner),
            Codec.STRING.optionalFieldOf("owner_name", "").forGetter(CampSlot::ownerName),
            Codec.INT.optionalFieldOf("slot", 0).forGetter(CampSlot::slotIndex),
            Codec.STRING.optionalFieldOf("name", "").forGetter(CampSlot::name),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(CampSlot::dimension),
            BlockPos.CODEC.fieldOf("origin").forGetter(CampSlot::origin),
            Direction.CODEC.optionalFieldOf("facing", Direction.NORTH).forGetter(CampSlot::facing),
            Codec.BOOL.optionalFieldOf("deployed", true).forGetter(CampSlot::deployed),
            Codec.INT.optionalFieldOf("budget_limit", 500).forGetter(CampSlot::budgetLimit),
            Codec.LONG.optionalFieldOf("created_at", 0L).forGetter(CampSlot::createdAt),
            Codec.INT.optionalFieldOf("version", CURRENT_VERSION).forGetter(CampSlot::version),
            CampObject.CODEC.listOf().optionalFieldOf("objects", List.of()).forGetter(CampSlot::objects),
            CampAccess.CODEC.optionalFieldOf("access", CampAccess.DEFAULT).forGetter(CampSlot::access)
        ).apply(instance, CampSlot::new)
    );

    public CampSlot {
        origin = origin.immutable();
        facing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        name = name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
        objects = List.copyOf(objects);
        version = CURRENT_VERSION;
    }

    public boolean ownedBy(UUID id) {
        return owner.equals(id);
    }

    public boolean in(ResourceKey<Level> level) {
        return deployed && dimension.equals(level);
    }

    public int budgetUsed() {
        var total = 0;
        for (var object : objects) {
            total += object.budget();
        }
        return total;
    }

    public int budgetRemaining() {
        return Math.max(0, budgetLimit - budgetUsed());
    }

    public int count(Identifier definition) {
        var total = 0;
        for (var object : objects) {
            if (object.definition().equals(definition)) {
                total++;
            }
        }
        return total;
    }

    public int storedCount() {
        var total = 0;
        for (var object : objects) {
            if (object.stored()) {
                total++;
            }
        }
        return total;
    }

    public Optional<CampObject> objectAt(BlockPos pos) {
        for (var object : objects) {
            if (object.occupies(pos)) {
                return Optional.of(object);
            }
        }
        return Optional.empty();
    }

    private CampSlot copy(
        String ownerName,
        ResourceKey<Level> dimension,
        BlockPos origin,
        Direction facing,
        boolean deployed,
        List<CampObject> objects
    ) {
        return new CampSlot(
            campId,
            owner,
            ownerName,
            slotIndex,
            name,
            dimension,
            origin,
            facing,
            deployed,
            budgetLimit,
            createdAt,
            version,
            objects,
            access
        );
    }

    public CampSlot withAccess(CampAccess value) {
        return new CampSlot(
            campId,
            owner,
            ownerName,
            slotIndex,
            name,
            dimension,
            origin,
            facing,
            deployed,
            budgetLimit,
            createdAt,
            version,
            objects,
            value
        );
    }

    public CampSlot withOwnerName(String value) {
        return copy(value, dimension, origin, facing, deployed, objects);
    }

    public CampSlot withObjects(List<CampObject> value) {
        return copy(ownerName, dimension, origin, facing, deployed, value);
    }

    public CampSlot deployedAt(ResourceKey<Level> level, BlockPos pos, Direction direction) {
        return copy(ownerName, level, pos, direction, true, objects);
    }

    public CampSlot asStored(List<CampObject> value) {
        return copy(ownerName, dimension, origin, facing, false, value);
    }

    public CampSlot withObject(CampObject object) {
        var list = new ArrayList<>(objects);
        list.add(object);
        return withObjects(list);
    }

    public CampSlot withoutObject(UUID objectId) {
        return withObjects(objects.stream().filter(object -> !object.objectId().equals(objectId)).toList());
    }
}

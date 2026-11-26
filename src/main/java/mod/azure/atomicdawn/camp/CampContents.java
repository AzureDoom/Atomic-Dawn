package mod.azure.atomicdawn.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.powerarmor.PowerArmorChassisItem;
import mod.azure.atomicdawn.powerarmor.PowerArmorEntity;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationBlock;
import mod.azure.atomicdawn.registry.ADDataComponents;

public final class CampContents {

    public static final TagKey<EntityType<?>> STORABLE = TagKey.create(
        Registries.ENTITY_TYPE,
        AtomicDawn.id("camp_storable")
    );

    private static final String[] TRANSIENT_KEYS = { "UUID", "Pos", "Motion", "Passengers", "leash", "Leash" };

    private static final double ATTACH_RANGE_SQR = 8.0 * 8.0;

    private CampContents() {}

    public static CompoundTag blockData(ServerLevel level, BlockPos pos) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return new CompoundTag();
        }
        return blockEntity.saveWithoutMetadata(level.registryAccess());
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Clearable clearable) {
            clearable.clearContent();
        }
    }

    public static void applyBlockData(ServerLevel level, BlockPos pos, CompoundTag data) {
        if (data.isEmpty()) {
            return;
        }
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        try {
            var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data);
            blockEntity.loadWithComponents(input);
            blockEntity.setChanged();
            var state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        } catch (RuntimeException exception) {
            AtomicDawn.LOGGER.warn("Could not restore C.A.M.P. block data at {}", pos, exception);
        }
    }

    public static Map<UUID, List<Entity>> assign(
        ServerLevel level,
        List<CampObject> objects,
        AABB area,
        Predicate<BlockPos> inside
    ) {
        var result = new HashMap<UUID, List<Entity>>();
        var taken = new HashSet<UUID>();
        for (var object : objects) {
            var masters = new HashSet<BlockPos>();
            for (var part : object.parts()) {
                var state = level.getBlockState(part);
                var station = state.getBlock() instanceof PowerArmorStationBlock;
                if (station && CampWorkshopService.matches(level, part, object)) {
                    masters.add(PowerArmorStationBlock.masterPos(part, state));
                }
            }
            for (var master : masters) {
                var armor = PowerArmorStationBlock.docked(level, master);
                if (armor != null && armor.getPassengers().isEmpty() && taken.add(armor.getUUID())) {
                    result.computeIfAbsent(object.objectId(), _ -> new ArrayList<>()).add(armor);
                }
            }
        }
        var loose = level.getEntities(
            (Entity) null,
            area,
            entity -> BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(STORABLE)
                && !(entity instanceof PowerArmorEntity)
        );
        for (var entity : loose) {
            if (!eligible(entity) || taken.contains(entity.getUUID()) || !inside.test(entity.blockPosition())) {
                continue;
            }
            var owner = nearest(objects, entity.blockPosition());
            if (owner != null) {
                taken.add(entity.getUUID());
                result.computeIfAbsent(owner.objectId(), _ -> new ArrayList<>()).add(entity);
            }
        }
        return result;
    }

    private static boolean eligible(Entity entity) {
        return entity.isAlive() && !(entity instanceof Player) && !entity.isPassenger() && !entity.isVehicle();
    }

    @Nullable
    private static CampObject nearest(List<CampObject> objects, BlockPos pos) {
        CampObject best = null;
        var bestDistance = ATTACH_RANGE_SQR;
        for (var object : objects) {
            for (var part : object.parts()) {
                var distance = part.distSqr(pos);
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = object;
                }
            }
        }
        return best;
    }

    public static EntitySnapshot capture(ServerLevel level, Entity entity, BlockPos anchor, Rotation toNorth) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(output);
        var data = output.buildResult();
        for (var key : TRANSIENT_KEYS) {
            data.remove(key);
        }
        var relative = entity.position().subtract(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
        return new EntitySnapshot(
            EntityType.getKey(entity.getType()),
            CampNbt.rotate(relative, toNorth),
            toNorth,
            data
        );
    }

    @Nullable
    private static Entity create(ServerLevel level, EntitySnapshot snapshot) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(snapshot.type());
        if (type.isEmpty() || !BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type.get()).is(STORABLE)) {
            return null;
        }
        try {
            var entity = type.get().create(level, EntitySpawnReason.LOAD);
            if (entity == null) {
                return null;
            }
            if (!snapshot.data().isEmpty()) {
                entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), snapshot.data()));
            }
            return entity;
        } catch (RuntimeException exception) {
            AtomicDawn.LOGGER.warn("Could not restore C.A.M.P. entity {}", snapshot.type(), exception);
            return null;
        }
    }

    public static void spawn(
        ServerLevel level,
        EntitySnapshot snapshot,
        BlockPos anchor,
        Rotation fromNorth,
        List<BlockPos> parts
    ) {
        var entity = create(level, snapshot);
        if (entity == null) {
            return;
        }
        var offset = CampNbt.rotate(snapshot.offset(), fromNorth);
        var x = anchor.getX() + 0.5 + offset.x;
        var y = anchor.getY() + offset.y;
        var z = anchor.getZ() + 0.5 + offset.z;
        var yaw = entity.rotate(snapshot.rotation().getRotated(fromNorth));
        entity.setPos(x, y, z);
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = yaw;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof PowerArmorEntity armor) {
            var master = station(level, parts, entity.position());
            if (master != null) {
                var facing = level.getBlockState(master).getValue(PowerArmorStationBlock.FACING);
                armor.dock(master, PowerArmorStationBlock.dockPosition(master, facing), facing);
            } else {
                armor.undock();
            }
        }
        level.addFreshEntity(entity);
    }

    @Nullable
    private static BlockPos station(ServerLevel level, List<BlockPos> parts, Vec3 near) {
        BlockPos best = null;
        var bestDistance = Double.MAX_VALUE;
        for (var part : parts) {
            var state = level.getBlockState(part);
            if (!(state.getBlock() instanceof PowerArmorStationBlock) || !PowerArmorStationBlock.isMaster(state)) {
                continue;
            }
            var dock = PowerArmorStationBlock.dockPosition(part, state.getValue(PowerArmorStationBlock.FACING));
            var distance = dock.distanceToSqr(near);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = part;
            }
        }
        return best;
    }

    public static boolean returnsBlock() {
        return AtomicDawn.CONFIG.common.settlement.camp.building.CAMP_SCRAP_RETURNS_BLOCK;
    }

    public static CompoundTag objectData(ServerLevel level, CampObject object) {
        var primary = blockData(level, object.pos());
        if (!primary.isEmpty()) {
            return primary;
        }
        for (var part : object.parts()) {
            var data = blockData(level, part);
            if (!data.isEmpty()) {
                return data;
            }
        }
        return new CompoundTag();
    }

    public static CompoundTag storedData(List<PartSnapshot> parts, BlockPos primary) {
        for (var part : parts) {
            if (part.offset().equals(primary) && part.hasData()) {
                return part.data();
            }
        }
        for (var part : parts) {
            if (part.hasData()) {
                return part.data();
            }
        }
        return new CompoundTag();
    }

    public static ItemStack blockItem(Identifier block, CompoundTag data) {
        var item = BuiltInRegistries.BLOCK.getOptional(block).map(Block::asItem).orElse(Items.AIR);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        var stack = new ItemStack(item);
        if (!data.isEmpty()) {
            stack.set(ADDataComponents.CAMP_BLOCK_DATA.get(), data.copy());
        }
        return stack;
    }

    public static void applyItemData(ServerLevel level, BlockPos pos, ItemStack stack) {
        var data = stack.get(ADDataComponents.CAMP_BLOCK_DATA.get());
        if (data != null) {
            applyBlockData(level, pos, data);
        }
    }

    public static List<ItemStack> containerDrops(ServerLevel level, List<PartSnapshot> parts) {
        var items = new ArrayList<ItemStack>();
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (var part : parts) {
            for (var tag : part.data().getListOrEmpty("Items")) {
                if (tag instanceof CompoundTag compound) {
                    ItemStack.CODEC.parse(ops, compound)
                        .result()
                        .filter(stack -> !stack.isEmpty())
                        .ifPresent(items::add);
                }
            }
        }
        return items;
    }

    public static List<ItemStack> entityDrops(ServerLevel level, List<EntitySnapshot> entities) {
        var items = new ArrayList<ItemStack>();
        for (var snapshot : entities) {
            items.addAll(entityDrops(level, snapshot));
        }
        return items;
    }

    private static List<ItemStack> entityDrops(ServerLevel level, EntitySnapshot snapshot) {
        var entity = create(level, snapshot);
        if (entity == null) {
            return List.of();
        }
        if (entity instanceof PowerArmorEntity armor) {
            return List.of(PowerArmorChassisItem.fromEntity(armor));
        }
        var items = new ArrayList<ItemStack>();
        var pick = entity.getPickResult();
        if (pick != null && !pick.isEmpty()) {
            items.add(pick.copy());
        }
        if (entity instanceof LivingEntity living) {
            for (var slot : EquipmentSlot.values()) {
                var stack = living.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    items.add(stack.copy());
                }
            }
        }
        if (entity instanceof ItemFrame frame && !frame.getItem().isEmpty()) {
            items.add(frame.getItem().copy());
        }
        return items;
    }
}

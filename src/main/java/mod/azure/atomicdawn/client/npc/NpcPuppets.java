package mod.azure.atomicdawn.client.npc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcAppearance;
import mod.azure.atomicdawn.npc.NpcBehavior;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.npc.NpcIds;

public final class NpcPuppets {

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET };

    private record Puppet(
        @Nullable Entity entity,
        NpcAppearance appearance
    ) {}

    private static final Map<Integer, Puppet> PUPPETS = new HashMap<>();

    @Nullable
    private static Level lastLevel;

    private NpcPuppets() {}

    @Nullable
    public static Entity get(NpcEntity npc, NpcAppearance appearance) {
        var level = npc.level();
        if (level != lastLevel) {
            PUPPETS.clear();
            lastLevel = level;
        }
        var existing = PUPPETS.get(npc.getId());
        if (existing != null && existing.appearance().equals(appearance)) {
            return existing.entity();
        }
        var puppet = create(level, appearance);
        if (puppet == null && appearance.kind() == NpcAppearance.Kind.ENTITY) {
            puppet = create(level, NpcAppearance.DEFAULT);
        }
        PUPPETS.put(npc.getId(), new Puppet(puppet, appearance));
        if (puppet != null) {
            puppet.setId(-npc.getId() - 1);
            sync(npc, puppet);
        }
        return puppet;
    }

    public static void markBroken(NpcEntity npc, NpcAppearance appearance) {
        PUPPETS.put(npc.getId(), new Puppet(null, appearance));
    }

    @Nullable
    private static Entity create(Level level, NpcAppearance appearance) {
        var model = appearance.kind() == NpcAppearance.Kind.ENTITY ? appearance.model() : NpcAppearance.DEFAULT_MODEL;
        if (model.equals(NpcIds.NPC_TYPE)) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(model).orElse(null);
        if (type == null) {
            return null;
        }
        try {
            var entity = type.create(level, EntitySpawnReason.LOAD);
            if (entity == null) {
                return null;
            }
            var data = appearance.kind() == NpcAppearance.Kind.ENTITY ? appearance.data() : null;
            if (data != null && !data.isEmpty()) {
                entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
            }
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
            entity.setInvisible(false);
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
            }
            return entity;
        } catch (RuntimeException exception) {
            AtomicDawn.LOGGER.warn("Could not create NPC puppet for {}", model, exception);
            return null;
        }
    }

    public static void tick() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            PUPPETS.clear();
            lastLevel = null;
            return;
        }
        var iterator = PUPPETS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!(level.getEntity(entry.getKey()) instanceof NpcEntity npc) || npc.isRemoved()) {
                iterator.remove();
                continue;
            }
            var puppet = entry.getValue().entity();
            if (puppet != null) {
                try {
                    sync(npc, puppet);
                } catch (RuntimeException exception) {
                    entry.setValue(new Puppet(null, entry.getValue().appearance()));
                }
            }
        }
    }

    private static void sync(NpcEntity npc, Entity puppet) {
        puppet.setPos(npc.getX(), npc.getY(), npc.getZ());
        puppet.xo = npc.xo;
        puppet.yo = npc.yo;
        puppet.zo = npc.zo;
        puppet.xOld = npc.xOld;
        puppet.yOld = npc.yOld;
        puppet.zOld = npc.zOld;
        puppet.setYRot(npc.getYRot());
        puppet.yRotO = npc.yRotO;
        puppet.setXRot(npc.getXRot());
        puppet.xRotO = npc.xRotO;
        puppet.tickCount = npc.tickCount;
        puppet.setOnGround(npc.onGround());
        if (!(puppet instanceof LivingEntity living)) {
            return;
        }
        living.yBodyRot = npc.yBodyRot;
        living.yBodyRotO = npc.yBodyRotO;
        living.yHeadRot = npc.yHeadRot;
        living.yHeadRotO = npc.yHeadRotO;
        living.hurtTime = npc.hurtTime;
        living.deathTime = npc.deathTime;
        LivingEntity.SwingDescription swing = npc.getCurrentSwing();

        if (swing != null) {
            LivingEntity.SwingDescription current = living.getCurrentSwing();

            if (current == null || !current.equals(swing)) {
                living.swing(
                    swing.hand(),
                    swing.animation(),
                    false
                );
            }
        }
        for (var slot : SLOTS) {
            var wanted = npc.getItemBySlot(slot);
            if (!ItemStack.matches(living.getItemBySlot(slot), wanted)) {
                living.setItemSlot(slot, wanted.copy());
            }
        }
        var pose = NpcClient.pose(npc);
        var crouching = pose == NpcBehavior.NpcPose.CROUCHING;
        living.setShiftKeyDown(crouching);
        living.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
        if (living instanceof TamableAnimal tamable) {
            tamable.setInSittingPose(pose == NpcBehavior.NpcPose.SITTING);
        }
        if (living instanceof Mob mob) {
            mob.setAggressive(npc.isAggressive());
            mob.setLeftHanded(npc.isLeftHanded());
        }
        living.calculateEntityAnimation(false);
    }
}

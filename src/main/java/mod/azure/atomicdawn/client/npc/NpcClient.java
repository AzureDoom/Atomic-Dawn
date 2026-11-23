package mod.azure.atomicdawn.client.npc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.network.NpcDialogueChoicePayload;
import mod.azure.atomicdawn.network.NpcDialoguePayload;
import mod.azure.atomicdawn.network.NpcEditorPayload;
import mod.azure.atomicdawn.network.NpcPickerPayload;
import mod.azure.atomicdawn.npc.NpcAppearance;
import mod.azure.atomicdawn.npc.NpcBehavior;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.npc.NpcIds;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class NpcClient {

    private static final Map<Integer, NpcAppearance> PREVIEW = new HashMap<>();

    private static final Map<Integer, NpcBehavior.NpcPose> POSE_PREVIEW = new HashMap<>();

    private static List<Identifier> livingTypes;

    private NpcClient() {}

    public static NpcAppearance appearance(NpcEntity npc) {
        var preview = PREVIEW.get(npc.getId());
        return preview != null ? preview : npc.appearance();
    }

    public static NpcBehavior.NpcPose pose(NpcEntity npc) {
        var preview = POSE_PREVIEW.get(npc.getId());
        return preview != null ? preview : npc.npcPose();
    }

    public static void previewPose(int entityId, NpcBehavior.NpcPose pose) {
        POSE_PREVIEW.put(entityId, pose);
    }

    public static void preview(int entityId, NpcAppearance appearance) {
        PREVIEW.put(entityId, appearance);
    }

    public static void clearPreview(int entityId) {
        PREVIEW.remove(entityId);
        POSE_PREVIEW.remove(entityId);
    }

    public static void onDialogue(NpcDialoguePayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        var current = minecraft.gui.screen();
        if (!payload.open()) {
            if (current instanceof NpcDialogueScreen screen && screen.entityId() == payload.entityId()) {
                screen.closeFromServer();
            }
            return;
        }
        if (current instanceof NpcDialogueScreen screen && screen.entityId() == payload.entityId()) {
            screen.update(payload);
            return;
        }
        minecraft.gui.setScreen(new NpcDialogueScreen(payload));
    }

    public static void onEditor(NpcEditorPayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.gui.setScreen(new NpcEditorScreen(payload.entityId(), payload.config()));
    }

    public static void onPicker(NpcPickerPayload payload) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.gui.setScreen(new NpcPickerScreen(payload.entries()));
    }

    public static void choose(int entityId, int choice) {
        ClientPacketDistributor.sendToServer(new NpcDialogueChoicePayload(entityId, choice));
    }

    public static List<Identifier> livingTypes() {
        if (livingTypes != null) {
            return livingTypes;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        var result = new ArrayList<Identifier>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id.equals(NpcIds.NPC_TYPE) || type == EntityTypes.PLAYER) {
                continue;
            }
            try {
                var entity = type.create(level, EntitySpawnReason.LOAD);
                if (entity instanceof LivingEntity) {
                    result.add(id);
                }
                if (entity != null) {
                    entity.discard();
                }
            } catch (RuntimeException exception) {
                AtomicDawn.LOGGER.debug("Skipping NPC appearance candidate {}", id, exception);
            }
        }
        result.sort(Comparator.comparing(Identifier::toString));
        livingTypes = List.copyOf(result);
        return livingTypes;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            PREVIEW.clear();
        }
        NpcPuppets.tick();
    }
}

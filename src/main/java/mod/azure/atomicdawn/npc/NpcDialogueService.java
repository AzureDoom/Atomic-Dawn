package mod.azure.atomicdawn.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.NpcDialoguePayload;
import mod.azure.atomicdawn.npc.dialogue.NpcAction;
import mod.azure.atomicdawn.npc.dialogue.NpcCondition;
import mod.azure.atomicdawn.npc.dialogue.NpcDialogue;
import mod.azure.atomicdawn.quest.QuestService;
import mod.azure.atomicdawn.special.SpecialService;
import mod.azure.atomicdawn.story.StoryService;

@SuppressWarnings("unused")
public final class NpcDialogueService {

    public static final int MAX_OPTIONS = 9;

    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private static final int GOODBYE = -1;

    private static final int HIRE = -2;

    private static final int FOLLOW = -3;

    private static final int WAIT = -4;

    private static final int DISMISS = -5;

    private static final int TALK = -6;

    private static final int TRADE = -7;

    private record Session(
        int entityId,
        Identifier dialogue,
        String node,
        int[] options
    ) {}

    private static final class Menu {

        final List<Integer> codes = new ArrayList<>();

        final List<Component> labels = new ArrayList<>();

        void add(int code, Component label) {
            if (codes.size() < MAX_OPTIONS) {
                codes.add(code);
                labels.add(label);
            }
        }

        int[] codes() {
            return codes.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private NpcDialogueService() {}

    public static boolean isTalking(ServerPlayer player, NpcEntity npc) {
        var session = SESSIONS.get(player.getUUID());
        return session != null && session.entityId() == npc.getId();
    }

    public static void open(ServerPlayer player, NpcEntity npc) {
        if (npc.isDowned() || npc.isHostile()) {
            return;
        }
        if (!npc.isOwnedBy(player) && StoryService.hostile(npc, player)) {
            player.sendSystemMessage(
                Component.translatable("faction.atomicdawn.refuses", npc.getDisplayName())
                    .withStyle(ChatFormatting.RED),
                true
            );
            return;
        }
        var previous = SESSIONS.remove(player.getUUID());
        if (previous != null && previous.entityId() != npc.getId()) {
            stopTalking(player, previous.entityId());
        }
        npc.config().identifier().ifPresent(id -> QuestService.onTalk(player, id));
        if (npc.isOwnedBy(player)) {
            npc.startTalking(player);
            companionMenu(player, npc);
            return;
        }
        openConversation(player, npc);
    }

    private static void openConversation(ServerPlayer player, NpcEntity npc) {
        var interaction = npc.config().interaction();
        var dialogueId = interaction.dialogue().orElse(null);
        var dialogue = dialogueId == null
            ? null
            : NpcDialogue.get(player.level().registryAccess(), dialogueId).orElse(null);
        if (dialogue != null) {
            var start = dialogue.startNode(player).orElse(null);
            if (start != null) {
                npc.startTalking(player);
                show(player, npc, dialogueId, dialogue, start, true, null);
                return;
            }
        }
        var menu = new Menu();
        services(player, npc, menu);
        if (interaction.greeting().isEmpty() && menu.codes.isEmpty()) {
            return;
        }
        menu.add(GOODBYE, goodbye());
        npc.startTalking(player);
        SESSIONS.put(player.getUUID(), new Session(npc.getId(), null, "", menu.codes()));
        var text = interaction.greeting().isEmpty()
            ? Component.translatable("npc.atomicdawn.dialogue.default_greeting")
            : text(interaction.greeting(), player, npc);
        send(player, npc, text, menu.labels);
    }

    private static void services(ServerPlayer player, NpcEntity npc, Menu menu) {
        if (!npc.config().shop().isEmpty()) {
            menu.add(TRADE, Component.translatable("npc.atomicdawn.dialogue.trade"));
        }
        if (NpcCompanions.canHire(player, npc)) {
            menu.add(HIRE, NpcCompanions.hireLabel(npc));
        }
    }

    private static void companionMenu(ServerPlayer player, NpcEntity npc) {
        var menu = new Menu();
        if (npc.companionMode() == NpcEntity.CompanionMode.FOLLOW) {
            menu.add(WAIT, Component.translatable("npc.atomicdawn.dialogue.wait"));
        } else {
            menu.add(FOLLOW, Component.translatable("npc.atomicdawn.dialogue.follow"));
        }
        if (!npc.config().shop().isEmpty()) {
            menu.add(TRADE, Component.translatable("npc.atomicdawn.dialogue.trade"));
        }
        var interaction = npc.config().interaction();
        if (interaction.dialogue().isPresent() || !interaction.greeting().isEmpty()) {
            menu.add(TALK, Component.translatable("npc.atomicdawn.dialogue.talk"));
        }
        menu.add(DISMISS, Component.translatable("npc.atomicdawn.dialogue.dismiss"));
        menu.add(GOODBYE, goodbye());
        SESSIONS.put(player.getUUID(), new Session(npc.getId(), null, "", menu.codes()));
        Component status = npc.hireExpires() > 0L
            ? Component.translatable(
                "npc.atomicdawn.dialogue.companion_timed",
                Math.max(1L, (npc.hireExpires() - npc.level().getGameTime()) / 1200L)
            )
            : Component.translatable("npc.atomicdawn.dialogue.companion");
        send(player, npc, status, menu.labels);
    }

    private static void show(
        ServerPlayer player,
        NpcEntity npc,
        Identifier dialogueId,
        NpcDialogue dialogue,
        String nodeId,
        boolean root,
        @Nullable Component prefix
    ) {
        var node = dialogue.nodes().get(nodeId);
        if (node == null) {
            close(player);
            return;
        }
        for (var action : node.actions()) {
            if (!handle(player, npc, action.run(player, npc))) {
                return;
            }
        }
        var menu = new Menu();
        for (var i = 0; i < node.options().size(); i++) {
            var option = node.options().get(i);
            if (!NpcCondition.all(option.conditions(), player, npc)) {
                continue;
            }
            var check = option.check().orElse(null);
            if (check == null) {
                menu.add(i, text(option.text(), player, npc));
                continue;
            }
            if (
                !check.visible(player, npc) || (check.oneShot() && StoryService.attempted(
                    player,
                    attemptKey(dialogueId, nodeId, i)
                ))
            ) {
                continue;
            }
            menu.add(i, check.label(player, npc).append(text(option.text(), player, npc)));
        }
        if (root) {
            services(player, npc, menu);
        }
        if (menu.codes.isEmpty()) {
            menu.add(GOODBYE, goodbye());
        }
        SESSIONS.put(player.getUUID(), new Session(npc.getId(), dialogueId, nodeId, menu.codes()));
        var body = text(node.text(), player, npc);
        send(player, npc, prefix == null ? body : prefix.copy().append(body), menu.labels);
    }

    private static String attemptKey(Identifier dialogue, String node, int index) {
        return dialogue + "#" + node + "#" + index;
    }

    private static boolean handle(ServerPlayer player, NpcEntity npc, NpcAction.Outcome outcome) {
        switch (outcome) {
            case CONTINUE -> {
                return true;
            }
            case OPEN_SHOP -> {
                close(player);
                NpcShops.open(player, npc);
                return false;
            }
            default -> {
                close(player);
                return false;
            }
        }
    }

    public static void choose(ServerPlayer player, int entityId, int choice) {
        var session = SESSIONS.get(player.getUUID());
        if (session == null || session.entityId() != entityId) {
            return;
        }
        if (
            !(player.level().getEntity(entityId) instanceof NpcEntity npc) || !npc.isAlive() || npc.isDowned()
                || npc.distanceToSqr(player) > MAX_DISTANCE_SQR || choice < 0 || choice >= session.options().length
        ) {
            close(player);
            return;
        }
        var code = session.options()[choice];
        switch (code) {
            case GOODBYE -> close(player);
            case TRADE -> {
                close(player);
                NpcShops.open(player, npc);
            }
            case HIRE -> {
                close(player);
                NpcCompanions.hire(player, npc);
            }
            case FOLLOW -> {
                close(player);
                NpcCompanions.setMode(player, npc, NpcEntity.CompanionMode.FOLLOW);
            }
            case WAIT -> {
                close(player);
                NpcCompanions.setMode(player, npc, NpcEntity.CompanionMode.WAIT);
            }
            case DISMISS -> {
                close(player);
                NpcCompanions.dismiss(player, npc);
            }
            case TALK -> {
                SESSIONS.remove(player.getUUID());
                openConversation(player, npc);
                if (!SESSIONS.containsKey(player.getUUID())) {
                    stopTalking(player, entityId);
                    ADNetwork.sendTo(player, NpcDialoguePayload.closed(entityId));
                }
            }
            default -> chooseOption(player, npc, session, code);
        }
    }

    private static void chooseOption(ServerPlayer player, NpcEntity npc, Session session, int index) {
        if (session.dialogue() == null) {
            close(player);
            return;
        }
        var dialogue = NpcDialogue.get(player.level().registryAccess(), session.dialogue()).orElse(null);
        var node = dialogue == null ? null : dialogue.nodes().get(session.node());
        if (node == null || index < 0 || index >= node.options().size()) {
            close(player);
            return;
        }
        var option = node.options().get(index);
        if (!NpcCondition.all(option.conditions(), player, npc)) {
            show(player, npc, session.dialogue(), dialogue, session.node(), false, null);
            return;
        }
        var check = option.check().orElse(null);
        var key = attemptKey(session.dialogue(), session.node(), index);
        if (check != null && check.oneShot() && StoryService.attempted(player, key)) {
            show(player, npc, session.dialogue(), dialogue, session.node(), false, null);
            return;
        }
        for (var action : option.actions()) {
            if (!handle(player, npc, action.run(player, npc))) {
                return;
            }
        }
        var next = option.next();
        Component prefix = null;
        if (check != null) {
            var passed = check.roll(player, npc);
            if (check.oneShot()) {
                StoryService.markAttempted(player, key);
            }
            if (passed && check.experience() > 0 && !StoryService.attempted(player, "xp#" + key)) {
                StoryService.markAttempted(player, "xp#" + key);
                SpecialService.awardXp(player, check.experience());
            }
            prefix = Component.translatable(
                passed ? "dialogue.atomicdawn.check.succeeded" : "dialogue.atomicdawn.check.failed"
            ).withStyle(passed ? ChatFormatting.GREEN : ChatFormatting.RED).append(" ");
            for (var action : passed ? check.successActions() : check.failureActions()) {
                if (!handle(player, npc, action.run(player, npc))) {
                    return;
                }
            }
            var branch = passed ? check.success() : check.failure();
            if (branch.isPresent()) {
                next = branch;
            }
        }
        if (next.isPresent()) {
            show(player, npc, session.dialogue(), dialogue, next.get(), false, prefix);
        } else {
            close(player);
        }
    }

    public static void close(ServerPlayer player) {
        var session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        stopTalking(player, session.entityId());
        ADNetwork.sendTo(player, NpcDialoguePayload.closed(session.entityId()));
    }

    public static void forget(UUID id) {
        SESSIONS.remove(id);
    }

    private static void stopTalking(ServerPlayer player, int entityId) {
        if (player.level().getEntity(entityId) instanceof NpcEntity npc) {
            npc.stopTalking(player.getUUID());
        }
    }

    private static void send(ServerPlayer player, NpcEntity npc, Component text, List<Component> options) {
        var name = npc.hasCustomName() && npc.getCustomName() != null
            ? npc.getCustomName()
            : npc.getType().getDescription();
        ADNetwork.sendTo(
            player,
            new NpcDialoguePayload(true, npc.getId(), name, Component.literal(npc.title()), text, options)
        );
    }

    private static Component goodbye() {
        return Component.translatable("npc.atomicdawn.dialogue.goodbye");
    }

    private static Component text(String raw, ServerPlayer player, NpcEntity npc) {
        var npcName = npc.hasCustomName() && npc.getCustomName() != null ? npc.getCustomName().getString() : "";
        var fallback = raw.replace("%player%", player.getScoreboardName()).replace("%npc%", npcName);
        return Component.translatableWithFallback(raw, fallback, player.getScoreboardName(), npcName);
    }
}

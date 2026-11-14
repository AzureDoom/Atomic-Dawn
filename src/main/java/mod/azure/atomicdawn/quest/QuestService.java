package mod.azure.atomicdawn.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.Optional;

import mod.azure.atomicdawn.item.CampPlanItem;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.pipboy.PipBoyNotifications;
import mod.azure.atomicdawn.pipboy.holotape.HolotapeService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.special.SpecialService;

public final class QuestService {

    private QuestService() {}

    public static QuestLog log(ServerPlayer player) {
        return player.getData(ADAttachments.QUESTS.get());
    }

    private static void set(ServerPlayer player, QuestLog log) {
        player.setData(ADAttachments.QUESTS.get(), log);
    }

    public static boolean start(ServerPlayer player, Identifier id) {
        var access = player.level().registryAccess();
        var definition = QuestDefinition.get(access, id).orElse(null);
        var log = log(player);
        if (definition == null || log.quests().containsKey(id) || log.quests().size() >= QuestLog.MAX_QUESTS) {
            return false;
        }
        set(player, log.with(id, QuestProgress.start(definition.objectives().size())));
        PipBoyNotifications.send(player, PipBoyNotifications.Kind.QUEST_STARTED, 0, PipBoyNames.key("quest", id));
        evaluate(player);
        return true;
    }

    public static boolean reset(ServerPlayer player, Identifier id) {
        var log = log(player);
        if (!log.quests().containsKey(id)) {
            return false;
        }
        set(player, log.without(id));
        return true;
    }

    public static boolean forceComplete(ServerPlayer player, Identifier id) {
        var definition = QuestDefinition.get(player.level().registryAccess(), id).orElse(null);
        if (definition == null || log(player).isCompleted(id)) {
            return false;
        }
        if (!log(player).quests().containsKey(id)) {
            set(player, log(player).with(id, QuestProgress.start(definition.objectives().size())));
        }
        finish(player, id, definition);
        return true;
    }

    public static void track(ServerPlayer player, Optional<Identifier> id) {
        var log = log(player);
        if (id.isPresent() && log.get(id.get()).map(QuestProgress::completed).orElse(true)) {
            return;
        }
        set(player, log.withTracked(id));
    }

    public static void autoStart(ServerPlayer player) {
        var log = log(player);
        for (var holder : QuestDefinition.all(player.level().registryAccess())) {
            var id = holder.key().identifier();
            var definition = holder.value();
            if (!definition.autoStart() || log.quests().containsKey(id)) {
                continue;
            }
            if (definition.prerequisites().stream().allMatch(log::isCompleted)) {
                start(player, id);
                log = log(player);
            }
        }
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        var log = log(player);
        var access = player.level().registryAccess();
        var changed = false;
        for (var entry : log.quests().entrySet()) {
            var progress = entry.getValue();
            if (progress.completed()) {
                continue;
            }
            var definition = QuestDefinition.get(access, entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            var objectives = definition.objectives();
            for (var i = 0; i < objectives.size(); i++) {
                var objective = objectives.get(i);
                if (!active(definition, progress, i) || objective.type() != ObjectiveType.KILL) {
                    continue;
                }
                if (objective.target().map(target -> target.matchesEntity(victim.getType())).orElse(false)) {
                    if (progress.count(i) < objective.count()) {
                        progress = progress.withCount(i, progress.count(i) + 1);
                        changed = true;
                    }
                }
            }
            log = log.with(entry.getKey(), progress);
        }
        if (changed) {
            set(player, log);
            evaluate(player);
        }
    }

    public static void onTalk(ServerPlayer player, Identifier npcId) {
        var log = log(player);
        var access = player.level().registryAccess();
        var changed = false;
        for (var entry : log.quests().entrySet()) {
            var progress = entry.getValue();
            if (progress.completed()) {
                continue;
            }
            var definition = QuestDefinition.get(access, entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            var objectives = definition.objectives();
            for (var i = 0; i < objectives.size(); i++) {
                var objective = objectives.get(i);
                if (!active(definition, progress, i) || objective.type() != ObjectiveType.TALK) {
                    continue;
                }
                var matches = objective.target()
                    .map(target -> !target.tag() && target.id().equals(npcId))
                    .orElse(false);
                if (matches && progress.count(i) < objective.count()) {
                    progress = progress.withCount(i, progress.count(i) + 1);
                    changed = true;
                }
            }
            log = log.with(entry.getKey(), progress);
        }
        if (changed) {
            set(player, log);
            evaluate(player);
        }
    }

    private static boolean active(QuestDefinition definition, QuestProgress progress, int index) {
        return !definition.sequential() || progress.stage() == index;
    }

    public static void evaluate(ServerPlayer player) {
        var access = player.level().registryAccess();
        var pending = new ArrayDeque<Identifier>();
        var log = log(player);
        var changed = false;
        for (var entry : log.quests().entrySet()) {
            if (entry.getValue().completed()) {
                continue;
            }
            var definition = QuestDefinition.get(access, entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            var before = entry.getValue();
            var progress = before;
            var objectives = definition.objectives();
            var advanced = true;
            while (advanced) {
                advanced = false;
                for (var i = 0; i < objectives.size(); i++) {
                    if (!active(definition, progress, i)) {
                        continue;
                    }
                    var objective = objectives.get(i);
                    var current = progress.count(i);
                    if (current < objective.count()) {
                        var polled = poll(player, objective);
                        if (polled >= 0) {
                            var value = Math.min(
                                objective.count(),
                                objective.type() == ObjectiveType.KILL ? current : polled
                            );
                            if (objective.type() != ObjectiveType.COLLECT) {
                                value = Math.max(current, value);
                            }
                            progress = progress.withCount(i, value);
                        }
                    }
                    if (definition.sequential() && progress.count(i) >= objective.count() && progress.stage() == i) {
                        progress = progress.withStage(i + 1);
                        advanced = true;
                        if (i + 1 < objectives.size()) {
                            PipBoyNotifications.send(
                                player,
                                PipBoyNotifications.Kind.QUEST_UPDATED,
                                0,
                                PipBoyNames.key("quest", entry.getKey())
                            );
                        }
                    }
                }
            }
            var done = true;
            for (var i = 0; i < objectives.size(); i++) {
                done &= progress.count(i) >= objectives.get(i).count();
            }
            if (!progress.equals(before)) {
                log = log.with(entry.getKey(), progress);
                changed = true;
            }
            if (done) {
                pending.add(entry.getKey());
            }
        }
        if (changed) {
            set(player, log);
        }
        for (var id : pending) {
            QuestDefinition.get(access, id).ifPresent(definition -> finish(player, id, definition));
        }
    }

    private static int poll(ServerPlayer player, QuestObjective objective) {
        var target = objective.target().orElse(null);
        return switch (objective.type()) {
            case KILL, TALK -> -1;
            case COLLECT -> {
                var total = 0;
                var inventory = player.getInventory();
                for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
                    var stack = inventory.getItem(slot);
                    if (target != null && target.matchesItem(stack)) {
                        total += stack.getCount();
                    }
                }
                yield total;
            }
            case DISCOVER -> {
                var access = player.level().registryAccess();
                var count = 0;
                for (var location : player.getData(ADAttachments.LOCATIONS.get()).discovered()) {
                    if (target != null && target.matchesStructure(access, location.structure())) {
                        count++;
                    }
                }
                yield count;
            }
            case VISIT_BIOME -> target != null && target.matchesBiome(player.level().getBiome(player.blockPosition()))
                ? 1
                : 0;
            case REACH_LEVEL -> SpecialService.get(player).level();
            case ESCORT -> escorted(player, objective) ? 1 : 0;
            case HOLOTAPE -> target != null && HolotapeService.log(player).has(target.id()) ? 1 : 0;
            case STUDY -> {
                var bestiary = player.getData(ADAttachments.BESTIARY.get());
                var best = 0;
                for (var entry : bestiary.kills().entrySet()) {
                    var type = BuiltInRegistries.ENTITY_TYPE.getOptional(entry.getKey()).orElse(null);
                    if (type != null && target != null && target.matchesEntity(type)) {
                        best = Math.max(best, entry.getValue());
                    }
                }
                yield best;
            }
        };
    }

    private static boolean escorted(ServerPlayer player, QuestObjective objective) {
        var target = objective.target().orElse(null);
        var location = objective.location().orElse(null);
        if (target == null || location == null) {
            return false;
        }
        var level = player.level();
        if (location.dimension().isPresent() && !level.dimension().identifier().equals(location.dimension().get())) {
            return false;
        }
        var box = new AABB(location.pos()).inflate(location.radius());
        return !level.getEntitiesOfClass(
            NpcEntity.class,
            box,
            npc -> npc.isOwnedBy(player) && npc.config().identifier().map(target.id()::equals).orElse(false)
        ).isEmpty();
    }

    private static void finish(ServerPlayer player, Identifier id, QuestDefinition definition) {
        var log = log(player);
        var progress = log.get(id).orElse(QuestProgress.start(definition.objectives().size()));
        if (progress.completed()) {
            return;
        }
        for (var i = 0; i < definition.objectives().size(); i++) {
            progress = progress.withCount(i, Math.max(progress.count(i), definition.objectives().get(i).count()));
        }
        log = log.with(id, progress.withStage(definition.objectives().size()).complete());
        if (log.tracked().map(id::equals).orElse(false)) {
            log = log.withTracked(Optional.empty());
        }
        set(player, log);
        PipBoyNotifications.send(player, PipBoyNotifications.Kind.QUEST_COMPLETED, 0, PipBoyNames.key("quest", id));
        grant(player, definition.rewards());
        autoStart(player);
    }

    private static void grant(ServerPlayer player, QuestReward reward) {
        if (reward.xp() > 0) {
            SpecialService.awardXp(player, reward.xp());
        }
        if (reward.perkPicks() > 0) {
            SpecialService.grantPerkPicks(player, reward.perkPicks());
        }
        for (var item : reward.items()) {
            BuiltInRegistries.ITEM.getOptional(item.item())
                .ifPresent(value -> give(player, new ItemStack(value, item.count())));
        }
        for (var holotape : reward.holotapes()) {
            give(player, HolotapeService.stack(holotape));
        }
        for (var plan : reward.plans()) {
            give(player, CampPlanItem.stack(plan));
        }
        for (var next : reward.startQuests()) {
            start(player, next);
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }
}

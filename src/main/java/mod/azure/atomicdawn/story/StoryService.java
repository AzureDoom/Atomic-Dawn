package mod.azure.atomicdawn.story;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcCompanions;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADRegistries;

public final class StoryService {

    private StoryService() {}

    public static StoryState state(Player player) {
        return player.getData(ADAttachments.STORY.get());
    }

    private static void set(Player player, StoryState state) {
        player.setData(ADAttachments.STORY.get(), state);
    }

    public static List<Holder.Reference<Faction>> factions(RegistryAccess access) {
        return access.lookup(ADRegistries.FACTION).map(registry -> registry.listElements().toList()).orElse(List.of());
    }

    public static Optional<Faction> faction(RegistryAccess access, Identifier id) {
        return access.lookup(ADRegistries.FACTION)
            .flatMap(registry -> registry.get(ResourceKey.create(ADRegistries.FACTION, id)))
            .map(Holder::value);
    }

    public static Optional<CompanionProfile> companion(RegistryAccess access, Identifier npc) {
        return access.lookup(ADRegistries.COMPANION)
            .flatMap(
                registry -> registry.listElements()
                    .map(Holder::value)
                    .filter(profile -> profile.npc().equals(npc))
                    .findFirst()
            );
    }

    public static int reputation(Player player, Identifier faction) {
        var stored = state(player).reputation().get(faction);
        if (stored != null) {
            return stored;
        }
        return faction(player.level().registryAccess(), faction).map(Faction::start).orElse(0);
    }

    public static Optional<Faction.Rank> rank(Player player, Identifier faction) {
        return faction(player.level().registryAccess(), faction).map(value -> value.rank(reputation(player, faction)));
    }

    public static void setReputation(ServerPlayer player, Identifier id, int value) {
        faction(player.level().registryAccess(), id).ifPresent(
            faction -> set(player, state(player).withReputation(id, faction.clamp(value)))
        );
    }

    public static void addReputation(ServerPlayer player, Identifier id, int amount) {
        change(player, id, amount, true);
    }

    private static void change(ServerPlayer player, Identifier id, int amount, boolean spill) {
        var access = player.level().registryAccess();
        var faction = faction(access, id).orElse(null);
        if (faction == null || amount == 0) {
            return;
        }
        var scaled = Math.round(amount * (float) AtomicDawn.CONFIG.story.REPUTATION_MULTIPLIER);
        if (scaled == 0) {
            return;
        }
        var before = reputation(player, id);
        var after = faction.clamp(before + scaled);
        if (after == before) {
            return;
        }
        set(player, state(player).withReputation(id, after));
        if (AtomicDawn.CONFIG.story.REPUTATION_NOTIFICATIONS) {
            var oldRank = faction.rank(before);
            var newRank = faction.rank(after);
            var name = Component.translatableWithFallback(faction.name(), faction.name());
            var message = oldRank.equals(newRank)
                ? Component.translatable(
                    after > before ? "faction.atomicdawn.gained" : "faction.atomicdawn.lost",
                    name
                )
                : Component.translatable(
                    "faction.atomicdawn.rank_changed",
                    name,
                    Component.translatableWithFallback(newRank.name(), newRank.name())
                );
            player.sendSystemMessage(
                message.withStyle(after > before ? ChatFormatting.GREEN : ChatFormatting.RED),
                true
            );
        }
        reactToReputation(player, id, after - before);
        if (!spill || faction.spillover() <= 0F) {
            return;
        }
        var spilled = Math.round((after - before) * faction.spillover());
        if (spilled == 0) {
            return;
        }
        for (var ally : faction.allies()) {
            change(player, ally, Math.round(spilled / (float) AtomicDawn.CONFIG.story.REPUTATION_MULTIPLIER), false);
        }
        for (var enemy : faction.enemies()) {
            change(player, enemy, Math.round(-spilled / (float) AtomicDawn.CONFIG.story.REPUTATION_MULTIPLIER), false);
        }
    }

    public static List<Identifier> factionsOf(Entity entity) {
        var result = new ArrayList<Identifier>();
        var npcId = entity instanceof NpcEntity npc ? npc.config().identifier().orElse(null) : null;
        var type = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType());
        for (var holder : factions(entity.level().registryAccess())) {
            var faction = holder.value();
            if ((npcId != null && faction.npcs().contains(npcId)) || faction.entities().contains(type)) {
                result.add(holder.key().identifier());
            }
        }
        return result;
    }

    public static boolean hostile(Entity entity, Player player) {
        if (!AtomicDawn.CONFIG.story.FACTION_HOSTILITY || player.isCreative() || player.isSpectator()) {
            return false;
        }
        for (var id : factionsOf(entity)) {
            if (rank(player, id).map(Faction.Rank::hostile).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    public static void onKill(ServerPlayer player, LivingEntity victim) {
        var access = player.level().registryAccess();
        for (var id : factionsOf(victim)) {
            faction(access, id).ifPresent(faction -> addReputation(player, id, faction.killPenalty()));
        }
    }

    public static int affinity(Player player, Identifier npc) {
        return state(player).affinity().getOrDefault(npc, 0);
    }

    public static void setAffinity(ServerPlayer player, Identifier npc, int value) {
        set(player, state(player).withAffinity(npc, Math.clamp(value, CompanionProfile.MIN, CompanionProfile.MAX)));
    }

    public static void addAffinity(ServerPlayer player, Identifier npcId, int amount, @Nullable NpcEntity npc) {
        if (amount == 0) {
            return;
        }
        var before = affinity(player, npcId);
        var after = Math.clamp(before + amount, CompanionProfile.MIN, CompanionProfile.MAX);
        if (after == before) {
            return;
        }
        set(player, state(player).withAffinity(npcId, after));
        var profile = companion(player.level().registryAccess(), npcId);
        var name = profile.map(value -> (Component) Component.translatableWithFallback(value.name(), value.name()))
            .orElseGet(() -> npc != null ? npc.getDisplayName() : Component.literal(npcId.getPath()));
        if (AtomicDawn.CONFIG.story.COMPANION_REACTIONS) {
            player.sendSystemMessage(
                Component.translatable(
                    after > before ? "companion.atomicdawn.liked" : "companion.atomicdawn.disliked",
                    name
                ).withStyle(after > before ? ChatFormatting.AQUA : ChatFormatting.GOLD),
                true
            );
        }
        if (npc != null && profile.isPresent() && after <= profile.get().leaveBelow() && npc.isOwnedBy(player)) {
            NpcCompanions.dismiss(player, npc);
            player.sendSystemMessage(
                Component.translatable("companion.atomicdawn.left", name).withStyle(ChatFormatting.RED)
            );
        }
    }

    public static List<NpcEntity> companions(ServerPlayer player) {
        var range = AtomicDawn.CONFIG.story.COMPANION_REACTION_RANGE;
        return player.level()
            .getEntitiesOfClass(
                NpcEntity.class,
                player.getBoundingBox().inflate(range),
                npc -> npc.isAlive() && npc.isOwnedBy(player) && npc.config().identifier().isPresent()
            );
    }

    public static void onAction(ServerPlayer player, Identifier action) {
        var access = player.level().registryAccess();
        for (var npc : companions(player)) {
            var id = npc.config().identifier().orElseThrow();
            companion(access, id).ifPresent(profile -> {
                for (var reaction : profile.reactions()) {
                    if (reaction.action().equals(action)) {
                        addAffinity(player, id, reaction.amount(), npc);
                    }
                }
            });
        }
    }

    private static void reactToReputation(ServerPlayer player, Identifier faction, int delta) {
        var access = player.level().registryAccess();
        for (var npc : companions(player)) {
            var id = npc.config().identifier().orElseThrow();
            companion(access, id).ifPresent(profile -> {
                for (var reaction : profile.factions()) {
                    if (reaction.faction().equals(faction)) {
                        addAffinity(player, id, Math.round(delta * reaction.perPoint()), npc);
                    }
                }
            });
        }
    }

    public static Optional<String> flag(Player player, String key) {
        return Optional.ofNullable(state(player).flags().get(key));
    }

    public static void setFlag(ServerPlayer player, String key, @Nullable String value) {
        set(player, state(player).withFlag(key, value));
    }

    public static Optional<String> outcome(Player player, Identifier quest) {
        return Optional.ofNullable(state(player).outcomes().get(quest));
    }

    public static void setOutcome(ServerPlayer player, Identifier quest, @Nullable String outcome) {
        set(player, state(player).withOutcome(quest, outcome));
    }

    public static boolean attempted(Player player, String key) {
        return state(player).attempted().contains(key);
    }

    public static void markAttempted(ServerPlayer player, String key) {
        set(player, state(player).withAttempt(key));
    }
}

package mod.azure.atomicdawn.party;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.network.ADNetwork;
import mod.azure.atomicdawn.network.PartySyncPayload;

public final class PartyService {

    private record Invite(
        UUID party,
        String inviter,
        long expires
    ) {}

    private static final Map<UUID, Party> BY_PLAYER = new HashMap<>();

    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    public static volatile boolean clientHasOnlineMates;

    private PartyService() {}

    public static boolean enabled() {
        return AtomicDawn.CONFIG.party.PARTY_ENABLED;
    }

    public static int maxSize() {
        return AtomicDawn.CONFIG.party.PARTY_MAX_SIZE;
    }

    public static Optional<Party> party(UUID player) {
        return Optional.ofNullable(BY_PLAYER.get(player));
    }

    public static Optional<Party> party(Player player) {
        return party(player.getUUID());
    }

    public static boolean sameParty(Player a, Player b) {
        if (a == b || !enabled()) {
            return false;
        }
        var party = BY_PLAYER.get(a.getUUID());
        return party != null && party.contains(b.getUUID());
    }

    public static List<ServerPlayer> onlineMates(ServerPlayer player) {
        var party = BY_PLAYER.get(player.getUUID());
        var result = new ArrayList<ServerPlayer>();
        if (party == null) {
            return result;
        } else {
            player.level();
        }
        for (var id : party.members()) {
            var other = player.level().getServer().getPlayerList().getPlayer(id);
            if (other != null && other != player) {
                result.add(other);
            }
        }
        return result;
    }

    public static void invite(ServerPlayer inviter, ServerPlayer target) {
        if (!checkEnabled(inviter)) {
            return;
        }
        if (target == inviter) {
            fail(inviter, "party.atomicdawn.invite_self");
            return;
        }
        if (BY_PLAYER.containsKey(target.getUUID())) {
            fail(inviter, "party.atomicdawn.already_in_party", target.getDisplayName());
            return;
        }
        var party = BY_PLAYER.get(inviter.getUUID());
        if (party == null) {
            party = new Party(inviter.getUUID(), inviter.getScoreboardName());
            BY_PLAYER.put(inviter.getUUID(), party);
        }
        if (party.size() >= maxSize()) {
            fail(inviter, "party.atomicdawn.full", maxSize());
            return;
        }
        var expires = inviter.level().getGameTime() + AtomicDawn.CONFIG.party.PARTY_INVITE_SECONDS * 20L;
        INVITES.put(target.getUUID(), new Invite(party.id(), inviter.getScoreboardName(), expires));
        inviter.sendSystemMessage(Component.translatable("party.atomicdawn.invite_sent", target.getDisplayName()));
        var accept = Component.translatable("party.atomicdawn.accept_button")
            .withStyle(
                style -> style.withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent.RunCommand("/party accept"))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("/party accept")))
            );
        var decline = Component.translatable("party.atomicdawn.decline_button")
            .withStyle(
                style -> style.withColor(ChatFormatting.RED)
                    .withClickEvent(new ClickEvent.RunCommand("/party decline"))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("/party decline")))
            );
        target.sendSystemMessage(
            Component.translatable("party.atomicdawn.invited", inviter.getDisplayName())
                .append(" ")
                .append(accept)
                .append(" ")
                .append(decline)
        );
        sync(party, inviter.level().getServer());
    }

    public static void accept(ServerPlayer player) {
        if (!checkEnabled(player)) {
            return;
        }
        var invite = INVITES.remove(player.getUUID());
        if (invite == null || invite.expires() < player.level().getGameTime()) {
            fail(player, "party.atomicdawn.no_invite");
            return;
        }
        if (BY_PLAYER.containsKey(player.getUUID())) {
            fail(player, "party.atomicdawn.leave_first");
            return;
        }
        var party = find(invite.party());
        if (party == null) {
            fail(player, "party.atomicdawn.no_invite");
            return;
        }
        if (party.size() >= maxSize()) {
            fail(player, "party.atomicdawn.full", maxSize());
            return;
        }
        party.add(player.getUUID(), player.getScoreboardName());
        BY_PLAYER.put(player.getUUID(), party);
        var joined = Component.translatable("party.atomicdawn.joined", player.getDisplayName());
        broadcast(party, player.level().getServer(), joined);
        sync(party, player.level().getServer());
    }

    public static void decline(ServerPlayer player) {
        var invite = INVITES.remove(player.getUUID());
        if (invite == null) {
            fail(player, "party.atomicdawn.no_invite");
            return;
        }
        player.sendSystemMessage(Component.translatable("party.atomicdawn.declined", invite.inviter()));
    }

    public static void leave(ServerPlayer player) {
        var party = BY_PLAYER.get(player.getUUID());
        if (party == null) {
            fail(player, "party.atomicdawn.not_in_party");
            return;
        }
        remove(party, player.getUUID(), player.level().getServer());
        player.sendSystemMessage(Component.translatable("party.atomicdawn.left"));
        var left = Component.translatable("party.atomicdawn.member_left", player.getDisplayName());
        broadcast(party, player.level().getServer(), left);
    }

    public static void kick(ServerPlayer leader, String name) {
        var party = BY_PLAYER.get(leader.getUUID());
        if (party == null || !party.isLeader(leader.getUUID())) {
            fail(leader, "party.atomicdawn.not_leader");
            return;
        }
        var target = byName(party, name);
        if (target == null || target.equals(leader.getUUID())) {
            fail(leader, "party.atomicdawn.no_member", name);
            return;
        }
        var server = leader.level().getServer();
        remove(party, target, server);
        var kicked = server.getPlayerList().getPlayer(target);
        if (kicked != null) {
            kicked.sendSystemMessage(Component.translatable("party.atomicdawn.kicked"));
        }
        broadcast(party, server, Component.translatable("party.atomicdawn.member_kicked", name));
    }

    public static void promote(ServerPlayer leader, String name) {
        var party = BY_PLAYER.get(leader.getUUID());
        if (party == null || !party.isLeader(leader.getUUID())) {
            fail(leader, "party.atomicdawn.not_leader");
            return;
        }
        var target = byName(party, name);
        if (target == null) {
            fail(leader, "party.atomicdawn.no_member", name);
            return;
        }
        party.promote(target);
        broadcast(party, leader.level().getServer(), Component.translatable("party.atomicdawn.promoted", name));
        sync(party, leader.level().getServer());
    }

    public static void list(ServerPlayer player) {
        var party = BY_PLAYER.get(player.getUUID());
        if (party == null) {
            fail(player, "party.atomicdawn.not_in_party");
            return;
        }
        var text = Component.translatable("party.atomicdawn.list", party.size(), maxSize());
        for (var id : party.members()) {
            text.append("\n").append(party.isLeader(id) ? "★ " : "- ").append(party.name(id));
        }
        player.sendSystemMessage(text);
    }

    public static List<String> memberNames(UUID player) {
        var party = BY_PLAYER.get(player);
        return party == null ? List.of() : party.members().stream().map(party::name).toList();
    }

    public static void syncAll(MinecraftServer server) {
        INVITES.values().removeIf(invite -> invite.expires() < server.overworld().getGameTime());
        BY_PLAYER.values().stream().distinct().toList().forEach(party -> sync(party, server));
    }

    static void sync(Party party, @Nullable MinecraftServer server) {
        if (server == null) {
            return;
        }
        var members = new ArrayList<PartySyncPayload.Member>();
        for (var id : party.members()) {
            var online = server.getPlayerList().getPlayer(id);
            if (online != null) {
                party.names.put(id, online.getScoreboardName());
                members.add(
                    new PartySyncPayload.Member(
                        id,
                        party.name(id),
                        party.isLeader(id),
                        true,
                        online.getHealth(),
                        online.getMaxHealth(),
                        online.level().dimension().identifier(),
                        online.blockPosition()
                    )
                );
            } else {
                members.add(
                    new PartySyncPayload.Member(
                        id,
                        party.name(id),
                        party.isLeader(id),
                        false,
                        0F,
                        0F,
                        Level.OVERWORLD.identifier(),
                        BlockPos.ZERO
                    )
                );
            }
        }
        var payload = new PartySyncPayload(maxSize(), members);
        for (var id : party.members()) {
            var online = server.getPlayerList().getPlayer(id);
            if (online != null) {
                ADNetwork.sendTo(online, payload);
            }
        }
    }

    private static void remove(Party party, UUID player, @Nullable MinecraftServer server) {
        party.remove(player);
        BY_PLAYER.remove(player);
        if (server != null && server.getPlayerList().getPlayer(player) instanceof ServerPlayer removed) {
            ADNetwork.sendTo(removed, PartySyncPayload.NONE);
        }
        if (party.size() == 0) {
            INVITES.values().removeIf(invite -> invite.party().equals(party.id()));
            return;
        }
        sync(party, server);
    }

    @Nullable
    private static Party find(UUID id) {
        for (var party : BY_PLAYER.values()) {
            if (party.id().equals(id)) {
                return party;
            }
        }
        return null;
    }

    @Nullable
    private static UUID byName(Party party, String name) {
        for (var id : party.members()) {
            if (party.name(id).equalsIgnoreCase(name)) {
                return id;
            }
        }
        return null;
    }

    private static void broadcast(Party party, @Nullable MinecraftServer server, Component message) {
        if (server == null) {
            return;
        }
        for (var id : party.members()) {
            var online = server.getPlayerList().getPlayer(id);
            if (online != null) {
                online.sendSystemMessage(message);
            }
        }
    }

    private static boolean checkEnabled(ServerPlayer player) {
        if (!enabled()) {
            fail(player, "party.atomicdawn.disabled");
            return false;
        }
        return true;
    }

    private static void fail(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }

    public static void clear() {
        BY_PLAYER.clear();
        INVITES.clear();
    }
}

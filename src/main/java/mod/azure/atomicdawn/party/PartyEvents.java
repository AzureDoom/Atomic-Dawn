package mod.azure.atomicdawn.party;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class PartyEvents {

    private PartyEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher()
            .register(
                Commands.literal("party")
                    .then(
                        Commands.literal("invite")
                            .then(
                                Commands.argument("player", EntityArgument.player())
                                    .executes(context -> {
                                        PartyService.invite(
                                            context.getSource().getPlayerOrException(),
                                            EntityArgument.getPlayer(context, "player")
                                        );
                                        return 1;
                                    })
                            )
                    )
                    .then(Commands.literal("accept").executes(context -> {
                        PartyService.accept(context.getSource().getPlayerOrException());
                        return 1;
                    }))
                    .then(Commands.literal("decline").executes(context -> {
                        PartyService.decline(context.getSource().getPlayerOrException());
                        return 1;
                    }))
                    .then(Commands.literal("leave").executes(context -> {
                        PartyService.leave(context.getSource().getPlayerOrException());
                        return 1;
                    }))
                    .then(Commands.literal("list").executes(context -> {
                        PartyService.list(context.getSource().getPlayerOrException());
                        return 1;
                    }))
                    .then(
                        Commands.literal("kick")
                            .then(
                                Commands.argument("member", StringArgumentType.word())
                                    .suggests(PartyEvents::suggestMembers)
                                    .executes(context -> {
                                        PartyService.kick(
                                            context.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(context, "member")
                                        );
                                        return 1;
                                    })
                            )
                    )
                    .then(
                        Commands.literal("promote")
                            .then(
                                Commands.argument("member", StringArgumentType.word())
                                    .suggests(PartyEvents::suggestMembers)
                                    .executes(context -> {
                                        PartyService.promote(
                                            context.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(context, "member")
                                        );
                                        return 1;
                                    })
                            )
                    )
            );
    }

    private static CompletableFuture<Suggestions> suggestMembers(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        var player = context.getSource().getPlayer();
        return SharedSuggestionProvider.suggest(
            player == null ? List.of() : PartyService.memberNames(player.getUUID()),
            builder
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 == 0) {
            PartyService.syncAll(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PartyService.party(player).ifPresent(party -> PartyService.sync(party, player.level().getServer()));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PartyService.clear();
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (
            !AtomicDawn.CONFIG.common.social.party.PARTY_FRIENDLY_FIRE && event
                .getEntity() instanceof ServerPlayer victim
                && event.getSource().getEntity() instanceof ServerPlayer attacker && PartyService.sameParty(
                    victim,
                    attacker
                )
        ) {
            event.setCanceled(true);
        }
    }
}

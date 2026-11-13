package mod.azure.atomicdawn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import mod.azure.atomicdawn.special.SpecialService;

public final class AtomicDawnCommands {

    private AtomicDawnCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("atomicdawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("special")
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(
                                    Commands.literal("xp")
                                        .then(
                                            Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    var player = player(context);
                                                    SpecialService.addXp(
                                                        player,
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                    );
                                                    return feedback(context, "xp", player);
                                                })
                                        )
                                )
                                .then(
                                    Commands.literal("perkpicks")
                                        .then(
                                            Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                .executes(context -> {
                                                    var player = player(context);
                                                    SpecialService.grantPerkPicks(
                                                        player,
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                    );
                                                    return feedback(context, "perkpicks", player);
                                                })
                                        )
                                )
                                .then(
                                    Commands.literal("grant")
                                        .then(
                                            Commands.argument("perk", StringArgumentType.string())
                                                .then(
                                                    Commands.argument("rank", IntegerArgumentType.integer(1, 10))
                                                        .executes(context -> {
                                                            var player = player(context);
                                                            var id = Identifier.tryParse(
                                                                StringArgumentType.getString(context, "perk")
                                                            );
                                                            if (
                                                                id == null || !SpecialService.grantPerk(
                                                                    player,
                                                                    id,
                                                                    IntegerArgumentType.getInteger(context, "rank")
                                                                )
                                                            ) {
                                                                context.getSource()
                                                                    .sendFailure(
                                                                        Component.translatable(
                                                                            "commands.atomicdawn.special.unknown_perk"
                                                                        )
                                                                    );
                                                                return 0;
                                                            }
                                                            return feedback(context, "grant", player);
                                                        })
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("coins")
                                        .then(
                                            Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                                .executes(context -> {
                                                    var player = player(context);
                                                    SpecialService.grantLegendaryCoins(
                                                        player,
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                    );
                                                    return feedback(context, "coins", player);
                                                })
                                        )
                                )
                                .then(
                                    Commands.literal("legendary")
                                        .then(
                                            Commands.argument("perk", StringArgumentType.string())
                                                .then(
                                                    Commands.argument("rank", IntegerArgumentType.integer(1, 8))
                                                        .executes(context -> {
                                                            var player = player(context);
                                                            var id = Identifier.tryParse(
                                                                StringArgumentType.getString(context, "perk")
                                                            );
                                                            if (
                                                                id == null || !SpecialService.grantLegendary(
                                                                    player,
                                                                    id,
                                                                    IntegerArgumentType.getInteger(context, "rank")
                                                                )
                                                            ) {
                                                                context.getSource()
                                                                    .sendFailure(
                                                                        Component.translatable(
                                                                            "commands.atomicdawn.special.unknown_perk"
                                                                        )
                                                                    );
                                                                return 0;
                                                            }
                                                            return feedback(context, "legendary", player);
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("respec").executes(context -> {
                                    var player = player(context);
                                    SpecialService.respec(player);
                                    return feedback(context, "respec", player);
                                }))
                                .then(Commands.literal("reset").executes(context -> {
                                    var player = player(context);
                                    SpecialService.reset(player);
                                    return feedback(context, "reset", player);
                                }))
                        )
                )
        );
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return EntityArgument.getPlayer(context, "player");
    }

    private static int feedback(CommandContext<CommandSourceStack> context, String key, ServerPlayer player) {
        context.getSource()
            .sendSuccess(
                () -> Component.translatable("commands.atomicdawn.special." + key, player.getDisplayName()),
                true
            );
        return 1;
    }
}

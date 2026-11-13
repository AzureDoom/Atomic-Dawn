package mod.azure.atomicdawn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiPredicate;

import mod.azure.atomicdawn.pipboy.PipBoyNames;
import mod.azure.atomicdawn.quest.QuestService;

public final class QuestCommands {

    private QuestCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("atomicdawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("quest")
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(action("start", QuestService::start))
                                .then(action("complete", QuestService::forceComplete))
                                .then(action("reset", QuestService::reset))
                        )
                )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> action(
        String name,
        BiPredicate<ServerPlayer, Identifier> handler
    ) {
        return Commands.literal(name)
            .then(
                Commands.argument("quest", StringArgumentType.greedyString())
                    .executes(context -> run(context, name, handler))
            );
    }

    private static int run(
        CommandContext<CommandSourceStack> context,
        String name,
        BiPredicate<ServerPlayer, Identifier> handler
    ) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var id = Identifier.tryParse(StringArgumentType.getString(context, "quest"));
        if (id == null || !handler.test(player, id)) {
            context.getSource().sendFailure(Component.translatable("commands.atomicdawn.quest.failed"));
            return 0;
        }
        context.getSource()
            .sendSuccess(
                () -> Component.translatable(
                    "commands.atomicdawn.quest." + name,
                    PipBoyNames.quest(id),
                    player.getDisplayName()
                ),
                true
            );
        return 1;
    }
}

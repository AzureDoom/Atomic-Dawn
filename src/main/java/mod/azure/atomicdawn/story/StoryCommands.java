package mod.azure.atomicdawn.story;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;

import mod.azure.atomicdawn.registry.ADAttachments;

public final class StoryCommands {

    private StoryCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("atomicdawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("story")
                        .then(
                            Commands.literal("reputation")
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .then(
                                            Commands.argument("faction", IdentifierArgument.id())
                                                .suggests(
                                                    (context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        StoryService.factions(
                                                            context.getSource().registryAccess()
                                                        ).stream().map(holder -> holder.key().identifier()),
                                                        builder
                                                    )
                                                )
                                                .executes(StoryCommands::getReputation)
                                                .then(
                                                    Commands.literal("add")
                                                        .then(
                                                            Commands.argument("amount", IntegerArgumentType.integer())
                                                                .executes(context -> changeReputation(context, true))
                                                        )
                                                )
                                                .then(
                                                    Commands.literal("set")
                                                        .then(
                                                            Commands.argument("amount", IntegerArgumentType.integer())
                                                                .executes(context -> changeReputation(context, false))
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("affinity")
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .then(
                                            Commands.argument("npc", IdentifierArgument.id())
                                                .then(
                                                    Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(StoryCommands::setAffinity)
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("flag")
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .then(
                                            Commands.argument("key", StringArgumentType.word())
                                                .executes(context -> setFlag(context, false))
                                                .then(
                                                    Commands.argument("value", StringArgumentType.greedyString())
                                                        .executes(context -> setFlag(context, true))
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("outcome")
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .then(
                                            Commands.argument("quest", IdentifierArgument.id())
                                                .then(
                                                    Commands.argument("outcome", StringArgumentType.word())
                                                        .executes(StoryCommands::setOutcome)
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("reset")
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .executes(StoryCommands::reset)
                                )
                        )
                )
        );
    }

    private static int getReputation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var faction = IdentifierArgument.getId(context, "faction");
        var value = StoryService.reputation(player, faction);
        var rank = StoryService.rank(player, faction).map(Faction.Rank::name).orElse("?");
        context.getSource()
            .sendSuccess(
                () -> Component.translatable(
                    "commands.atomicdawn.story.reputation",
                    player.getDisplayName(),
                    faction.toString(),
                    value,
                    Component.translatableWithFallback(rank, rank)
                ),
                false
            );
        return value;
    }

    private static int changeReputation(
        CommandContext<CommandSourceStack> context,
        boolean add
    ) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var faction = IdentifierArgument.getId(context, "faction");
        var amount = IntegerArgumentType.getInteger(context, "amount");
        if (StoryService.faction(player.level().registryAccess(), faction).isEmpty()) {
            context.getSource()
                .sendFailure(Component.translatable("commands.atomicdawn.story.unknown_faction", faction.toString()));
            return 0;
        }
        if (add) {
            StoryService.addReputation(player, faction, amount);
        } else {
            StoryService.setReputation(player, faction, amount);
        }
        return getReputation(context);
    }

    private static int setAffinity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var npc = IdentifierArgument.getId(context, "npc");
        var amount = IntegerArgumentType.getInteger(context, "amount");
        StoryService.setAffinity(player, npc, amount);
        context.getSource()
            .sendSuccess(
                () -> Component.translatable(
                    "commands.atomicdawn.story.affinity",
                    player.getDisplayName(),
                    npc.toString(),
                    StoryService.affinity(player, npc)
                ),
                true
            );
        return 1;
    }

    private static int setFlag(
        CommandContext<CommandSourceStack> context,
        boolean hasValue
    ) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var key = StringArgumentType.getString(context, "key");
        var value = hasValue ? StringArgumentType.getString(context, "value") : null;
        StoryService.setFlag(player, key, value);
        context.getSource()
            .sendSuccess(
                () -> Component.translatable(
                    "commands.atomicdawn.story.flag",
                    player.getDisplayName(),
                    key,
                    value == null ? "-" : value
                ),
                true
            );
        return 1;
    }

    private static int setOutcome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        var quest = IdentifierArgument.getId(context, "quest");
        var outcome = StringArgumentType.getString(context, "outcome");
        StoryService.setOutcome(player, quest, outcome);
        context.getSource()
            .sendSuccess(
                () -> Component.translatable(
                    "commands.atomicdawn.story.outcome",
                    player.getDisplayName(),
                    quest.toString(),
                    outcome
                ),
                true
            );
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        player.setData(ADAttachments.STORY.get(), StoryState.EMPTY);
        context.getSource()
            .sendSuccess(
                () -> Component.translatable("commands.atomicdawn.story.reset", player.getDisplayName()),
                true
            );
        return 1;
    }
}

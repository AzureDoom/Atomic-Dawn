package mod.azure.atomicdawn.ghoul;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class GhoulCommands {

    private GhoulCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("atomicdawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("ghoul")
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("status").executes(GhoulCommands::status))
                                .then(Commands.literal("transform").executes(context -> {
                                    var player = player(context);
                                    return result(
                                        context,
                                        player,
                                        GhoulService.transform(player, TransformationContext.operator())
                                    );
                                }))
                                .then(Commands.literal("revert").executes(context -> {
                                    var player = player(context);
                                    return result(
                                        context,
                                        player,
                                        GhoulService.revert(player, TransformationContext.operator())
                                    );
                                }))
                                .then(
                                    Commands.literal("glow")
                                        .then(
                                            Commands.argument("amount", FloatArgumentType.floatArg(0F, 10_000F))
                                                .executes(context -> {
                                                    var player = player(context);
                                                    if (!GhoulService.state(player).isGhoul()) {
                                                        return notGhoul(context);
                                                    }
                                                    GhoulService.setGlow(
                                                        player,
                                                        FloatArgumentType.getFloat(context, "amount")
                                                    );
                                                    return feedback(context, "glow", player);
                                                })
                                        )
                                )
                                .then(
                                    Commands.literal("stability")
                                        .then(
                                            Commands.argument("amount", FloatArgumentType.floatArg(0F, 10_000F))
                                                .executes(context -> {
                                                    var player = player(context);
                                                    if (!GhoulService.state(player).isGhoul()) {
                                                        return notGhoul(context);
                                                    }
                                                    GhoulService.setStability(
                                                        player,
                                                        FloatArgumentType.getFloat(context, "amount")
                                                    );
                                                    return feedback(context, "stability", player);
                                                })
                                        )
                                )
                                .then(Commands.literal("resetcooldown").executes(context -> {
                                    var player = player(context);
                                    GhoulService.resetCooldown(player);
                                    return feedback(context, "resetcooldown", player);
                                }))
                                .then(Commands.literal("resync").executes(context -> {
                                    var player = player(context);
                                    GhoulService.resync(player);
                                    return feedback(context, "resync", player);
                                }))
                        )
                )
        );
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = player(context);
        var state = GhoulService.state(player);
        var remaining = GhoulService.cooldownRemainingMillis(player, System.currentTimeMillis());
        var text = Component.translatable(
            "commands.atomicdawn.ghoul.status",
            player.getDisplayName(),
            Component.translatable(state.form().translationKey()),
            String.format(Locale.ROOT, "%.1f/%.0f", state.glow(), GhoulService.maxGlow(player)),
            String.format(Locale.ROOT, "%.1f/%.0f", state.stability(), GhoulService.maxStability()),
            Component.translatable(GhoulService.tier(player).translationKey()),
            remaining > 0L
                ? Component.literal(GhoulTreatmentService.formatDuration(remaining))
                : Component.translatable("ghoul.atomicdawn.cooldown.ready")
        );
        context.getSource().sendSuccess(() -> text, false);
        return 1;
    }

    private static int result(
        CommandContext<CommandSourceStack> context,
        ServerPlayer player,
        TransformationResult result
    ) {
        if (!result.success()) {
            context.getSource().sendFailure(Component.translatable(result.translationKey(), "-"));
            return 0;
        }
        return feedback(context, "form", player);
    }

    private static int notGhoul(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("ghoul.atomicdawn.result.already_human"));
        return 0;
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return EntityArgument.getPlayer(context, "player");
    }

    private static int feedback(CommandContext<CommandSourceStack> context, String key, ServerPlayer player) {
        context.getSource()
            .sendSuccess(
                () -> Component.translatable("commands.atomicdawn.ghoul." + key, player.getDisplayName()),
                true
            );
        return 1;
    }
}

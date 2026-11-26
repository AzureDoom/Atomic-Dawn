package mod.azure.atomicdawn.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

import mod.azure.atomicdawn.lock.Lock;
import mod.azure.atomicdawn.lock.LockLevel;
import mod.azure.atomicdawn.lock.LockService;

public final class LockCommands {

    private LockCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var names = Arrays.stream(LockLevel.values()).map(LockLevel::getSerializedName).toList();
        dispatcher.register(
            Commands.literal("atomicdawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("lock")
                        .then(
                            Commands.literal("set")
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(
                                            Commands.argument("level", StringArgumentType.word())
                                                .suggests(
                                                    (ignored, builder) -> SharedSuggestionProvider.suggest(
                                                        names,
                                                        builder
                                                    )
                                                )
                                                .executes(LockCommands::set)
                                        )
                                )
                        )
                        .then(posAction("remove", LockCommands::remove))
                        .then(posAction("reset", LockCommands::reset))
                        .then(posAction("info", LockCommands::info))
                )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> posAction(
        String name,
        Command<CommandSourceStack> command
    ) {
        return Commands.literal(name)
            .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(command));
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var level = source.getLevel();
        var pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        var lockLevel = LockLevel.byName(StringArgumentType.getString(context, "level"));
        if (lockLevel == null) {
            source.sendFailure(Component.translatable("commands.atomicdawn.lock.unknown_level"));
            return 0;
        }
        var state = level.getBlockState(pos);
        if (!LockService.lockable(level, pos, state)) {
            source.sendFailure(Component.translatable("lock.atomicdawn.not_lockable"));
            return 0;
        }
        var existing = LockService.find(level, pos);
        var target = existing.map(LockService.Located::pos).orElse(LockService.anchor(state, pos));
        LockService.put(level, target, Lock.world(lockLevel));
        source.sendSuccess(
            () -> Component.translatable(
                "commands.atomicdawn.lock.set",
                lockLevel.component(),
                target.getX(),
                target.getY(),
                target.getZ()
            ),
            true
        );
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var level = source.getLevel();
        var located = LockService.find(level, BlockPosArgument.getLoadedBlockPos(context, "pos"));
        if (located.isEmpty()) {
            source.sendFailure(Component.translatable("commands.atomicdawn.lock.none"));
            return 0;
        }
        LockService.put(level, located.get().pos(), null);
        source.sendSuccess(() -> Component.translatable("commands.atomicdawn.lock.removed"), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var level = source.getLevel();
        var located = LockService.find(level, BlockPosArgument.getLoadedBlockPos(context, "pos"));
        if (located.isEmpty()) {
            source.sendFailure(Component.translatable("commands.atomicdawn.lock.none"));
            return 0;
        }
        LockService.put(level, located.get().pos(), located.get().lock().withoutAccess());
        source.sendSuccess(() -> Component.translatable("commands.atomicdawn.lock.reset"), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var level = source.getLevel();
        var located = LockService.find(level, BlockPosArgument.getLoadedBlockPos(context, "pos"));
        if (located.isEmpty()) {
            source.sendFailure(Component.translatable("commands.atomicdawn.lock.none"));
            return 0;
        }
        var lock = located.get().lock();
        var owner = lock.owner().map(Object::toString).orElse("-");
        source.sendSuccess(
            () -> Component.translatable(
                "commands.atomicdawn.lock.info",
                lock.level().component(),
                owner,
                lock.access().size()
            ),
            false
        );
        return 1;
    }
}

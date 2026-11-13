package mod.azure.atomicdawn.npc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mod.azure.atomicdawn.registry.ADItems;

public final class NpcCommands {

    private NpcCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("atomicdawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("npc")
                        .then(Commands.literal("wand").executes(NpcCommands::wand))
                        .then(
                            Commands.literal("spawn")
                                .executes(context -> spawn(context, ""))
                                .then(
                                    Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(
                                            context -> spawn(context, StringArgumentType.getString(context, "name"))
                                        )
                                )
                        )
                        .then(
                            Commands.literal("talk")
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .then(
                                            Commands.argument("npc", EntityArgument.entity())
                                                .executes(NpcCommands::talk)
                                        )
                                )
                        )
                )
        );
    }

    private static int wand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        player.getInventory().add(new ItemStack(ADItems.NPC_WAND.get()));
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var config = NpcConfig.DEFAULT.withGeneral("", name, "", true);
        var npc = NpcService.spawn(
            context.getSource().getLevel(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            config
        );
        if (npc == null) {
            context.getSource().sendFailure(Component.translatable("commands.atomicdawn.npc.spawn_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.atomicdawn.npc.spawned"), true);
        return 1;
    }

    private static int talk(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        if (!(EntityArgument.getEntity(context, "npc") instanceof NpcEntity npc)) {
            context.getSource().sendFailure(Component.translatable("commands.atomicdawn.npc.not_npc"));
            return 0;
        }
        NpcDialogueService.open(player, npc);
        return 1;
    }
}

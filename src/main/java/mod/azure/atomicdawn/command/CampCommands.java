package mod.azure.atomicdawn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import mod.azure.atomicdawn.camp.CampAttachments;
import mod.azure.atomicdawn.camp.CampBuildable;
import mod.azure.atomicdawn.camp.CampManager;
import mod.azure.atomicdawn.camp.CampPlans;
import mod.azure.atomicdawn.camp.CampSlot;
import mod.azure.atomicdawn.camp.CampSync;

public final class CampCommands {

    private CampCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("atomicdawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("camp")
                        .then(Commands.literal("list").executes(CampCommands::list))
                        .then(
                            Commands.literal("plan")
                                .then(planAction("grant", true))
                                .then(planAction("revoke", false))
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("camp", UuidArgument.uuid())
                                        .suggests(
                                            (context, builder) -> SharedSuggestionProvider.suggest(
                                                CampManager.registry(context.getSource().getServer())
                                                    .list()
                                                    .stream()
                                                    .map(slot -> slot.campId().toString()),
                                                builder
                                            )
                                        )
                                        .executes(CampCommands::remove)
                                )
                        )
                )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> planAction(String name, boolean grant) {
        return Commands.literal(name)
            .then(
                Commands.argument("targets", EntityArgument.players())
                    .then(
                        Commands.argument("plan", StringArgumentType.greedyString())
                            .suggests(
                                (context, builder) -> SharedSuggestionProvider.suggest(
                                    CampBuildable.all(context.getSource().registryAccess())
                                        .stream()
                                        .flatMap(holder -> holder.value().plan().stream())
                                        .map(Identifier::toString)
                                        .distinct(),
                                    builder
                                )
                            )
                            .executes(context -> plan(context, grant))
                    )
            );
    }

    private static int plan(CommandContext<CommandSourceStack> context, boolean grant) throws CommandSyntaxException {
        var source = context.getSource();
        var id = Identifier.tryParse(StringArgumentType.getString(context, "plan").trim());
        if (id == null) {
            source.sendFailure(Component.translatable("commands.atomicdawn.camp.plan.invalid"));
            return 0;
        }
        var targets = EntityArgument.getPlayers(context, "targets");
        for (var player : targets) {
            var plans = CampPlans.of(player);
            player.setData(CampAttachments.CAMP_PLANS.get(), grant ? plans.with(id) : plans.without(id));
        }
        var key = grant ? "commands.atomicdawn.camp.plan.granted" : "commands.atomicdawn.camp.plan.revoked";
        source.sendSuccess(() -> Component.translatable(key, id.toString(), targets.size()), true);
        return targets.size();
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var camps = CampManager.registry(source.getServer()).list();
        if (camps.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.atomicdawn.camp.none"), false);
            return 0;
        }
        for (var camp : camps) {
            source.sendSuccess(() -> describe(camp), false);
        }
        return camps.size();
    }

    private static Component describe(CampSlot camp) {
        return Component.translatable(
            "commands.atomicdawn.camp.entry",
            camp.ownerName().isEmpty() ? camp.owner().toString() : camp.ownerName(),
            camp.dimension().identifier().toString(),
            camp.origin().getX(),
            camp.origin().getY(),
            camp.origin().getZ(),
            camp.campId().toString()
        );
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var server = source.getServer();
        var id = UuidArgument.getUuid(context, "camp");
        var camp = CampManager.get(server, id);
        if (camp.isEmpty()) {
            source.sendFailure(Component.translatable("commands.atomicdawn.camp.unknown"));
            return 0;
        }
        CampManager.remove(server, id);
        CampSync.changed(server);
        source.sendSuccess(
            () -> Component.translatable("commands.atomicdawn.camp.removed", describe(camp.get())),
            true
        );
        return 1;
    }
}

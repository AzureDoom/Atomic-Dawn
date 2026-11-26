package mod.azure.atomicdawn.limb;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Locale;

import mod.azure.atomicdawn.vats.BodyPart;

public final class LimbCommands {

    private LimbCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var parts = Arrays.stream(BodyPart.values()).map(part -> part.name().toLowerCase(Locale.ROOT)).toList();
        dispatcher.register(
            Commands.literal("atomicdawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("limbs")
                        .then(
                            Commands.literal("heal")
                                .then(
                                    Commands.argument("targets", EntityArgument.entities())
                                        .executes(LimbCommands::heal)
                                )
                        )
                        .then(
                            Commands.literal("cripple")
                                .then(
                                    Commands.argument("targets", EntityArgument.entities())
                                        .then(
                                            Commands.argument("part", StringArgumentType.word())
                                                .suggests(
                                                    (ignored, builder) -> SharedSuggestionProvider.suggest(
                                                        parts,
                                                        builder
                                                    )
                                                )
                                                .executes(LimbCommands::cripple)
                                        )
                                )
                        )
                        .then(
                            Commands.literal("info")
                                .then(
                                    Commands.argument("target", EntityArgument.entity())
                                        .executes(LimbCommands::info)
                                )
                        )
                )
        );
    }

    private static int heal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var count = 0;
        for (var entity : EntityArgument.getEntities(context, "targets")) {
            if (entity instanceof LivingEntity living) {
                LimbService.healAll(living);
                count++;
            }
        }
        var healed = count;
        context.getSource().sendSuccess(() -> Component.translatable("commands.atomicdawn.limbs.heal", healed), true);
        return count;
    }

    private static int cripple(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var name = StringArgumentType.getString(context, "part");
        var part = Arrays.stream(BodyPart.values())
            .filter(value -> value.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
        if (part == null) {
            context.getSource().sendFailure(Component.translatable("commands.atomicdawn.limbs.unknown_part", name));
            return 0;
        }
        var count = 0;
        for (var entity : EntityArgument.getEntities(context, "targets")) {
            if (entity instanceof LivingEntity living && LimbService.enabled(living)) {
                LimbService.cripple(living, part, context.getSource().getEntity());
                count++;
            }
        }
        var crippled = count;
        context.getSource()
            .sendSuccess(
                () -> Component.translatable(
                    "commands.atomicdawn.limbs.cripple",
                    Component.translatable(part.translationKey()),
                    crippled
                ),
                true
            );
        return count;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!(EntityArgument.getEntity(context, "target") instanceof LivingEntity living)) {
            return 0;
        }
        var state = LimbService.state(living);
        var profile = LimbProfiles.of(living);
        context.getSource()
            .sendSuccess(
                () -> Component.translatable(
                    "commands.atomicdawn.limbs.info",
                    living.getDisplayName(),
                    Math.round(LimbService.limbHealth(living))
                ),
                false
            );
        for (var part : profile.parts()) {
            var percent = Math.round(state.get(part) * 100F);
            context.getSource()
                .sendSuccess(
                    () -> Component.translatable(
                        "commands.atomicdawn.limbs.info.part",
                        Component.translatable(part.translationKey()),
                        percent
                    ),
                    false
                );
        }
        return 1;
    }
}

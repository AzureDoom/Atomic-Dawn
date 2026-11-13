package mod.azure.atomicdawn.npc.dialogue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcCompanions;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.quest.QuestService;

public record NpcAction(
    Type type,
    Optional<Identifier> id,
    Optional<String> value,
    int amount
) {

    public enum Type implements StringRepresentable {

        START_QUEST,
        COMPLETE_QUEST,
        TRACK_QUEST,
        GIVE_ITEM,
        TAKE_ITEM,
        EMOTE,
        COMMAND,
        HIRE,
        RECRUIT,
        DISMISS,
        OPEN_SHOP,
        CLOSE;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<NpcAction> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Type.CODEC.fieldOf("type").forGetter(NpcAction::type),
            Identifier.CODEC.optionalFieldOf("id").forGetter(NpcAction::id),
            Codec.STRING.optionalFieldOf("value").forGetter(NpcAction::value),
            Codec.intRange(1, 6400).optionalFieldOf("amount", 1).forGetter(NpcAction::amount)
        ).apply(instance, NpcAction::new)
    );

    public enum Outcome {
        CONTINUE,
        CLOSE,
        OPEN_SHOP
    }

    public Outcome run(ServerPlayer player, NpcEntity npc) {
        switch (type) {
            case START_QUEST -> id.ifPresent(quest -> QuestService.start(player, quest));
            case COMPLETE_QUEST -> id.ifPresent(quest -> QuestService.forceComplete(player, quest));
            case TRACK_QUEST -> id.ifPresent(quest -> QuestService.track(player, Optional.of(quest)));
            case GIVE_ITEM -> id.flatMap(BuiltInRegistries.ITEM::getOptional).ifPresent(item -> {
                var stack = new ItemStack(item, amount);
                if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                    player.drop(stack, false, Prediction.SERVER_ONLY);
                }
            });
            case TAKE_ITEM -> id.flatMap(BuiltInRegistries.ITEM::getOptional).ifPresent(item -> {
                var remaining = amount;
                var inventory = player.getInventory();
                for (var slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                    var stack = inventory.getItem(slot);
                    if (stack.is(item)) {
                        var taken = Math.min(remaining, stack.getCount());
                        stack.shrink(taken);
                        remaining -= taken;
                    }
                }
            });
            case EMOTE -> value.ifPresent(npc::emote);
            case COMMAND -> value.ifPresent(command -> runCommand(player, npc, command));
            case HIRE -> {
                if (!NpcCompanions.hire(player, npc)) {
                    return Outcome.CLOSE;
                }
            }
            case RECRUIT -> NpcCompanions.recruit(player, npc);
            case DISMISS -> NpcCompanions.dismiss(player, npc);
            case OPEN_SHOP -> {
                return Outcome.OPEN_SHOP;
            }
            case CLOSE -> {
                return Outcome.CLOSE;
            }
        }
        return Outcome.CONTINUE;
    }

    private static void runCommand(ServerPlayer player, NpcEntity npc, String command) {
        ServerLevel level = player.level();
        var server = level.getServer();
        var resolved = command.replace("%player%", player.getScoreboardName())
            .replace("%npc%", npc.getStringUUID());
        try {
            var source = server.createCommandSourceStack()
                .withEntity(player)
                .withPosition(npc.position())
                .withLevel(level)
                .withSuppressedOutput();
            server.getCommands().performPrefixedCommand(source, resolved);
        } catch (RuntimeException exception) {
            AtomicDawn.LOGGER.warn("NPC dialogue command failed: {}", resolved, exception);
        }
    }
}

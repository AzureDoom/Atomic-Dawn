package mod.azure.atomicdawn.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.npc.NpcEntity;
import mod.azure.atomicdawn.npc.dialogue.NpcAction;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialService;
import mod.azure.atomicdawn.special.SpecialStatResolver;

public record SkillCheck(
    Type type,
    Optional<SpecialAttribute> stat,
    Optional<Identifier> id,
    int value,
    Mode mode,
    Optional<String> success,
    Optional<String> failure,
    List<NpcAction> successActions,
    List<NpcAction> failureActions,
    Optional<Boolean> once,
    boolean hideFailed,
    Optional<Integer> xp
) {

    public enum Type implements StringRepresentable {

        SPECIAL,
        LEVEL,
        PERK,
        REPUTATION,
        AFFINITY;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Mode implements StringRepresentable {

        REQUIREMENT,
        CHANCE;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<SkillCheck> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Type.CODEC.optionalFieldOf("type", Type.SPECIAL).forGetter(SkillCheck::type),
            SpecialAttribute.CODEC.optionalFieldOf("stat").forGetter(SkillCheck::stat),
            Identifier.CODEC.optionalFieldOf("id").forGetter(SkillCheck::id),
            Codec.INT.fieldOf("value").forGetter(SkillCheck::value),
            Mode.CODEC.optionalFieldOf("mode", Mode.REQUIREMENT).forGetter(SkillCheck::mode),
            Codec.STRING.optionalFieldOf("success").forGetter(SkillCheck::success),
            Codec.STRING.optionalFieldOf("failure").forGetter(SkillCheck::failure),
            NpcAction.CODEC.listOf()
                .optionalFieldOf("success_actions", List.of())
                .forGetter(SkillCheck::successActions),
            NpcAction.CODEC.listOf()
                .optionalFieldOf("failure_actions", List.of())
                .forGetter(SkillCheck::failureActions),
            Codec.BOOL.optionalFieldOf("once").forGetter(SkillCheck::once),
            Codec.BOOL.optionalFieldOf("hide_failed", false).forGetter(SkillCheck::hideFailed),
            Codec.INT.optionalFieldOf("xp").forGetter(SkillCheck::xp)
        ).apply(instance, SkillCheck::new)
    );

    public boolean oneShot() {
        return once.orElse(mode == Mode.CHANCE);
    }

    public int current(ServerPlayer player, @Nullable NpcEntity npc) {
        return switch (type) {
            case SPECIAL -> SpecialStatResolver.resolve(player).effective(stat.orElse(SpecialAttribute.CHARISMA));
            case LEVEL -> SpecialService.get(player).level();
            case PERK -> id.map(perk -> SpecialService.get(player).equippedRank(perk)).orElse(0);
            case REPUTATION -> id.map(faction -> StoryService.reputation(player, faction)).orElse(0);
            case AFFINITY -> target(npc).map(npcId -> StoryService.affinity(player, npcId)).orElse(0);
        };
    }

    private Optional<Identifier> target(@Nullable NpcEntity npc) {
        return id.isPresent() ? id : npc == null ? Optional.empty() : npc.config().identifier();
    }

    public boolean passes(ServerPlayer player, @Nullable NpcEntity npc) {
        return current(player, npc) >= value;
    }

    public float chance(ServerPlayer player, @Nullable NpcEntity npc) {
        var config = AtomicDawn.CONFIG.story;
        var ratio = value <= 0 ? 1F : (float) current(player, npc) / value;
        var luck = SpecialStatResolver.resolve(player).effective(SpecialAttribute.LUCK);
        var chance = ratio * (float) config.DIALOGUE_CHANCE_AT_TARGET + luck * (float) config.DIALOGUE_LUCK_BONUS;
        return Math.clamp(chance, (float) config.DIALOGUE_CHANCE_MIN, (float) config.DIALOGUE_CHANCE_MAX);
    }

    public boolean roll(ServerPlayer player, @Nullable NpcEntity npc) {
        if (mode == Mode.REQUIREMENT) {
            return passes(player, npc);
        }
        return player.getRandom().nextFloat() < chance(player, npc);
    }

    public boolean visible(ServerPlayer player, @Nullable NpcEntity npc) {
        return !(hideFailed && mode == Mode.REQUIREMENT && !passes(player, npc));
    }

    public MutableComponent label(ServerPlayer player, @Nullable NpcEntity npc) {
        Component name = switch (type) {
            case SPECIAL -> Component.translatable(stat.orElse(SpecialAttribute.CHARISMA).translationKey());
            case LEVEL -> Component.translatable("dialogue.atomicdawn.check.level");
            case PERK -> Component.translatable(
                id.map(perk -> "perk." + perk.getNamespace() + "." + perk.getPath())
                    .orElse("dialogue.atomicdawn.check.perk")
            );
            case REPUTATION -> id.flatMap(faction -> StoryService.faction(player.level().registryAccess(), faction))
                .map(faction -> (Component) Component.translatableWithFallback(faction.name(), faction.name()))
                .orElse(Component.translatable("dialogue.atomicdawn.check.reputation"));
            case AFFINITY -> Component.translatable("dialogue.atomicdawn.check.affinity");
        };
        if (mode == Mode.CHANCE) {
            var percent = Math.round(chance(player, npc) * 100F);
            return Component.literal("[")
                .append(name.copy())
                .append(" " + percent + "%] ")
                .withStyle(ChatFormatting.YELLOW);
        }
        var shown = type == Type.PERK
            ? Component.literal("[").append(name.copy()).append("] ")
            : Component.literal(
                "["
            ).append(name.copy()).append(" " + value + "] ");
        return shown.withStyle(passes(player, npc) ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    public int experience() {
        return xp.orElse(AtomicDawn.CONFIG.story.DIALOGUE_CHECK_XP);
    }
}

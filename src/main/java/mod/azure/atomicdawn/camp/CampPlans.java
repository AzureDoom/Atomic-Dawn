package mod.azure.atomicdawn.camp;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.quest.QuestActions;
import mod.azure.atomicdawn.registry.ADRegistries;

public record CampPlans(List<Identifier> known) {

    public static final CampPlans EMPTY = new CampPlans(List.of());

    public static final MapCodec<CampPlans> MAP_CODEC = Identifier.CODEC.listOf()
        .optionalFieldOf("known", List.of())
        .xmap(CampPlans::new, CampPlans::known);

    public static final StreamCodec<ByteBuf, CampPlans> STREAM_CODEC = Identifier.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(CampPlans::new, CampPlans::known);

    public CampPlans {
        known = List.copyOf(known);
    }

    public static CampPlans of(Player player) {
        return player.getData(CampAttachments.CAMP_PLANS.get());
    }

    public static boolean unlocked(Player player, CampBuildable definition) {
        if (!AtomicDawn.CONFIG.camp.CAMP_REQUIRE_PLANS || definition.plan().isEmpty()) {
            return true;
        }
        return of(player).known().contains(definition.plan().get());
    }

    public static boolean unlocked(Player player, CampBlueprint blueprint) {
        if (!AtomicDawn.CONFIG.camp.CAMP_REQUIRE_PLANS || blueprint.plan().isEmpty()) {
            return true;
        }
        return of(player).known().contains(blueprint.plan().get());
    }

    public static List<Identifier> all(HolderLookup.Provider registries) {
        var plans = new TreeSet<>(Comparator.comparing(Identifier::toString));
        registries.lookup(ADRegistries.CAMP_BUILDABLE)
            .ifPresent(
                registry -> registry.listElements()
                    .filter(holder -> holder.value().placeable())
                    .forEach(holder -> holder.value().plan().ifPresent(plans::add))
            );
        registries.lookup(ADRegistries.CAMP_BLUEPRINT)
            .ifPresent(
                registry -> registry.listElements().forEach(holder -> holder.value().plan().ifPresent(plans::add))
            );
        return List.copyOf(plans);
    }

    public static Optional<Identifier> random(HolderLookup.Provider registries, RandomSource random) {
        var plans = all(registries);
        return plans.isEmpty() ? Optional.empty() : Optional.of(plans.get(random.nextInt(plans.size())));
    }

    public static Optional<Identifier> randomUnknown(Player player, RandomSource random) {
        var known = of(player);
        var plans = all(player.level().registryAccess()).stream().filter(plan -> !known.has(plan)).toList();
        return plans.isEmpty() ? Optional.empty() : Optional.of(plans.get(random.nextInt(plans.size())));
    }

    public static boolean learn(ServerPlayer player, Identifier plan) {
        var plans = of(player);
        if (plans.has(plan)) {
            player.sendSystemMessage(
                Component.translatable("camp.atomicdawn.plan.known", displayName(plan)).withStyle(ChatFormatting.GRAY),
                true
            );
            return false;
        }
        player.setData(CampAttachments.CAMP_PLANS.get(), plans.with(plan));
        QuestActions.trigger(player, QuestActions.LEARN_PLAN);
        player.sendSystemMessage(
            Component.translatable("camp.atomicdawn.plan.learned", displayName(plan)).withStyle(ChatFormatting.GREEN),
            true
        );
        return true;
    }

    public static Component displayName(Identifier plan) {
        var fallback = plan.getPath().replace('_', ' ');
        return Component.translatableWithFallback("plan." + plan.getNamespace() + "." + plan.getPath(), fallback);
    }

    public boolean has(Identifier plan) {
        return known.contains(plan);
    }

    public CampPlans with(Identifier plan) {
        if (has(plan)) {
            return this;
        }
        var list = new ArrayList<>(known);
        list.add(plan);
        return new CampPlans(list);
    }

    public CampPlans without(Identifier plan) {
        return new CampPlans(known.stream().filter(id -> !id.equals(plan)).toList());
    }
}

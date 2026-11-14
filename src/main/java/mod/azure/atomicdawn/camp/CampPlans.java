package mod.azure.atomicdawn.camp;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.AtomicDawn;

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

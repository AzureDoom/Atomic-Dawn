package mod.azure.atomicdawn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.crafting.WorkbenchMenu;
import mod.azure.atomicdawn.crafting.WorkbenchService;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.weapon.modification.WeaponModding.Result;

public record WorkbenchActionPayload(
    int containerId,
    Action action,
    Optional<Identifier> target
) implements CustomPacketPayload {

    public static final Type<WorkbenchActionPayload> TYPE = new Type<>(AtomicDawn.id("workbench_action"));

    private static final Action[] ACTIONS = Action.values();

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchActionPayload> STREAM_CODEC = StreamCodec
        .composite(
            ByteBufCodecs.VAR_INT,
            WorkbenchActionPayload::containerId,
            ByteBufCodecs.VAR_INT.map(index -> ACTIONS[Math.floorMod(index, ACTIONS.length)], Action::ordinal),
            WorkbenchActionPayload::action,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC),
            WorkbenchActionPayload::target,
            WorkbenchActionPayload::new
        );

    public enum Action {
        REPAIR,
        REPAIR_WITH_KIT,
        SCRAP,
        CRAFT,
        APPLY_LEGENDARY
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(WorkbenchActionPayload payload, IPayloadContext context) {
        if (
            !(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof WorkbenchMenu menu)
                || menu.containerId != payload.containerId()
                || !menu.stillValid(player)
        ) {
            return;
        }
        var result = switch (payload.action()) {
            case REPAIR -> WorkbenchService.repair(player, menu.weapon(), false);
            case REPAIR_WITH_KIT -> WorkbenchService.repair(player, menu.weapon(), true);
            case SCRAP -> WorkbenchService.scrap(player, menu);
            case CRAFT -> payload.target()
                .map(id -> WorkbenchService.craft(player, menu, ResourceKey.create(ADRegistries.WEAPON_DEFINITION, id)))
                .orElse(Result.NOTHING_TO_DO);
            case APPLY_LEGENDARY -> payload.target()
                .map(
                    id -> WorkbenchService.applyLegendary(
                        player,
                        menu.weapon(),
                        ResourceKey.create(ADRegistries.LEGENDARY_EFFECT, id)
                    )
                )
                .orElse(Result.NOTHING_TO_DO);
        };
        menu.broadcastChanges();
        var success = result == Result.REPAIRED
            || result == Result.SCRAPPED
            || result == Result.CRAFTED
            || result == Result.LEGENDARY_APPLIED;
        if (success) {
            var sound = result == Result.SCRAPPED ? SoundEvents.GRINDSTONE_USE : SoundEvents.SMITHING_TABLE_USE;
            menu.position().ifPresent(pos -> player.level().playSound(null, pos, sound, SoundSource.BLOCKS, 1F, 1F));
        }
        ADNetwork.sendTo(player, new WorkbenchResultPayload(result));
    }
}

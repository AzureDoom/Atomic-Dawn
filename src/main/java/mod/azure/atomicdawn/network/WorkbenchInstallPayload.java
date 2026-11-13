package mod.azure.atomicdawn.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.crafting.WorkbenchMenu;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.weapon.modification.ModSlot;
import mod.azure.atomicdawn.weapon.modification.WeaponModding;
import mod.azure.atomicdawn.weapon.modification.WeaponModification;

public record WorkbenchInstallPayload(
    int containerId,
    ModSlot slot,
    Optional<ResourceKey<WeaponModification>> mod
) implements CustomPacketPayload {

    public static final Type<WorkbenchInstallPayload> TYPE = new Type<>(AtomicDawn.id("workbench_install"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchInstallPayload> STREAM_CODEC = StreamCodec
        .composite(
            ByteBufCodecs.VAR_INT,
            WorkbenchInstallPayload::containerId,
            ModSlot.STREAM_CODEC,
            WorkbenchInstallPayload::slot,
            ByteBufCodecs.optional(ResourceKey.streamCodec(ADRegistries.WEAPON_MODIFICATION)),
            WorkbenchInstallPayload::mod,
            WorkbenchInstallPayload::new
        );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(WorkbenchInstallPayload payload, IPayloadContext context) {
        if (
            !(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof WorkbenchMenu menu)
                || menu.containerId != payload.containerId()
                || !menu.stillValid(player)
        ) {
            return;
        }
        var result = WeaponModding.install(player, menu.weapon(), payload.slot(), payload.mod());
        menu.broadcastChanges();
        if (result == WeaponModding.Result.INSTALLED) {
            menu.position()
                .ifPresent(
                    pos -> player.level()
                        .playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1F, 1F)
                );
        }
        ADNetwork.sendTo(player, new WorkbenchResultPayload(result));
    }
}

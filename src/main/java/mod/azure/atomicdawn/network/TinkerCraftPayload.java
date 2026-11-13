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

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.crafting.TinkerService;
import mod.azure.atomicdawn.crafting.TinkersBenchMenu;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.weapon.modification.WeaponModding.Result;

public record TinkerCraftPayload(
    int containerId,
    Identifier recipe,
    int times
) implements CustomPacketPayload {

    public static final Type<TinkerCraftPayload> TYPE = new Type<>(AtomicDawn.id("tinker_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TinkerCraftPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TinkerCraftPayload::containerId,
        Identifier.STREAM_CODEC,
        TinkerCraftPayload::recipe,
        ByteBufCodecs.VAR_INT,
        TinkerCraftPayload::times,
        TinkerCraftPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handleOnServer(TinkerCraftPayload payload, IPayloadContext context) {
        if (
            !(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof TinkersBenchMenu menu)
                || menu.containerId != payload.containerId()
                || !menu.stillValid(player)
        ) {
            return;
        }
        var result = TinkerService.craft(
            player,
            ResourceKey.create(ADRegistries.TINKER_RECIPE, payload.recipe()),
            payload.times()
        );
        menu.broadcastChanges();
        if (result == Result.CRAFTED) {
            menu.position()
                .ifPresent(
                    pos -> player.level()
                        .playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1F, 1.2F)
                );
        }
        ADNetwork.sendTo(player, new WorkbenchResultPayload(result));
    }
}

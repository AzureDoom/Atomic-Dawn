package mod.azure.atomicdawn.thirst;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.radiation.RadiationService;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class SourceDrinking {

    private static final Map<UUID, Long> LAST_SIP = new ConcurrentHashMap<>();

    private SourceDrinking() {}

    public static boolean enabled() {
        return ThirstService.enabled() && AtomicDawn.CONFIG.common.survival.thirst.drinks.SOURCE_DRINK_ENABLED;
    }

    public static int cooldown() {
        return Math.max(1, AtomicDawn.CONFIG.common.survival.thirst.drinks.SOURCE_DRINK_COOLDOWN_TICKS);
    }

    public static boolean drinkable(FluidState fluid) {
        return fluid.isSource() && (fluid.is(FluidTags.WATER) || DirtyWater.isIrradiated(fluid));
    }

    public static @Nullable BlockPos target(Player player) {
        if (
            !enabled() || player.isSpectator() || !player.isAlive() || !player.getMainHandItem().isEmpty()
                || ThirstService.thirst(player) >= Thirst.MAX
        ) {
            return null;
        }
        var hit = player.pick(player.blockInteractionRange(), 1F, true);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return null;
        }
        var pos = blockHit.getBlockPos();
        return drinkable(player.level().getFluidState(pos)) ? pos : null;
    }

    public static void sip(ServerPlayer player, BlockPos pos) {
        if (!enabled() || player.isSpectator() || !player.isAlive() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        if (ThirstService.thirst(player) >= Thirst.MAX || !player.isWithinBlockInteractionRange(pos, 1.0)) {
            return;
        }
        var level = player.level();
        if (!level.isLoaded(pos)) {
            return;
        }
        var fluid = level.getFluidState(pos);
        if (!drinkable(fluid)) {
            return;
        }
        var now = level.getGameTime();
        var last = LAST_SIP.get(player.getUUID());
        if (last != null && now - last < cooldown()) {
            return;
        }
        LAST_SIP.put(player.getUUID(), now);

        ThirstService.drink(player, AtomicDawn.CONFIG.common.survival.thirst.drinks.SOURCE_DRINK_THIRST);
        var consumables = AtomicDawn.CONFIG.common.survival.radiation.consumables;
        var rads = DirtyWater.isIrradiated(fluid)
            ? consumables.SOURCE_DRINK_IRRADIATED_RADS
            : DirtyWater.isDirty(level, pos, fluid) ? consumables.SOURCE_DRINK_RADS : 0.0;
        if (rads > 0.0) {
            RadiationService.expose(player, (float) rads);
        }
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.GENERIC_DRINK,
            SoundSource.PLAYERS,
            0.5F,
            0.9F + level.getRandom().nextFloat() * 0.2F
        );
        level.sendParticles(
            ParticleTypes.SPLASH,
            pos.getX() + 0.5,
            pos.getY() + 0.9,
            pos.getZ() + 0.5,
            6,
            0.2,
            0.05,
            0.2,
            0.0
        );
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SIP.remove(event.getEntity().getUUID());
    }
}

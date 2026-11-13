package mod.azure.atomicdawn.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.registry.ADFluids;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class RadiationEvents {

    private static final TagKey<Block> RADIATION_HOTSPOT_BLOCKS =
        TagKey.create(Registries.BLOCK, AtomicDawn.id("radiation_hotspot_blocks"));

    private static final ResourceKey<Biome> TOXIC_VALLEY = biome("toxic_valley");

    private static final ResourceKey<Biome> ASH_HEAP = biome("ash_heap");

    private static final ResourceKey<Biome> IRRADIATED_MIRE = biome("irradiated_mire");

    private static final ResourceKey<Biome> CRIMSON_BOG = biome("crimson_bog");

    private static final ResourceKey<Biome> BURNING_SPRINGS = biome("burning_springs");

    private static final ResourceKey<Biome> SKYLINE_VALLEY = biome("skyline_valley");

    private static ResourceKey<Biome> biome(String id) {
        return ResourceKey.create(Registries.BIOME, AtomicDawn.id(id));
    }

    private RadiationEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        if ((living.tickCount + living.getId()) % 20 != 0) {
            return;
        }
        var type = ADFluids.IRRADIATED_WATER_TYPE.value();
        if (living.isInFluidType(type)) {
            var rate = (float) AtomicDawn.CONFIG.radiation.RAD_WATER_RADS_PER_SECOND;
            RadiationService.expose(living, living.isEyeInFluidType(type) ? rate * 2F : rate);
        }
        if (living instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                if (RadiationService.rads(player) > 0F) {
                    RadiationService.set(player, 0F);
                }
                if (player.isSpectator() || !GhoulService.isGhoul(player)) {
                    return;
                }
            }
            if (AtomicDawn.CONFIG.environmentalRadiation.BIOME_RADIATION_ENABLED) {
                float ambient = ambientRadiation(player);
                if (ambient > 0F) {
                    RadiationService.expose(player, ambient);
                }
            }
            if (AtomicDawn.CONFIG.environmentalRadiation.RADIATION_HOTSPOTS_ENABLED) {
                float hotspot = hotspotRadiation(player);
                if (hotspot > 0F) {
                    RadiationService.expose(player, hotspot);
                }
            }
            var decay = (float) AtomicDawn.CONFIG.radiation.RADS_DECAY_PER_SECOND;
            if (decay > 0F && RadiationService.rads(player) > 0F) {
                RadiationService.remove(player, decay);
            }
            RadiationService.updateMaxHealth(player);
        }
    }

    private static float ambientRadiation(Player player) {
        var current = player.level().getBiome(player.blockPosition());
        if (current.is(TOXIC_VALLEY))
            return (float) AtomicDawn.CONFIG.environmentalRadiation.TOXIC_VALLEY_RADS_PER_SECOND;
        if (current.is(ASH_HEAP))
            return (float) AtomicDawn.CONFIG.environmentalRadiation.ASH_HEAP_RADS_PER_SECOND;
        if (current.is(IRRADIATED_MIRE))
            return (float) AtomicDawn.CONFIG.environmentalRadiation.IRRADIATED_MIRE_RADS_PER_SECOND;
        if (current.is(CRIMSON_BOG))
            return (float) AtomicDawn.CONFIG.environmentalRadiation.CRIMSON_BOG_RADS_PER_SECOND;
        if (current.is(BURNING_SPRINGS))
            return (float) AtomicDawn.CONFIG.environmentalRadiation.BURNING_SPRINGS_RADS_PER_SECOND;
        if (current.is(SKYLINE_VALLEY))
            return (float) AtomicDawn.CONFIG.environmentalRadiation.SKYLINE_VALLEY_RADS_PER_SECOND;
        return 0F;
    }

    private static float hotspotRadiation(Player player) {
        int radius = AtomicDawn.CONFIG.environmentalRadiation.RADIATION_HOTSPOT_RADIUS;
        if (radius <= 0)
            return 0F;
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos();
        double minDistanceSq = Double.POSITIVE_INFINITY;
        int vertical = Math.min(radius, 5);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -vertical; y <= vertical; y++) {
                    double distanceSq = (double) x * x + (double) y * y + (double) z * z;
                    if (distanceSq > radius * radius || distanceSq >= minDistanceSq)
                        continue;
                    test.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!player.level().isLoaded(test))
                        continue;
                    if (player.level().getBlockState(test).is(RADIATION_HOTSPOT_BLOCKS)) {
                        minDistanceSq = distanceSq;
                    }
                }
            }
        }
        if (!Double.isFinite(minDistanceSq))
            return 0F;
        float proximity = 1F - (float) Math.sqrt(minDistanceSq) / (radius + 1F);
        return (float) AtomicDawn.CONFIG.environmentalRadiation.RADIATION_HOTSPOT_RADS_PER_SECOND * Math.max(
            0F,
            proximity
        );
    }

    @SubscribeEvent
    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        var mode = event.getNewGameMode();
        if (
            (mode == GameType.CREATIVE || mode == GameType.SPECTATOR) && !event.getEntity().level().isClientSide()
        ) {
            RadiationService.set(event.getEntity(), 0F);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        RadiationService.updateMaxHealth(event.getEntity());
    }
}

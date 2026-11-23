package mod.azure.atomicdawn.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.registry.ADFluids;
import mod.azure.atomicdawn.worldgen.ADBiomes;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class RadiationEvents {

    private static final TagKey<Block> RADIATION_HOTSPOT_BLOCKS =
        TagKey.create(Registries.BLOCK, AtomicDawn.id("radiation_hotspot_blocks"));

    public static final TagKey<Structure> RADIATION_SITES = TagKey.create(
        Registries.STRUCTURE,
        AtomicDawn.id("radiation_sites")
    );

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
            RadStormService.sync(player);
            if (player.isCreative() || player.isSpectator()) {
                if (RadiationService.rads(player) > 0F) {
                    RadiationService.set(player, 0F);
                }
                if (player.isSpectator() || !GhoulService.isGhoul(player)) {
                    return;
                }
            }
            float water = wastelandWaterRadiation(player);
            if (water > 0F) {
                RadiationService.expose(player, water);
            }
            if (player.level() instanceof ServerLevel level) {
                float environmental = environmentalRadiation(level, player);
                if (environmental > 0F) {
                    RadiationService.expose(player, environmental);
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

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var tick = server.getTickCount();
        if (tick % 20 != 0) {
            return;
        }
        for (var level : server.getAllLevels()) {
            RadStormService.tick(level);
            if (tick % 1200 == 0) {
                BlastZoneService.prune(level);
            }
        }
    }

    private static float wastelandWaterRadiation(Player player) {
        var type = NeoForgeMod.WATER_TYPE.value();
        if (!player.isInFluidType(type) || !player.level().getBiome(player.blockPosition()).is(ADBiomes.WASTELAND)) {
            return 0F;
        }
        var rate = (float) AtomicDawn.CONFIG.environmentalRadiation.WASTELAND_WATER_RADS_PER_SECOND;
        return player.isEyeInFluidType(type) ? rate * 2F : rate;
    }

    private static float environmentalRadiation(ServerLevel level, Player player) {
        var pos = player.blockPosition();
        var total = BlastZoneService.radiation(level, pos);
        total += RadStormService.radiation(level, player);
        if (AtomicDawn.CONFIG.environmentalRadiation.RADIATION_SITES_ENABLED) {
            total += siteRadiation(level, pos);
        }
        return total;
    }

    private static float siteRadiation(ServerLevel level, BlockPos pos) {
        var rate = (float) AtomicDawn.CONFIG.environmentalRadiation.RADIATION_SITE_RADS_PER_SECOND;
        if (rate <= 0F) {
            return 0F;
        }
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var manager = level.structureManager();
        var best = 0F;
        for (var structure : manager.getAllStructuresAt(pos).keySet()) {
            if (!structures.wrapAsHolder(structure).is(RADIATION_SITES)) {
                continue;
            }
            var start = manager.getStructureAt(pos, structure);
            if (!start.isValid()) {
                continue;
            }
            var box = start.getBoundingBox();
            var center = box.getCenter();
            var reach = Math.max(box.getXSpan(), box.getZSpan()) / 2.0;
            var dx = pos.getX() - center.getX();
            var dz = pos.getZ() - center.getZ();
            var distance = Math.sqrt((double) dx * dx + (double) dz * dz);
            var proximity = 1F - 0.75F * (float) Math.min(1.0, distance / Math.max(1.0, reach));
            best = Math.max(best, rate * proximity);
        }
        return best;
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

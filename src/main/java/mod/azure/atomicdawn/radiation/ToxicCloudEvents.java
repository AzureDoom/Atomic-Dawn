package mod.azure.atomicdawn.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.helper.ADWorldgenUtil;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class ToxicCloudEvents {

    public static final TagKey<Structure> TOXIC_CLOUD_SITES = TagKey.create(
        Registries.STRUCTURE,
        AtomicDawn.id("toxic_cloud_sites")
    );

    private static final DustParticleOptions CLOUD = new DustParticleOptions(0xB8553A, 1.6F);

    private ToxicCloudEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        if (
            !AtomicDawn.CONFIG.common.survival.radiation.sites.TOXIC_CLOUDS_ENABLED || player.isCreative() || player
                .isSpectator()
        ) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !inside(level, player.blockPosition())) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        level.sendParticles(CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 12, 2.5, 1.0, 2.5, 0.0);
    }

    public static boolean inside(ServerLevel level, BlockPos pos) {
        return ADWorldgenUtil.insideStructure(level, pos, TOXIC_CLOUD_SITES);
    }
}

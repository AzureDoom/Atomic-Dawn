package mod.azure.atomicdawn.weapon.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.network.NukeExplosionPayload;
import mod.azure.atomicdawn.radiation.FalloutCloudEntity;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADBlocks;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.registry.ADSounds;
import mod.azure.atomicdawn.weapon.action.WeaponActions;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public final class Detonation {

    private static final ExplosionDamageCalculator NO_ENTITY_DAMAGE = new ExplosionDamageCalculator() {

        @Override
        public boolean shouldDamageEntity(@NotNull Explosion explosion, @NotNull Entity entity) {
            return false;
        }
    };

    private static final double EFFECT_BROADCAST_RANGE = 512.0;

    private Detonation() {}

    public static void detonate(
        ServerLevel level,
        Entity projectile,
        @Nullable Entity owner,
        ItemStack weapon,
        @Nullable WeaponDefinition definition,
        WeaponDefinition.Projectile parameters,
        float damage,
        Vec3 at
    ) {
        var radius = parameters.blastRadius();
        splash(level, projectile, owner, weapon, definition, damage, radius, at);

        var breakBlocks = parameters.breakBlocks() && AtomicDawn.CONFIG.radiation.EXPLOSIONS_BREAK_BLOCKS;
        level.explode(
            projectile,
            null,
            NO_ENTITY_DAMAGE,
            at.x,
            at.y,
            at.z,
            parameters.explosionPower(),
            false,
            breakBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE
        );

        if (!parameters.nuclear()) {
            return;
        }
        level.playSound(null, at.x, at.y, at.z, ADSounds.MINI_NUKE_EXPLODE.get(), SoundSource.BLOCKS, 16F, 1F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 8F, 0.5F);
        PacketDistributor.sendToPlayersNear(
            level,
            null,
            at.x,
            at.y,
            at.z,
            EFFECT_BROADCAST_RANGE,
            new NukeExplosionPayload(at, radius)
        );
        radiationBurst(level, radius * 2F, at);
        irradiateWater(level, BlockPos.containing(at), AtomicDawn.CONFIG.radiation.NUKE_WATER_RADIUS);
        int duration = AtomicDawn.CONFIG.radiation.FALLOUT_DURATION_TICKS;
        if (duration > 0) {
            level.addFreshEntity(FalloutCloudEntity.create(level, at.x, at.y, at.z, radius * 1.5F, duration));
        }
    }

    private static void splash(
        ServerLevel level,
        Entity projectile,
        @Nullable Entity owner,
        ItemStack weapon,
        @Nullable WeaponDefinition definition,
        float damage,
        float radius,
        Vec3 at
    ) {
        if (radius <= 0F || damage <= 0F) {
            return;
        }
        var area = new AABB(at, at).inflate(radius);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            var distance = target.getBoundingBox().getCenter().distanceTo(at);
            if (distance > radius) {
                continue;
            }
            var falloff = Math.max(0.1F, 1F - (float) (distance / radius));
            var amount = damage * falloff;
            if (owner instanceof ServerPlayer player && definition != null) {
                DamageApplier.apply(
                    level,
                    player,
                    weapon,
                    definition,
                    WeaponActions.shooter(player),
                    target,
                    at,
                    amount
                );
            } else {
                var type = definition != null ? definition.damage().type() : ADDamageTypes.NUCLEAR;
                var holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type);
                target.hurtServer(level, new DamageSource(holder, projectile, owner), amount);
            }
            var push = target.position().subtract(at).normalize().scale(1.5 * falloff);
            target.push(push.x, Math.max(0.3, push.y + 0.4) * falloff, push.z);
            target.needsSync = true;
        }
    }

    private static void radiationBurst(ServerLevel level, float radius, Vec3 at) {
        var burst = (float) AtomicDawn.CONFIG.radiation.NUKE_RADIATION_BURST;
        if (burst <= 0F || radius <= 0F) {
            return;
        }
        var area = new AABB(at, at).inflate(radius);
        for (var living : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            var distance = living.position().distanceTo(at);
            if (distance < radius) {
                RadiationService.expose(living, burst * (1F - (float) (distance / radius)));
            }
        }
    }

    public static void irradiateWater(ServerLevel level, BlockPos center, double radius) {
        if (radius <= 0) {
            return;
        }
        var r = (int) Math.ceil(radius);
        var radiusSq = radius * radius;
        var target = ADBlocks.IRRADIATED_WATER.get().defaultBlockState();
        var cursor = new BlockPos.MutableBlockPos();
        for (var dx = -r; dx <= r; dx++) {
            for (var dy = -r; dy <= r; dy++) {
                for (var dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    var state = level.getBlockState(cursor);
                    if (!state.is(Blocks.WATER)) {
                        continue;
                    }
                    var fluid = state.getFluidState();
                    if (!fluid.is(Fluids.WATER) && !fluid.is(Fluids.FLOWING_WATER)) {
                        continue;
                    }
                    level.setBlock(
                        cursor,
                        target.setValue(LiquidBlock.LEVEL, state.getValue(LiquidBlock.LEVEL)),
                        Block.UPDATE_CLIENTS
                    );
                }
            }
        }
    }
}

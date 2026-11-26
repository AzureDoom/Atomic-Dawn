package mod.azure.atomicdawn.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.helper.ADSerialization;
import mod.azure.atomicdawn.weapon.combat.Detonation;
import mod.azure.atomicdawn.weapon.definition.WeaponDefinition;

public class ExplosiveProjectileEntity extends ThrowableProjectile {

    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(
        ExplosiveProjectileEntity.class,
        EntityDataSerializers.FLOAT
    );

    private static final EntityDataAccessor<Boolean> FALLING = SynchedEntityData.defineId(
        ExplosiveProjectileEntity.class,
        EntityDataSerializers.BOOLEAN
    );

    private static final int DROP_RAMP_TICKS = 12;

    private float damage = 100F;

    private WeaponDefinition.Projectile parameters = new WeaponDefinition.Projectile(
        1.5F,
        0.03F,
        6F,
        3F,
        true,
        false,
        200,
        0F
    );

    private ItemStack weapon = ItemStack.EMPTY;

    @Nullable
    private WeaponDefinition definition;

    private boolean detonated;

    private double traveled;

    private int fallingTicks;

    public ExplosiveProjectileEntity(EntityType<? extends ExplosiveProjectileEntity> type, Level level) {
        super(type, level);
    }

    public void configure(
        ItemStack weapon,
        WeaponDefinition definition,
        WeaponDefinition.Projectile parameters,
        float damage
    ) {
        this.weapon = weapon.copy();
        this.definition = definition;
        this.parameters = parameters;
        this.damage = damage;
        entityData.set(GRAVITY, parameters.gravity());
        entityData.set(FALLING, parameters.flatRange() <= 0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(GRAVITY, 0.03F);
        builder.define(FALLING, true);
    }

    @Override
    protected double getDefaultGravity() {
        if (!entityData.get(FALLING)) {
            return 0.0;
        }
        var ramp = Math.min(1F, (fallingTicks + 1F) / DROP_RAMP_TICKS);
        return entityData.get(GRAVITY) * ramp;
    }

    private void trackRange() {
        if (entityData.get(FALLING)) {
            fallingTicks++;
            return;
        }
        fallingTicks = 0;
        if (level().isClientSide()) {
            return;
        }
        traveled += position().distanceTo(new Vec3(xo, yo, zo));
        if (traveled >= parameters.flatRange()) {
            entityData.set(FALLING, true);
        }
    }

    private void faceVelocity() {
        var motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6) {
            return;
        }
        var horizontal = motion.horizontalDistance();
        setYRot((float) (Mth.atan2(-motion.x, motion.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(-motion.y, horizontal) * Mth.RAD_TO_DEG));
        if (tickCount <= 1) {
            yRotO = getYRot();
            xRotO = getXRot();
        }
    }

    @Override
    public void tick() {
        yRotO = getYRot();
        xRotO = getXRot();
        super.tick();
        trackRange();
        faceVelocity();
        var level = level();
        if (level.isClientSide()) {
            var back = getDeltaMovement().normalize().scale(-0.4);
            level.addParticle(
                ParticleTypes.SMOKE,
                getX() + back.x,
                getY() + 0.25 + back.y,
                getZ() + back.z,
                0,
                0.01,
                0
            );
            if (tickCount % 2 == 0) {
                level.addParticle(
                    ParticleTypes.FLAME,
                    getX() + back.x,
                    getY() + 0.25 + back.y,
                    getZ() + back.z,
                    0,
                    0,
                    0
                );
            }
            return;
        }
        if (tickCount >= parameters.fuseTicks()) {
            detonate(position());
        }
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        if (!level().isClientSide()) {
            detonate(result.getLocation());
        }
    }

    private void detonate(Vec3 at) {
        if (detonated || !(level() instanceof ServerLevel level)) {
            return;
        }
        detonated = true;
        Detonation.detonate(level, this, getOwner(), weapon, definition, parameters, damage, at);
        discard();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", damage);
        output.putDouble("Traveled", traveled);
        output.putBoolean("Falling", entityData.get(FALLING));
        ADSerialization.store(output, "Parameters", WeaponDefinition.Projectile.CODEC, parameters);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {
        super.readAdditionalSaveData(input);
        damage = ADSerialization.getFloat(input, "Damage", damage);
        traveled = ADSerialization.getDouble(input, "Traveled", 0.0);
        entityData.set(FALLING, ADSerialization.getBoolean(input, "Falling", true));
        ADSerialization.read(input, "Parameters", WeaponDefinition.Projectile.CODEC).ifPresent(p -> {
            parameters = p;
            entityData.set(GRAVITY, p.gravity());
        });
    }
}

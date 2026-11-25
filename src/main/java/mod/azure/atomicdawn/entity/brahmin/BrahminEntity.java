package mod.azure.atomicdawn.entity.brahmin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import mod.azure.atomicdawn.registry.ADEntities;
import mod.azure.atomicdawn.registry.ADItems;

public class BrahminEntity extends Animal {

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(
        BrahminEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(
        BrahminEntity.class,
        EntityDataSerializers.INT
    );

    private static final EntityDataAccessor<Integer> DATA_ACTION_ID = SynchedEntityData.defineId(
        BrahminEntity.class,
        EntityDataSerializers.INT
    );

    private static final byte EAT_EVENT = 10;

    private static final double RUN_THRESHOLD_SQR = 0.12 * 0.12;

    private static final double WALK_THRESHOLD_SQR = 0.012 * 0.012;

    private int clientState = -1;

    private int clientActionId;

    private int clientRedispatchAt = -1;

    public BrahminEntity(EntityType<? extends BrahminEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.2, stack -> stack.is(ItemTags.COW_FOOD), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.2));
        goalSelector.addGoal(5, new EatBlockGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, BrahminState.IDLE.ordinal());
        builder.define(DATA_ACTION, BrahminAction.NONE.ordinal());
        builder.define(DATA_ACTION_ID, 0);
    }

    public void playAction(BrahminAction action) {
        entityData.set(DATA_ACTION, action.ordinal());
        entityData.set(DATA_ACTION_ID, entityData.get(DATA_ACTION_ID) + 1);
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return stack.is(ItemTags.COW_FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob partner) {
        return ADEntities.BRAHMIN.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (stack.is(Items.GLASS_BOTTLE) && !isBaby()) {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 0.9F);
            var filled = ItemUtils.createFilledResult(stack, player, new ItemStack(ADItems.BRAHMIN_MILK.get()));
            player.setItemInHand(hand, filled);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientAnimations();
            return;
        }
        updateState();
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide() && isAlive() && random.nextInt(3) == 0) {
            playAction(BrahminAction.CALL);
        }
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        var hurt = super.hurtServer(level, source, amount);
        if (hurt && isAlive()) {
            playAction(BrahminAction.SHAKE);
        }
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EAT_EVENT) {
            BrahminAnimations.action(this, BrahminAction.GRAZE);
            return;
        }
        super.handleEntityEvent(id);
    }

    private void updateState() {
        BrahminState state;
        if (!isAlive()) {
            state = BrahminState.DEAD;
        } else {
            var dx = getX() - xo;
            var dz = getZ() - zo;
            var speed = dx * dx + dz * dz;
            state = speed > RUN_THRESHOLD_SQR
                ? BrahminState.RUN
                : speed > WALK_THRESHOLD_SQR ? BrahminState.WALK : BrahminState.IDLE;
        }
        if (state.ordinal() != entityData.get(DATA_STATE)) {
            entityData.set(DATA_STATE, state.ordinal());
        }
    }

    private void clientAnimations() {
        int state = entityData.get(DATA_STATE);
        if (state != clientState) {
            clientState = state;
            clientRedispatchAt = tickCount + 10;
            BrahminAnimations.state(this, BrahminState.byIndex(state));
        } else if (tickCount == clientRedispatchAt) {
            BrahminAnimations.state(this, BrahminState.byIndex(state));
        }
        var actionId = entityData.get(DATA_ACTION_ID);
        if (actionId != clientActionId) {
            clientActionId = actionId;
            BrahminAnimations.action(this, BrahminAction.byIndex(entityData.get(DATA_ACTION)));
        }
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        updateState();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.getSoundSet().ambientSound().value();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return this.getSoundSet().hurtSound().value();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return this.getSoundSet().deathSound().value();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        this.playSound(this.getSoundSet().stepSound().value(), 0.15F, 1.0F);
    }

    protected CowSoundVariant getSoundSet() {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }
}

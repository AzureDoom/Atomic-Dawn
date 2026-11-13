package mod.azure.atomicdawn.mutation;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.client.mixin.FoodDataAccessor;
import mod.azure.atomicdawn.party.PartyService;
import mod.azure.atomicdawn.perks.PerkEffectType;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.registry.ADMobEffects;
import mod.azure.atomicdawn.special.SpecialStatResolver;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID)
public final class MutationEvents {

    private static final class State {

        double lastX;

        double lastZ;

        long stillSince;

        float lastExhaustion = -1F;

        boolean hidden;

        final List<Holder<MobEffect>> toShorten = new ArrayList<>();
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static boolean reapplying;

    private MutationEvents() {}

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), _ -> new State());
    }

    private static boolean isMelee(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
            && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypes.THORNS);
    }

    private static boolean isEnergy(DamageSource source) {
        return source.is(ADDamageTypes.ENERGY) || source.is(DamageTypeTags.WITCH_RESISTANT_TO);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!MutationService.enabled()) {
            return;
        }
        var source = event.getSource();
        var victim = event.getEntity();
        var amount = event.getAmount();

        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            if (MutationService.negative(attacker, Mutation.GROUNDED) && source.is(ADDamageTypes.ENERGY)) {
                amount *= 0.5F;
            }
            if (MutationService.positive(attacker, Mutation.ADRENAL_REACTION)) {
                var missing = 1F - attacker.getHealth() / Math.max(1F, attacker.getMaxHealth());
                amount *= 1F + 0.5F * missing;
            }
            if (isMelee(source)) {
                if (attacker.getMainHandItem().isEmpty()) {
                    if (MutationService.positive(attacker, Mutation.TALONS)) {
                        amount *= 1.25F;
                        victim.addEffect(new MobEffectInstance(ADMobEffects.BLEEDING, 60, 0), attacker);
                    }
                } else if (MutationService.positive(attacker, Mutation.TWISTED_MUSCLES)) {
                    amount *= 1.25F;
                }
            }
        }

        if (victim instanceof ServerPlayer player && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            var physical = !isEnergy(source) && !source.is(DamageTypeTags.IS_FIRE)
                && !source.is(ADDamageTypes.RADIATION) && !source.is(DamageTypeTags.BYPASSES_ARMOR);
            if (physical && MutationService.positive(player, Mutation.SCALY_SKIN)) {
                amount *= 0.85F;
            }
            if (isEnergy(source) && MutationService.positive(player, Mutation.GROUNDED)) {
                amount *= 0.6F;
            }
            if (MutationService.negative(player, Mutation.EMPATH)) {
                amount *= 1.33F;
            }
            if (mateHasEmpath(player)) {
                amount *= 0.75F;
            }
            if (isMelee(source) && source.getEntity() instanceof LivingEntity attacker) {
                amount += meleeReactions(player, attacker);
            }
        }
        event.setAmount(amount);
    }

    private static boolean mateHasEmpath(ServerPlayer player) {
        for (var mate : PartyService.onlineMates(player)) {
            if (
                mate.level() == player.level() && mate.distanceToSqr(player) <= 64 * 64
                    && MutationService.positive(mate, Mutation.EMPATH)
            ) {
                return true;
            }
        }
        return false;
    }

    private static float meleeReactions(ServerPlayer player, LivingEntity attacker) {
        ServerLevel level = player.level();
        var extra = 0F;
        var random = player.getRandom();
        if (MutationService.positive(player, Mutation.ELECTRICALLY_CHARGED) && random.nextFloat() < 0.3F) {
            attacker.hurtServer(level, level.damageSources().thorns(player), 4F);
            if (MutationService.negative(player, Mutation.ELECTRICALLY_CHARGED)) {
                extra += 1F;
            }
        }
        if (MutationService.positive(player, Mutation.UNSTABLE_ISOTOPE) && random.nextFloat() < 0.25F) {
            var source = ADDamageTypes.source(level, ADDamageTypes.ENERGY, player);
            var area = player.getBoundingBox().inflate(4.0);
            for (var target : level.getEntitiesOfClass(LivingEntity.class, area, entity -> isFoe(player, entity))) {
                target.hurtServer(level, source, 5F);
                RadiationService.expose(target, 10F);
            }
            level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                player.getX(),
                player.getY() + 1,
                player.getZ(),
                30,
                2.0,
                1.0,
                2.0,
                0.1
            );
        }
        return extra;
    }

    private static boolean isFoe(ServerPlayer player, LivingEntity entity) {
        return entity != player && !(entity instanceof ServerPlayer other && PartyService.sameParty(player, other));
    }

    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !MutationService.enabled()) {
            return;
        }
        var food = event.getItem().get(DataComponents.FOOD);
        if (food == null) {
            return;
        }
        var meat = event.getItem().is(ItemTags.MEAT);
        var carnivore = MutationService.positive(player, Mutation.CARNIVORE);
        var herbivore = MutationService.positive(player, Mutation.HERBIVORE);
        if (!carnivore && !herbivore) {
            return;
        }
        var favoured = carnivore == meat;
        var data = player.getFoodData();
        if (favoured) {
            data.eat(food.nutrition(), food.saturation());
        } else if (MutationService.negative(player, carnivore ? Mutation.CARNIVORE : Mutation.HERBIVORE)) {
            data.setFoodLevel(Math.max(0, data.getFoodLevel() - food.nutrition()));
            data.setSaturation(Math.max(0F, data.getSaturationLevel() - food.saturation()));
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        var instance = event.getEffectInstance();
        if (
            reapplying || !(event.getEntity() instanceof ServerPlayer player) || instance.isAmbient() || instance
                .isInfiniteDuration() || !instance.getEffect().value().isBeneficial() || !MutationService.negative(
                    player,
                    Mutation.HEALING_FACTOR
                )
        ) {
            return;
        }
        state(player).toShorten.add(instance.getEffect());
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0F) {
            var rads = SpecialStatResolver.resolve(player).extra(PerkEffectType.RADS_PER_HEAL);
            if (rads > 0F && RadiationService.rads(player) > 0F) {
                RadiationService.remove(player, rads * event.getAmount());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var state = state(player);
        var now = player.level().getGameTime();
        var stats = SpecialStatResolver.resolve(player);

        var decay = stats.extra(PerkEffectType.RAD_DECAY);
        if (decay > 0F && now % 20 == 0 && RadiationService.rads(player) > 0F) {
            RadiationService.remove(player, decay / 60F);
        }
        var sponge = stats.extra(PerkEffectType.RAD_SPONGE);
        if (sponge > 0F && now % 100 == 0) {
            for (var mate : PartyService.onlineMates(player)) {
                if (mate.level() == player.level() && mate.distanceToSqr(player) <= 32 * 32) {
                    RadiationService.remove(mate, sponge);
                }
            }
        }

        if (!MutationService.enabled() || MutationService.get(player).size() == 0) {
            if (state.hidden) {
                setHidden(player, state, false);
            }
            if (now % 20 == 0) {
                applyAttributes(player);
            }
            return;
        }

        var moved = Math.abs(player.getX() - state.lastX) + Math.abs(player.getZ() - state.lastZ) > 0.01;
        state.lastX = player.getX();
        state.lastZ = player.getZ();
        if (moved || state.stillSince == 0) {
            state.stillSince = now;
        }
        tickChameleon(player, state, now);

        var food = (FoodDataAccessor) player.getFoodData();
        var exhaustion = food.atomicdawn$getExhaustionLevel();
        if (
            state.lastExhaustion >= 0F && exhaustion > state.lastExhaustion
                && MutationService.negative(player, Mutation.SPEED_DEMON)
        ) {
            food.atomicdawn$setExhaustionLevel(exhaustion + (exhaustion - state.lastExhaustion) * 0.5F);
        }
        state.lastExhaustion = food.atomicdawn$getExhaustionLevel();

        if (!state.toShorten.isEmpty()) {
            shortenEffects(player, state);
        }

        if (now % 20 != 0) {
            return;
        }
        applyAttributes(player);

        if (
            MutationService.positive(player, Mutation.HEALING_FACTOR) && now % 40 == 0
                && player.getHealth() < player.getMaxHealth() && player.getFoodData().getFoodLevel() > 0
        ) {
            player.heal(1F);
        }
        if (MutationService.positive(player, Mutation.PLAGUE_WALKER) && now % 40 == 0) {
            var area = player.getBoundingBox().inflate(5.0);
            for (var target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e instanceof Enemy)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
            }
        }
    }

    public static boolean chameleonActive(ServerPlayer player) {
        var state = STATES.get(player.getUUID());
        return state != null && state.hidden;
    }

    private static void tickChameleon(ServerPlayer player, State state, long now) {
        var active = MutationService.positive(player, Mutation.CHAMELEON) && now - state.stillSince >= 40;
        if (active) {
            var current = player.getEffect(MobEffects.INVISIBILITY);
            if (current == null || (current.isAmbient() && current.getDuration() < 20)) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, true));
            }

            if (!state.hidden || now % 20 == 0) {
                setHidden(player, state, true);
            }
        } else if (state.hidden) {
            setHidden(player, state, false);
        }
    }

    private static void setHidden(ServerPlayer player, State state, boolean hidden) {
        state.hidden = hidden;
        if (!hidden) {
            var current = player.getEffect(MobEffects.INVISIBILITY);
            if (current != null && current.isAmbient()) {
                player.removeEffect(MobEffects.INVISIBILITY);
            }
        }
        ServerLevel level = player.level();
        level.getChunkSource().sendToTrackingPlayers(player, equipmentPacket(player, hidden));
    }

    private static ClientboundSetEquipmentPacket equipmentPacket(ServerPlayer player, boolean hidden) {
        var slots = new ArrayList<Pair<EquipmentSlot, ItemStack>>();
        for (var slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HAND || slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                slots.add(Pair.of(slot, hidden ? ItemStack.EMPTY : player.getItemBySlot(slot).copy()));
            }
        }
        return new ClientboundSetEquipmentPacket(player.getId(), slots);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (
            event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer
                && chameleonActive(target)
        ) {
            viewer.connection.send(equipmentPacket(target, true));
        }
    }

    private static void shortenEffects(ServerPlayer player, State state) {
        reapplying = true;
        try {
            for (var effect : state.toShorten) {
                var current = player.getEffect(effect);
                if (current == null || current.isAmbient() || current.getDuration() <= 40) {
                    continue;
                }
                var shortened = new MobEffectInstance(
                    effect,
                    Math.max(20, Math.round(current.getDuration() * 0.45F)),
                    current.getAmplifier(),
                    false,
                    current.isVisible(),
                    current.showIcon()
                );
                player.removeEffect(effect);
                player.addEffect(shortened);
            }
        } finally {
            reapplying = false;
            state.toShorten.clear();
        }
    }

    private static void applyAttributes(ServerPlayer player) {
        modifier(
            player,
            Attributes.MAX_HEALTH,
            "adrenal_reaction",
            MutationService.negative(player, Mutation.ADRENAL_REACTION) ? -4.0 : 0.0,
            AttributeModifier.Operation.ADD_VALUE
        );
        var birdBones = MutationService.positive(player, Mutation.BIRD_BONES);
        modifier(
            player,
            Attributes.GRAVITY,
            "bird_bones",
            birdBones ? -0.3 : 0.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        modifier(
            player,
            Attributes.FALL_DAMAGE_MULTIPLIER,
            "bird_bones",
            birdBones ? -0.5 : 0.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        modifier(
            player,
            Attributes.JUMP_STRENGTH,
            "marsupial",
            MutationService.positive(player, Mutation.MARSUPIAL) ? 0.35 : 0.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        modifier(
            player,
            Attributes.MOVEMENT_SPEED,
            "speed_demon",
            MutationService.positive(player, Mutation.SPEED_DEMON) ? 0.2 : 0.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void modifier(
        ServerPlayer player,
        Holder<Attribute> attribute,
        String name,
        double amount,
        AttributeModifier.Operation operation
    ) {
        var instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        var id = AtomicDawn.id("mutation_" + name);
        var existing = instance.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        var rads = event.getOriginal().getData(ADAttachments.RADIATION.get()).rads();
        var kept = rads * (float) (1.0 - AtomicDawn.CONFIG.radiation.RADS_LOST_ON_DEATH);
        if (kept > 0F) {
            RadiationService.set(event.getEntity(), kept);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        var state = STATES.remove(event.getEntity().getUUID());
        if (state != null && state.hidden && event.getEntity() instanceof ServerPlayer player) {
            setHidden(player, state, false);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var player = Commands.argument("player", EntityArgument.player())
            .then(Commands.literal("add").then(mutationArgument(MutationService::add)))
            .then(Commands.literal("remove").then(mutationArgument(MutationService::remove)))
            .then(Commands.literal("clear").executes(context -> {
                MutationService.clear(EntityArgument.getPlayer(context, "player"));
                return 1;
            }))
            .then(Commands.literal("list").executes(context -> {
                var target = EntityArgument.getPlayer(context, "player");
                var text = Component.literal(target.getScoreboardName() + ": ");
                var first = true;
                for (var mutation : MutationService.get(target).all()) {
                    text.append(first ? "" : ", ").append(Component.translatable(mutation.translationKey()));
                    first = false;
                }
                context.getSource().sendSuccess(() -> text, false);
                return 1;
            }));
        event.getDispatcher()
            .register(
                Commands.literal("atomicdawn")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("mutation").then(player))
            );
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> mutationArgument(
        BiConsumer<ServerPlayer, Mutation> action
    ) {
        var names = Arrays.stream(Mutation.values()).map(Mutation::getSerializedName).toList();
        return Commands.argument("mutation", StringArgumentType.word())
            .suggests((_, builder) -> SharedSuggestionProvider.suggest(names, builder))
            .executes(context -> {
                var mutation = parse(StringArgumentType.getString(context, "mutation"));
                if (mutation == null) {
                    return 0;
                }
                action.accept(EntityArgument.getPlayer(context, "player"), mutation);
                return 1;
            });
    }

    private static Mutation parse(String name) {
        try {
            return Mutation.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package mod.azure.atomicdawn.mutation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.party.PartyService;
import mod.azure.atomicdawn.perks.PerkEffectType;
import mod.azure.atomicdawn.perks.PerkHooks;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.special.SpecialAttribute;
import mod.azure.atomicdawn.special.SpecialStatResolver;

public final class MutationService {

    private MutationService() {}

    public static boolean enabled() {
        return AtomicDawn.CONFIG.mutations.MUTATIONS_ENABLED;
    }

    public static Mutations get(Player player) {
        return player.getData(ADAttachments.MUTATIONS.get());
    }

    public static boolean positive(Player player, Mutation mutation) {
        return enabled() && get(player).has(mutation);
    }

    public static boolean negative(Player player, Mutation mutation) {
        var mutations = get(player);
        return enabled() && mutations.has(mutation) && !mutations.suppressed(mutation, player.level().getGameTime());
    }

    private static void set(Player player, Mutations mutations) {
        player.setData(ADAttachments.MUTATIONS.get(), mutations);
        SpecialStatResolver.invalidateAll();
    }

    public static void onExposure(ServerPlayer player, float rads) {
        if (!enabled() || rads <= 0F) {
            return;
        }
        var lock = SpecialStatResolver.resolve(player).extra(PerkEffectType.MUTATION_LOCK_RANDOM);
        var chance = rads * AtomicDawn.CONFIG.mutations.MUTATION_CHANCE_PER_RAD * Math.max(0F, 1F - lock);
        if (player.getRandom().nextDouble() >= chance) {
            return;
        }
        var mutations = get(player);
        if (mutations.size() >= AtomicDawn.CONFIG.mutations.MUTATION_MAX) {
            return;
        }
        var candidates = new ArrayList<Mutation>();
        for (var mutation : Mutation.values()) {
            var exclusive = mutation.exclusive();
            if (!mutations.has(mutation) && (exclusive == null || !mutations.has(exclusive))) {
                candidates.add(mutation);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        var mutation = candidates.get(player.getRandom().nextInt(candidates.size()));
        set(player, mutations.with(mutation, 0L));
        announce(player, "mutation.atomicdawn.gained", mutation, ChatFormatting.GREEN);
    }

    public static boolean applySerum(ServerPlayer player, Mutation mutation) {
        if (!enabled()) {
            return false;
        }
        var mutations = get(player);
        var exclusive = mutation.exclusive();
        var replacing = exclusive != null && mutations.has(exclusive);
        if (!mutations.has(mutation) && !replacing && mutations.size() >= AtomicDawn.CONFIG.mutations.MUTATION_MAX) {
            player.sendSystemMessage(
                Component.translatable("mutation.atomicdawn.full", AtomicDawn.CONFIG.mutations.MUTATION_MAX),
                true
            );
            return false;
        }
        if (replacing) {
            mutations = mutations.without(exclusive);
            announce(player, "mutation.atomicdawn.lost", exclusive, ChatFormatting.GRAY);
        }
        var gained = !mutations.has(mutation);
        var until = player.level().getGameTime() + AtomicDawn.CONFIG.mutations.MUTATION_SERUM_MINUTES * 1200L;
        set(player, mutations.with(mutation, until));
        announce(
            player,
            gained ? "mutation.atomicdawn.gained_serum" : "mutation.atomicdawn.suppressed",
            mutation,
            ChatFormatting.GREEN
        );
        return true;
    }

    public static void onRadAway(ServerPlayer player) {
        var mutations = get(player);
        if (!enabled() || mutations.size() == 0) {
            return;
        }
        var lock = SpecialStatResolver.resolve(player).extra(PerkEffectType.MUTATION_LOCK_REMOVAL);
        var chance = AtomicDawn.CONFIG.mutations.RADAWAY_MUTATION_CURE_CHANCE * Math.max(0F, 1F - lock);
        if (player.getRandom().nextDouble() >= chance) {
            return;
        }
        var owned = new ArrayList<>(mutations.all());
        var mutation = owned.get(player.getRandom().nextInt(owned.size()));
        set(player, mutations.without(mutation));
        announce(player, "mutation.atomicdawn.lost", mutation, ChatFormatting.GRAY);
    }

    public static void add(ServerPlayer player, Mutation mutation) {
        var mutations = get(player);
        var exclusive = mutation.exclusive();
        if (exclusive != null) {
            mutations = mutations.without(exclusive);
        }
        set(player, mutations.with(mutation, 0L));
    }

    public static void remove(ServerPlayer player, Mutation mutation) {
        set(player, get(player).without(mutation));
    }

    public static void clear(ServerPlayer player) {
        set(player, Mutations.NONE);
    }

    public static int specialBonus(Player player, SpecialAttribute attribute) {
        if (!enabled() || get(player).size() == 0) {
            return 0;
        }
        var gained = 0;
        var lost = 0;
        switch (attribute) {
            case STRENGTH -> {
                if (negative(player, Mutation.BIRD_BONES)) {
                    lost += 4;
                }
                if (negative(player, Mutation.EAGLE_EYES)) {
                    lost += 4;
                }
                if (negative(player, Mutation.EGG_HEAD)) {
                    lost += 3;
                }
            }
            case PERCEPTION -> {
                if (positive(player, Mutation.EAGLE_EYES)) {
                    gained += 4;
                }
            }
            case ENDURANCE -> {
                if (negative(player, Mutation.EGG_HEAD)) {
                    lost += 3;
                }
            }
            case INTELLIGENCE -> {
                if (positive(player, Mutation.EGG_HEAD)) {
                    gained += 6;
                }
                if (negative(player, Mutation.MARSUPIAL)) {
                    lost += 4;
                }
            }
            case AGILITY -> {
                if (positive(player, Mutation.BIRD_BONES)) {
                    gained += 4;
                }
                if (negative(player, Mutation.TALONS)) {
                    lost += 4;
                }
            }
            default -> {}
        }
        if (positive(player, Mutation.HERD_MENTALITY)) {
            if (inParty(player)) {
                gained += 2;
            } else if (negative(player, Mutation.HERD_MENTALITY)) {
                lost += 2;
            }
        }
        return Math.round(gained * positiveScale(player)) - Math.round(lost * negativeScale(player));
    }

    public static float positiveScale(Player player) {
        return inParty(player) ? 1F + PerkHooks.strangeInNumbers(player) : 1F;
    }

    public static float negativeScale(Player player) {
        return 1F - PerkHooks.classFreak(player);
    }

    public static boolean inParty(Player player) {
        return player instanceof ServerPlayer serverPlayer
            ? !PartyService.onlineMates(serverPlayer).isEmpty()
            : PartyService.clientHasOnlineMates;
    }

    public static float maxApBonus(Player player) {
        return negative(player, Mutation.SCALY_SKIN) ? -25F * negativeScale(player) : 0F;
    }

    public static float criticalDamageBonus(Player player) {
        return positive(player, Mutation.EAGLE_EYES) ? 0.25F * positiveScale(player) : 0F;
    }

    public static float spreadMultiplier(Player player) {
        return negative(player, Mutation.TWISTED_MUSCLES) ? 1F + 0.5F * negativeScale(player) : 1F;
    }

    private static void announce(ServerPlayer player, String key, Mutation mutation, ChatFormatting color) {
        player.sendSystemMessage(
            Component.translatable(key, Component.translatable(mutation.translationKey())).withStyle(color)
        );
        player.level()
            .playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.4F, 1.6F);
    }
}

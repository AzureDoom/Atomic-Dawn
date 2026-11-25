package mod.azure.atomicdawn.weapon.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import mod.azure.atomicdawn.registry.ADDamageTypes;
import mod.azure.atomicdawn.registry.ADRegistries;
import mod.azure.atomicdawn.weapon.modification.ModSlot;
import mod.azure.atomicdawn.weapon.modification.WeaponModification;

public record WeaponDefinition(
    WeaponCategory category,
    Damage damage,
    Firing firing,
    Accuracy accuracy,
    Recoil recoil,
    Ammo ammo,
    Reload reload,
    int drawTicks,
    Sounds sounds,
    Modifications modifications,
    Optional<Melee> melee,
    Optional<Tracer> tracer,
    Condition condition,
    Optional<Crafting> crafting,
    Vats vats,
    Optional<Projectile> projectile
) {

    public static final Codec<WeaponDefinition> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            WeaponCategory.CODEC.fieldOf("category").forGetter(WeaponDefinition::category),
            Damage.CODEC.fieldOf("damage").forGetter(WeaponDefinition::damage),
            Firing.CODEC.fieldOf("firing").forGetter(WeaponDefinition::firing),
            Accuracy.CODEC.optionalFieldOf("accuracy", Accuracy.DEFAULT).forGetter(WeaponDefinition::accuracy),
            Recoil.CODEC.optionalFieldOf("recoil", Recoil.DEFAULT).forGetter(WeaponDefinition::recoil),
            Ammo.CODEC.optionalFieldOf("ammo", Ammo.NONE).forGetter(WeaponDefinition::ammo),
            Reload.CODEC.optionalFieldOf("reload", Reload.NONE).forGetter(WeaponDefinition::reload),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("draw_ticks", 10).forGetter(WeaponDefinition::drawTicks),
            Sounds.CODEC.fieldOf("sounds").forGetter(WeaponDefinition::sounds),
            Modifications.CODEC.optionalFieldOf("modifications", Modifications.NONE)
                .forGetter(WeaponDefinition::modifications),
            Melee.CODEC.optionalFieldOf("melee").forGetter(WeaponDefinition::melee),
            Tracer.CODEC.optionalFieldOf("tracer").forGetter(WeaponDefinition::tracer),
            Condition.CODEC.optionalFieldOf("condition", Condition.NONE).forGetter(WeaponDefinition::condition),
            Crafting.CODEC.optionalFieldOf("crafting").forGetter(WeaponDefinition::crafting),
            Vats.CODEC.optionalFieldOf("vats", Vats.DEFAULT).forGetter(WeaponDefinition::vats),
            Projectile.CODEC.optionalFieldOf("projectile").forGetter(WeaponDefinition::projectile)
        ).apply(instance, WeaponDefinition::new)
    );

    public boolean usesAmmo() {
        return ammo.usesAmmo();
    }

    public boolean isMelee() {
        return melee.isPresent();
    }

    public boolean isProjectile() {
        return projectile.isPresent();
    }

    public record Projectile(
        float speed,
        float gravity,
        float blastRadius,
        float explosionPower,
        boolean breakBlocks,
        boolean nuclear,
        int fuseTicks,
        float flatRange
    ) {

        public static final Codec<Projectile> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.floatRange(0.05F, 10F).optionalFieldOf("speed", 1.5F).forGetter(Projectile::speed),
                Codec.floatRange(0F, 1F).optionalFieldOf("gravity", 0.03F).forGetter(Projectile::gravity),
                Codec.floatRange(0F, 64F).optionalFieldOf("blast_radius", 6F).forGetter(Projectile::blastRadius),
                Codec.floatRange(0F, 32F).optionalFieldOf("explosion_power", 3F).forGetter(Projectile::explosionPower),
                Codec.BOOL.optionalFieldOf("break_blocks", true).forGetter(Projectile::breakBlocks),
                Codec.BOOL.optionalFieldOf("nuclear", false).forGetter(Projectile::nuclear),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("fuse_ticks", 200).forGetter(Projectile::fuseTicks),
                Codec.floatRange(0F, 512F).optionalFieldOf("flat_range", 0F).forGetter(Projectile::flatRange)
            ).apply(instance, Projectile::new)
        );
    }

    public record Damage(
        float base,
        int pellets,
        float headshotMultiplier,
        float falloffStart,
        float range,
        float minFalloffMultiplier,
        ResourceKey<DamageType> type
    ) {

        public static final Codec<Damage> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("base").forGetter(Damage::base),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("pellets", 1).forGetter(Damage::pellets),
                ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("headshot_multiplier", 1.5F)
                    .forGetter(Damage::headshotMultiplier),
                Codec.floatRange(0, 512).optionalFieldOf("falloff_start", 24F).forGetter(Damage::falloffStart),
                Codec.floatRange(1, 512).optionalFieldOf("range", 96F).forGetter(Damage::range),
                Codec.floatRange(0, 1)
                    .optionalFieldOf("min_falloff_multiplier", 0.5F)
                    .forGetter(Damage::minFalloffMultiplier),
                ResourceKey.codec(Registries.DAMAGE_TYPE)
                    .optionalFieldOf("type", ADDamageTypes.BALLISTIC)
                    .forGetter(Damage::type)
            ).apply(instance, Damage::new)
        );

        public float falloffMultiplier(double distance) {
            if (distance <= falloffStart) {
                return 1F;
            }
            if (distance >= range || range <= falloffStart) {
                return minFalloffMultiplier;
            }
            var t = (float) ((distance - falloffStart) / (range - falloffStart));
            return 1F + (minFalloffMultiplier - 1F) * t;
        }
    }

    public record Firing(
        List<FireMode> modes,
        float intervalTicks,
        Burst burst,
        Charge charge,
        int spinUpTicks
    ) {

        public static final Codec<Firing> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ExtraCodecs.nonEmptyList(FireMode.CODEC.listOf()).fieldOf("modes").forGetter(Firing::modes),
                Codec.floatRange(1, 200).fieldOf("interval_ticks").forGetter(Firing::intervalTicks),
                Burst.CODEC.optionalFieldOf("burst", Burst.DEFAULT).forGetter(Firing::burst),
                Charge.CODEC.optionalFieldOf("charge", Charge.DEFAULT).forGetter(Firing::charge),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("spin_up_ticks", 20).forGetter(Firing::spinUpTicks)
            ).apply(instance, Firing::new)
        );

        public FireMode defaultMode() {
            return modes.getFirst();
        }

        public FireMode next(FireMode current) {
            var index = modes.indexOf(current);
            return modes.get((index + 1) % modes.size());
        }

        public FireMode sanitize(FireMode mode) {
            return modes.contains(mode) ? mode : defaultMode();
        }
    }

    public record Burst(
        int count,
        float cooldownTicks
    ) {

        public static final Burst DEFAULT = new Burst(3, 8F);

        public static final Codec<Burst> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.intRange(2, 16).optionalFieldOf("count", 3).forGetter(Burst::count),
                Codec.floatRange(0, 200).optionalFieldOf("cooldown_ticks", 8F).forGetter(Burst::cooldownTicks)
            ).apply(instance, Burst::new)
        );
    }

    public record Charge(
        int ticks,
        float minCharge,
        float minDamage,
        float maxDamage
    ) {

        public static final Charge DEFAULT = new Charge(20, 0.2F, 0.5F, 2.0F);

        public static final Codec<Charge> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("ticks", 20).forGetter(Charge::ticks),
                Codec.floatRange(0, 1).optionalFieldOf("min_charge", 0.2F).forGetter(Charge::minCharge),
                Codec.floatRange(0, 10).optionalFieldOf("min_damage", 0.5F).forGetter(Charge::minDamage),
                Codec.floatRange(0, 10).optionalFieldOf("max_damage", 2.0F).forGetter(Charge::maxDamage)
            ).apply(instance, Charge::new)
        );

        public float damageFor(float fraction) {
            if (fraction < minCharge) {
                return -1F;
            }
            var t = minCharge >= 1F ? 1F : (fraction - minCharge) / (1F - minCharge);
            return minDamage + (maxDamage - minDamage) * Math.min(1F, t);
        }
    }

    public record Accuracy(
        float baseSpread,
        float adsMultiplier,
        float crouchMultiplier,
        float movingMultiplier,
        float airborneMultiplier,
        float bloomPerShot,
        float maxBloom,
        float bloomRecoveryPerTick
    ) {

        public static final Accuracy DEFAULT = new Accuracy(2.5F, 0.35F, 0.8F, 1.6F, 2.5F, 0.6F, 4F, 0.25F);

        public static final Codec<Accuracy> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.floatRange(0, 45)
                    .optionalFieldOf("base_spread", DEFAULT.baseSpread)
                    .forGetter(Accuracy::baseSpread),
                Codec.floatRange(0, 4)
                    .optionalFieldOf("ads_multiplier", DEFAULT.adsMultiplier)
                    .forGetter(Accuracy::adsMultiplier),
                Codec.floatRange(0, 4)
                    .optionalFieldOf("crouch_multiplier", DEFAULT.crouchMultiplier)
                    .forGetter(Accuracy::crouchMultiplier),
                Codec.floatRange(0, 8)
                    .optionalFieldOf("moving_multiplier", DEFAULT.movingMultiplier)
                    .forGetter(Accuracy::movingMultiplier),
                Codec.floatRange(0, 8)
                    .optionalFieldOf("airborne_multiplier", DEFAULT.airborneMultiplier)
                    .forGetter(Accuracy::airborneMultiplier),
                Codec.floatRange(0, 45)
                    .optionalFieldOf("bloom_per_shot", DEFAULT.bloomPerShot)
                    .forGetter(Accuracy::bloomPerShot),
                Codec.floatRange(0, 45).optionalFieldOf("max_bloom", DEFAULT.maxBloom).forGetter(Accuracy::maxBloom),
                Codec.floatRange(0, 45)
                    .optionalFieldOf("bloom_recovery_per_tick", DEFAULT.bloomRecoveryPerTick)
                    .forGetter(Accuracy::bloomRecoveryPerTick)
            ).apply(instance, Accuracy::new)
        );
    }

    public record Recoil(
        float vertical,
        float horizontal,
        float adsMultiplier
    ) {

        public static final Recoil DEFAULT = new Recoil(2F, 0.6F, 0.7F);

        public static final Codec<Recoil> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.floatRange(0, 45).optionalFieldOf("vertical", DEFAULT.vertical).forGetter(Recoil::vertical),
                Codec.floatRange(0, 45)
                    .optionalFieldOf("horizontal", DEFAULT.horizontal)
                    .forGetter(Recoil::horizontal),
                Codec.floatRange(0, 4)
                    .optionalFieldOf("ads_multiplier", DEFAULT.adsMultiplier)
                    .forGetter(Recoil::adsMultiplier)
            ).apply(instance, Recoil::new)
        );
    }

    public record Ammo(
        Item item,
        int magazineSize,
        int perShot
    ) {

        public static final Ammo NONE = new Ammo(Items.AIR, 0, 0);

        public static final Codec<Ammo> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Ammo::item),
                ExtraCodecs.POSITIVE_INT.fieldOf("magazine_size").forGetter(Ammo::magazineSize),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("per_shot", 1).forGetter(Ammo::perShot)
            ).apply(instance, Ammo::new)
        );

        public boolean usesAmmo() {
            return item != Items.AIR && magazineSize > 0;
        }
    }

    public record Reload(
        ReloadType type,
        int ticks,
        int emptyTicks,
        int perRoundTicks,
        int endTicks
    ) {

        public static final Reload NONE = new Reload(ReloadType.MAGAZINE, 1, 1, 1, 0);

        public static final Codec<Reload> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ReloadType.CODEC.optionalFieldOf("type", ReloadType.MAGAZINE).forGetter(Reload::type),
                ExtraCodecs.POSITIVE_INT.fieldOf("ticks").forGetter(Reload::ticks),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("empty_ticks", 0).forGetter(Reload::emptyTicks),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("per_round_ticks", 10).forGetter(Reload::perRoundTicks),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("end_ticks", 8).forGetter(Reload::endTicks)
            ).apply(instance, Reload::new)
        );

        public int ticksFor(boolean empty) {
            return empty && emptyTicks > 0 ? emptyTicks : ticks;
        }

        public int estimateTicks(int roundsNeeded, boolean empty) {
            return type.perRound() ? ticks + perRoundTicks * Math.max(1, roundsNeeded) + endTicks : ticksFor(empty);
        }
    }

    public record Melee(
        float reach,
        float knockback,
        float sweepRadius,
        float sweepDamageMultiplier
    ) {

        public static final Codec<Melee> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.floatRange(0.5F, 8F).optionalFieldOf("reach", 3.5F).forGetter(Melee::reach),
                Codec.floatRange(0F, 10F).optionalFieldOf("knockback", 0.5F).forGetter(Melee::knockback),
                Codec.floatRange(0F, 6F).optionalFieldOf("sweep_radius", 0F).forGetter(Melee::sweepRadius),
                Codec.floatRange(0F, 1F)
                    .optionalFieldOf("sweep_damage_multiplier", 0.5F)
                    .forGetter(Melee::sweepDamageMultiplier)
            ).apply(instance, Melee::new)
        );
    }

    public record Tracer(
        int color,
        float size
    ) {

        private static final Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(
            text -> {
                try {
                    return DataResult.success(Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16));
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Not a hex colour: " + text);
                }
            },
            color -> String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF)
        );

        public static final Codec<Tracer> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                HEX_COLOR.fieldOf("color").forGetter(Tracer::color),
                Codec.floatRange(0.1F, 4F).optionalFieldOf("size", 0.6F).forGetter(Tracer::size)
            ).apply(instance, Tracer::new)
        );
    }

    public record Condition(
        int max,
        float lossPerUse,
        List<SizedIngredient> repairCost
    ) {

        public static final Condition NONE = new Condition(0, 0F, List.of());

        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max").forGetter(Condition::max),
                Codec.floatRange(0, 1000).optionalFieldOf("loss_per_use", 1F).forGetter(Condition::lossPerUse),
                SizedIngredient.NESTED_CODEC.listOf()
                    .optionalFieldOf("repair_cost", List.of())
                    .forGetter(Condition::repairCost)
            ).apply(instance, Condition::new)
        );

        public boolean enabled() {
            return max > 0;
        }
    }

    public record Vats(
        float apCost,
        float critMultiplier,
        float hitChanceMultiplier
    ) {

        public static final Vats DEFAULT = new Vats(25F, 2F, 1F);

        public static final Codec<Vats> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.floatRange(0, 200).optionalFieldOf("ap_cost", DEFAULT.apCost).forGetter(Vats::apCost),
                Codec.floatRange(1, 20)
                    .optionalFieldOf("crit_multiplier", DEFAULT.critMultiplier)
                    .forGetter(Vats::critMultiplier),
                Codec.floatRange(0, 4)
                    .optionalFieldOf("hit_chance_multiplier", DEFAULT.hitChanceMultiplier)
                    .forGetter(Vats::hitChanceMultiplier)
            ).apply(instance, Vats::new)
        );
    }

    public record Crafting(
        List<SizedIngredient> cost,
        List<ScrapEntry> scrap,
        int scrapExperience
    ) {

        public static final Codec<Crafting> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                SizedIngredient.NESTED_CODEC.listOf().optionalFieldOf("cost", List.of()).forGetter(Crafting::cost),
                ScrapEntry.CODEC.listOf().optionalFieldOf("scrap", List.of()).forGetter(Crafting::scrap),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("scrap_experience", 5).forGetter(Crafting::scrapExperience)
            ).apply(instance, Crafting::new)
        );
    }

    public record ScrapEntry(
        Item item,
        int count
    ) {

        public static final Codec<ScrapEntry> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ScrapEntry::item),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(ScrapEntry::count)
            ).apply(instance, ScrapEntry::new)
        );
    }

    public record Sounds(
        Holder<SoundEvent> fire,
        Optional<Holder<SoundEvent>> dryFire,
        Optional<Holder<SoundEvent>> reload,
        Optional<Holder<SoundEvent>> draw,
        Optional<Holder<SoundEvent>> reloadInsert,
        Optional<Holder<SoundEvent>> impact
    ) {

        public static final Codec<Sounds> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                SoundEvent.CODEC.fieldOf("fire").forGetter(Sounds::fire),
                SoundEvent.CODEC.optionalFieldOf("dry_fire").forGetter(Sounds::dryFire),
                SoundEvent.CODEC.optionalFieldOf("reload").forGetter(Sounds::reload),
                SoundEvent.CODEC.optionalFieldOf("draw").forGetter(Sounds::draw),
                SoundEvent.CODEC.optionalFieldOf("reload_insert").forGetter(Sounds::reloadInsert),
                SoundEvent.CODEC.optionalFieldOf("impact").forGetter(Sounds::impact)
            ).apply(instance, Sounds::new)
        );
    }

    public record Modifications(
        List<ModSlot> slots,
        Map<ModSlot, ResourceKey<WeaponModification>> defaults
    ) {

        public static final Modifications NONE = new Modifications(List.of(), Map.of());

        public static final Codec<Modifications> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ModSlot.CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(Modifications::slots),
                Codec.unboundedMap(ModSlot.CODEC, ResourceKey.codec(ADRegistries.WEAPON_MODIFICATION))
                    .optionalFieldOf("defaults", Map.of())
                    .forGetter(Modifications::defaults)
            ).apply(instance, Modifications::new)
        );
    }
}

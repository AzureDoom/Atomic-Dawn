package mod.azure.atomicdawn.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.armor.upgrade.ArmorUpgrades;
import mod.azure.atomicdawn.camp.CampNbt;
import mod.azure.atomicdawn.powerarmor.upgrade.PowerArmorUpgrades;
import mod.azure.atomicdawn.weapon.instance.WeaponCondition;
import mod.azure.atomicdawn.weapon.instance.WeaponState;
import mod.azure.atomicdawn.weapon.legendary.WeaponLegendary;
import mod.azure.atomicdawn.weapon.modification.WeaponMods;

public final class ADDataComponents {

    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(
        Registries.DATA_COMPONENT_TYPE,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponState>> WEAPON_STATE = REGISTER
        .registerComponentType(
            "weapon_state",
            builder -> builder.persistent(WeaponState.CODEC)
                .networkSynchronized(WeaponState.STREAM_CODEC)
                .ignoreSwapAnimation()
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponMods>> WEAPON_MODS = REGISTER
        .registerComponentType(
            "weapon_mods",
            builder -> builder.persistent(WeaponMods.CODEC)
                .networkSynchronized(WeaponMods.STREAM_CODEC)
                .ignoreSwapAnimation()
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponCondition>> WEAPON_CONDITION =
        REGISTER.registerComponentType(
            "weapon_condition",
            builder -> builder.persistent(WeaponCondition.CODEC)
                .networkSynchronized(WeaponCondition.STREAM_CODEC)
                .ignoreSwapAnimation()
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponLegendary>> LEGENDARY = REGISTER
        .registerComponentType(
            "legendary",
            builder -> builder.persistent(WeaponLegendary.CODEC).networkSynchronized(WeaponLegendary.STREAM_CODEC)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMMO_REMAINING = REGISTER
        .registerComponentType(
            "ammo_remaining",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> HOLOTAPE = REGISTER
        .registerComponentType(
            "holotape",
            builder -> builder.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> CAMP_PLAN = REGISTER
        .registerComponentType(
            "camp_plan",
            builder -> builder.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CORE_CHARGE = REGISTER
        .registerComponentType(
            "core_charge",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> POWER_ARMOR_PAINT = REGISTER
        .registerComponentType(
            "power_armor_paint",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PowerArmorUpgrades>> POWER_ARMOR_UPGRADES =
        REGISTER.registerComponentType(
            "power_armor_upgrades",
            builder -> builder.persistent(PowerArmorUpgrades.CODEC).networkSynchronized(PowerArmorUpgrades.STREAM_CODEC)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArmorUpgrades>> ARMOR_UPGRADES =
        REGISTER.registerComponentType(
            "armor_upgrades",
            builder -> builder.persistent(ArmorUpgrades.CODEC).networkSynchronized(ArmorUpgrades.STREAM_CODEC)
        );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> CAMP_BLOCK_DATA =
        REGISTER.registerComponentType(
            "camp_block_data",
            builder -> builder.persistent(CampNbt.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
        );

    private ADDataComponents() {}
}

package mod.azure.atomicdawn.armor;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;

import mod.azure.atomicdawn.AtomicDawn;

@SuppressWarnings("unused")
public enum ArmorSet implements StringRepresentable {

    SECRET_SERVICE(ArmorWeight.HEAVY, Family.WELDED, new int[] { 3, 8, 7, 3 }, 3F, 0.1F, 35, 14, Sound.NETHERITE),
    HEAVY_ROBOT(ArmorWeight.HEAVY, Family.METAL, new int[] { 3, 8, 6, 3 }, 2F, 0.1F, 25, 9, Sound.IRON),
    HEAVY_RAIDER(ArmorWeight.HEAVY, Family.WELDED, new int[] { 3, 7, 6, 2 }, 1F, 0F, 20, 9, Sound.CHAIN),
    HEAVY_METAL(ArmorWeight.HEAVY, Family.METAL, new int[] { 3, 7, 6, 3 }, 1.5F, 0.05F, 22, 9, Sound.IRON),
    HEAVY_LEATHER(ArmorWeight.HEAVY, Family.LEATHER, new int[] { 3, 7, 5, 2 }, 1F, 0F, 18, 15, Sound.LEATHER),
    HEAVY_COMBAT(ArmorWeight.HEAVY, Family.COMBAT, new int[] { 3, 8, 6, 3 }, 1.5F, 0.05F, 24, 10, Sound.CHAIN),
    BROTHERHOOD_RECON(ArmorWeight.HEAVY, Family.WELDED, new int[] { 3, 8, 6, 3 }, 2.5F, 0.05F, 30, 12, Sound.NETHERITE),
    BOTSMITH(ArmorWeight.HEAVY, Family.METAL, new int[] { 3, 8, 6, 3 }, 2F, 0.1F, 28, 10, Sound.NETHERITE),

    TRAPPER(ArmorWeight.STURDY, Family.WELDED, new int[] { 2, 6, 5, 2 }, 1F, 0F, 17, 11, Sound.CHAIN),
    STURDY_ROBOT(ArmorWeight.STURDY, Family.METAL, new int[] { 2, 6, 5, 2 }, 1F, 0.05F, 17, 9, Sound.IRON),
    STURDY_RAIDER(ArmorWeight.STURDY, Family.WELDED, new int[] { 2, 5, 5, 2 }, 0.5F, 0F, 14, 9, Sound.CHAIN),
    STURDY_METAL(ArmorWeight.STURDY, Family.METAL, new int[] { 2, 6, 5, 2 }, 0.5F, 0F, 15, 9, Sound.IRON),
    MARINE(ArmorWeight.STURDY, Family.WELDED, new int[] { 3, 6, 5, 2 }, 1.5F, 0F, 20, 12, Sound.IRON),
    STURDY_LEATHER(ArmorWeight.STURDY, Family.LEATHER, new int[] { 2, 5, 4, 2 }, 0.5F, 0F, 13, 15, Sound.LEATHER),
    STURDY_COMBAT(ArmorWeight.STURDY, Family.COMBAT, new int[] { 2, 6, 5, 2 }, 1F, 0F, 16, 10, Sound.CHAIN),
    CIVIL_ENGINEER(ArmorWeight.STURDY, Family.WELDED, new int[] { 2, 6, 5, 2 }, 1F, 0F, 18, 11, Sound.IRON),
    ARCTIC_MARINE(ArmorWeight.STURDY, Family.WELDED, new int[] { 3, 6, 5, 2 }, 1.5F, 0F, 20, 12, Sound.IRON),

    WOOD(ArmorWeight.LIGHT, Family.WOOD, new int[] { 1, 2, 2, 1 }, 0F, 0F, 6, 12, Sound.LEATHER),
    URBAN_SCOUT(ArmorWeight.LIGHT, Family.POLYMER, new int[] { 2, 4, 3, 2 }, 0.5F, 0F, 14, 12, Sound.LEATHER),
    THORN(ArmorWeight.LIGHT, Family.POLYMER, new int[] { 2, 4, 3, 2 }, 0.5F, 0F, 14, 12, Sound.LEATHER),
    SOLAR(ArmorWeight.LIGHT, Family.POLYMER, new int[] { 2, 4, 3, 2 }, 0.5F, 0F, 15, 13, Sound.LEATHER),
    LIGHT_ROBOT(ArmorWeight.LIGHT, Family.METAL, new int[] { 2, 4, 3, 1 }, 0.5F, 0F, 12, 9, Sound.IRON),
    LIGHT_RAIDER(ArmorWeight.LIGHT, Family.WELDED, new int[] { 1, 3, 3, 1 }, 0F, 0F, 9, 9, Sound.CHAIN),
    LIGHT_METAL(ArmorWeight.LIGHT, Family.METAL, new int[] { 1, 4, 3, 1 }, 0F, 0F, 11, 9, Sound.IRON),
    LIGHT_LEATHER(ArmorWeight.LIGHT, Family.LEATHER, new int[] { 1, 3, 2, 1 }, 0F, 0F, 8, 15, Sound.LEATHER),
    LIGHT_COMBAT(ArmorWeight.LIGHT, Family.COMBAT, new int[] { 2, 4, 3, 1 }, 0F, 0F, 12, 10, Sound.CHAIN),
    COVERT_SCOUT(ArmorWeight.LIGHT, Family.POLYMER, new int[] { 2, 4, 4, 2 }, 1F, 0F, 16, 14, Sound.LEATHER),
    FOREST_SCOUT(ArmorWeight.LIGHT, Family.POLYMER, new int[] { 2, 4, 3, 2 }, 0.5F, 0F, 14, 12, Sound.LEATHER);

    public static final Codec<ArmorSet> CODEC = StringRepresentable.fromEnum(ArmorSet::values);

    public enum Family implements StringRepresentable {

        WELDED,
        METAL,
        COMBAT,
        POLYMER,
        LEATHER,
        WOOD;

        private final TagKey<Item> repairTag = TagKey.create(
            Registries.ITEM,
            AtomicDawn.id("armor_repair/" + getSerializedName())
        );

        public TagKey<Item> repairTag() {
            return repairTag;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private enum Sound {

        LEATHER,
        CHAIN,
        IRON,
        NETHERITE;

        Holder<SoundEvent> holder() {
            return switch (this) {
                case LEATHER -> SoundEvents.ARMOR_EQUIP_LEATHER;
                case CHAIN -> SoundEvents.ARMOR_EQUIP_CHAIN;
                case IRON -> SoundEvents.ARMOR_EQUIP_IRON;
                case NETHERITE -> SoundEvents.ARMOR_EQUIP_NETHERITE;
            };
        }
    }

    private final ArmorWeight weight;

    private final Family family;

    private final int[] defense;

    private final float toughness;

    private final float knockbackResistance;

    private final int durability;

    private final int enchantability;

    private final Sound sound;

    private final ResourceKey<EquipmentAsset> asset;

    @Nullable
    private ArmorMaterial material;

    ArmorSet(
        ArmorWeight weight,
        Family family,
        int[] defense,
        float toughness,
        float knockbackResistance,
        int durability,
        int enchantability,
        Sound sound
    ) {
        this.weight = weight;
        this.family = family;
        this.defense = defense;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.durability = durability;
        this.enchantability = enchantability;
        this.sound = sound;
        this.asset = ResourceKey.create(EquipmentAssets.ROOT_ID, AtomicDawn.id(getSerializedName()));
    }

    public ArmorWeight weight() {
        return weight;
    }

    public Family family() {
        return family;
    }

    public ResourceKey<EquipmentAsset> asset() {
        return asset;
    }

    public int defense(ArmorPiece piece) {
        return defense[piece.ordinal()];
    }

    public String itemName(ArmorPiece piece) {
        return getSerializedName() + "_" + piece.getSerializedName();
    }

    public String translationKey() {
        return "armor.atomicdawn.set." + getSerializedName();
    }

    public boolean matches(String filter) {
        if (filter.startsWith("#")) {
            return family.getSerializedName().equals(filter.substring(1));
        }
        return getSerializedName().equals(filter);
    }

    public ArmorMaterial material() {
        if (material == null) {
            var map = new EnumMap<ArmorType, Integer>(ArmorType.class);
            for (var piece : ArmorPiece.all()) {
                map.put(piece.type(), defense(piece));
            }
            map.put(ArmorType.BODY, defense[ArmorPiece.CHESTPLATE.ordinal()]);
            material = new ArmorMaterial(
                durability,
                map,
                enchantability,
                sound.holder(),
                toughness,
                knockbackResistance,
                family.repairTag(),
                asset
            );
        }
        return material;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

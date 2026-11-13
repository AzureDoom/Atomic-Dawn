package mod.azure.atomicdawn.mutation;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Mutation implements StringRepresentable {

    ADRENAL_REACTION(0xE0402A),
    BIRD_BONES(0xE8E0C8),
    CARNIVORE(0xB0302A),
    CHAMELEON(0x7CC8A0),
    EAGLE_EYES(0xE8B83A),
    EGG_HEAD(0xF0E8A0),
    ELECTRICALLY_CHARGED(0x6AD0FF),
    EMPATH(0xF080B0),
    GROUNDED(0x8A6A3A),
    HEALING_FACTOR(0x60E060),
    HERBIVORE(0x5AA83A),
    HERD_MENTALITY(0xC08A50),
    MARSUPIAL(0xD8A070),
    PLAGUE_WALKER(0x8AA020),
    SCALY_SKIN(0x3A8A70),
    SPEED_DEMON(0xFF8A20),
    TALONS(0x9A9A9A),
    TWISTED_MUSCLES(0xC04060),
    UNSTABLE_ISOTOPE(0x9AFF3A);

    public static final Codec<Mutation> CODEC = StringRepresentable.fromEnum(Mutation::values);

    private final int color;

    Mutation(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    @Nullable
    public Mutation exclusive() {
        return switch (this) {
            case CARNIVORE -> HERBIVORE;
            case HERBIVORE -> CARNIVORE;
            default -> null;
        };
    }

    public String translationKey() {
        return "mutation.atomicdawn." + getSerializedName();
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Mutation byIndex(int index) {
        var values = values();
        return values[Math.floorMod(index, values.length)];
    }
}

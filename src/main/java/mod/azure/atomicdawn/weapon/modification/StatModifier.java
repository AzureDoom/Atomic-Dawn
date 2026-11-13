package mod.azure.atomicdawn.weapon.modification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record StatModifier(
    WeaponStat stat,
    Operation operation,
    float amount
) {

    public static final Codec<StatModifier> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            WeaponStat.CODEC.fieldOf("stat").forGetter(StatModifier::stat),
            Operation.CODEC.optionalFieldOf("operation", Operation.ADD_PERCENT).forGetter(StatModifier::operation),
            Codec.FLOAT.fieldOf("amount").forGetter(StatModifier::amount)
        ).apply(instance, StatModifier::new)
    );

    public static float apply(WeaponStat stat, float base, List<StatModifier> modifiers) {
        var add = 0F;
        var addPercent = 0F;
        var multiply = 1F;
        for (var modifier : modifiers) {
            if (modifier.stat != stat) {
                continue;
            }
            switch (modifier.operation) {
                case ADD -> add += modifier.amount;
                case ADD_PERCENT -> addPercent += modifier.amount;
                case MULTIPLY -> multiply *= 1F + modifier.amount;
            }
        }
        return (base + add) * (1F + addPercent) * multiply;
    }

    public enum Operation implements StringRepresentable {

        ADD("add"),
        ADD_PERCENT("add_percent"),
        MULTIPLY("multiply");

        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);

        private final String serializedName;

        Operation(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return serializedName;
        }
    }
}

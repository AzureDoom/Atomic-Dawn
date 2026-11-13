package mod.azure.atomicdawn.ghoul;

import java.util.function.BooleanSupplier;

public record TransformationContext(
    boolean admin,
    BooleanSupplier requirement
) {

    public static TransformationContext operator() {
        return new TransformationContext(true, () -> true);
    }

    public static TransformationContext quest() {
        return new TransformationContext(false, () -> true);
    }

    public static TransformationContext treatment(BooleanSupplier consume) {
        return new TransformationContext(false, consume);
    }
}

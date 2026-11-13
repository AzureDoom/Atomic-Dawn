package mod.azure.atomicdawn.util;

public class MixinUtil {

    @SuppressWarnings("unchecked")
    public static <T> T self(Object object) {
        return (T) object;
    }
}

package mod.azure.atomicdawn.helper;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ADRegistryUtil {

    private ADRegistryUtil() {}

    public static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier vanillaId(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    @Nullable
    public static Identifier tryParse(String value) {
        return Identifier.tryParse(value);
    }

    public static <T> Registry<T> registry(RegistryAccess access, ResourceKey<? extends Registry<? extends T>> key) {
        return access.lookupOrThrow(key);
    }

    public static <T> Optional<Registry<T>> optionalRegistry(
        RegistryAccess access,
        ResourceKey<? extends Registry<? extends T>> key
    ) {
        return access.lookup(key);
    }
}

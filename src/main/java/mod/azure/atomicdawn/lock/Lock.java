package mod.azure.atomicdawn.lock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Lock(
    LockLevel level,
    Optional<UUID> owner,
    List<UUID> access
) {

    public static final Codec<Lock> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            LockLevel.CODEC.fieldOf("level").forGetter(Lock::level),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Lock::owner),
            UUIDUtil.CODEC.listOf().optionalFieldOf("access", List.of()).forGetter(Lock::access)
        ).apply(instance, Lock::new)
    );

    public Lock {
        access = List.copyOf(access);
    }

    public static Lock owned(LockLevel level, UUID owner) {
        return new Lock(level, Optional.of(owner), List.of());
    }

    public static Lock world(LockLevel level) {
        return new Lock(level, Optional.empty(), List.of());
    }

    public boolean ownedBy(UUID id) {
        return owner.map(id::equals).orElse(false);
    }

    public boolean grants(UUID id) {
        return ownedBy(id) || access.contains(id);
    }

    public Lock withAccess(UUID id) {
        if (grants(id)) {
            return this;
        }
        var copy = new ArrayList<>(access);
        copy.add(id);
        return new Lock(level, owner, copy);
    }

    public Lock withoutAccess() {
        return new Lock(level, owner, List.of());
    }
}

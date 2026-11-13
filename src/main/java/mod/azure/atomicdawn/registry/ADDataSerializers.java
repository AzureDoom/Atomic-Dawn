package mod.azure.atomicdawn.registry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import mod.azure.atomicdawn.AtomicDawn;

public final class ADDataSerializers {

    public static final DeferredRegister<EntityDataSerializer<?>> REGISTER = DeferredRegister.create(
        NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS,
        AtomicDawn.MOD_ID
    );

    public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG = EntityDataSerializer.forValueType(
        ByteBufCodecs.COMPOUND_TAG
    );

    static {
        REGISTER.register("compound_tag", () -> COMPOUND_TAG);
    }

    private ADDataSerializers() {}
}

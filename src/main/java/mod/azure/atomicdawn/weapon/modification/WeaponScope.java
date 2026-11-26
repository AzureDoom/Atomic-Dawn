package mod.azure.atomicdawn.weapon.modification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

import mod.azure.atomicdawn.helper.ADRegistryUtil;

public record WeaponScope(
    float zoom,
    float sensitivity,
    Optional<Identifier> overlay,
    boolean reticle
) {

    public static final Identifier DEFAULT_OVERLAY = ADRegistryUtil.vanillaId(
        "textures/misc/spyglass_scope.png"
    );

    public static final Codec<WeaponScope> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.floatRange(0.05F, 1F).optionalFieldOf("zoom", 0.25F).forGetter(WeaponScope::zoom),
            Codec.floatRange(0.05F, 1F).optionalFieldOf("sensitivity", 0.35F).forGetter(WeaponScope::sensitivity),
            Identifier.CODEC.optionalFieldOf("overlay").forGetter(WeaponScope::overlay),
            Codec.BOOL.optionalFieldOf("reticle", true).forGetter(WeaponScope::reticle)
        ).apply(instance, WeaponScope::new)
    );

    public Identifier overlayTexture() {
        return overlay.orElse(DEFAULT_OVERLAY);
    }
}

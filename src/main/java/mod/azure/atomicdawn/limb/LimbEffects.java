package mod.azure.atomicdawn.limb;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.vats.BodyPart;

public final class LimbEffects {

    private static final Identifier LEGS = AtomicDawn.id("crippled_legs");

    private static final Identifier JUMP = AtomicDawn.id("crippled_legs_jump");

    private static final Identifier ARMS = AtomicDawn.id("crippled_arms");

    private static final Identifier HEAD = AtomicDawn.id("crippled_head");

    private LimbEffects() {}

    public static void apply(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        var config = AtomicDawn.CONFIG.common.combat.limbs.penalties;
        var legs = LimbService.crippledLegs(entity);
        var speed = legs == 0
            ? 0.0
            : LimbService.legsGone(entity) ? -config.BOTH_LEGS_SPEED_PENALTY : -config.LEG_SPEED_PENALTY;
        modifier(entity, Attributes.MOVEMENT_SPEED, LEGS, speed);
        modifier(entity, Attributes.JUMP_STRENGTH, JUMP, -config.LEG_JUMP_PENALTY * legs);
        modifier(entity, Attributes.ATTACK_DAMAGE, ARMS, -config.ARM_DAMAGE_PENALTY * LimbService.crippledArms(entity));
        var head = !(entity instanceof Player) && LimbService.crippled(entity, BodyPart.HEAD)
            ? -config.HEAD_FOLLOW_RANGE_PENALTY
            : 0.0;
        modifier(entity, Attributes.FOLLOW_RANGE, HEAD, head);
    }

    private static void modifier(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double amount) {
        var instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        var current = instance.getModifier(id);
        if (current != null && current.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(
                new AttributeModifier(id, Math.max(-1.0, amount), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        }
    }
}

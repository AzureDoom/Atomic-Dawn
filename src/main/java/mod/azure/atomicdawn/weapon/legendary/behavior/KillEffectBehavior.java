package mod.azure.atomicdawn.weapon.legendary.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import mod.azure.atomicdawn.ghoul.GhoulService;
import mod.azure.atomicdawn.radiation.RadiationService;
import mod.azure.atomicdawn.registry.ADAttachments;
import mod.azure.atomicdawn.util.PlayerItems;
import mod.azure.atomicdawn.weapon.action.VatsActions;
import mod.azure.atomicdawn.weapon.legendary.LegendaryBehavior;
import mod.azure.atomicdawn.weapon.legendary.LegendaryContext;
import mod.azure.atomicdawn.weapon.legendary.LegendaryHooks;

public record KillEffectBehavior(
    Effect effect,
    float amount,
    float chance,
    float radius
) implements LegendaryBehavior {

    public static final MapCodec<KillEffectBehavior> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Effect.CODEC.fieldOf("effect").forGetter(KillEffectBehavior::effect),
            Codec.floatRange(0, 1000).optionalFieldOf("amount", 1F).forGetter(KillEffectBehavior::amount),
            Codec.floatRange(0, 1).optionalFieldOf("chance", 1F).forGetter(KillEffectBehavior::chance),
            Codec.floatRange(0, 16).optionalFieldOf("radius", 4F).forGetter(KillEffectBehavior::radius)
        ).apply(instance, KillEffectBehavior::new)
    );

    @Override
    public MapCodec<KillEffectBehavior> codec() {
        return CODEC;
    }

    @Override
    public void onKill(LegendaryContext context, float damageDealt) {
        var player = context.attacker();
        if (chance < 1F && player.getRandom().nextFloat() >= chance) {
            return;
        }
        switch (effect) {
            case ACTION_POINTS -> {
                var ap = VatsActions.actionPoints(player);
                player.setData(
                    ADAttachments.ACTION_POINTS.get(),
                    ap.withPoints(ap.points() + amount, VatsActions.maxActionPoints(player))
                );
            }
            case CURRENCY -> {
                var options = new ArrayList<ItemStack>();
                BuiltInRegistries.ITEM.getTagOrEmpty(LegendaryHooks.CURRENCY)
                    .forEach(holder -> options.add(new ItemStack(holder)));
                if (options.isEmpty()) {
                    return;
                }
                var stack = options.get(player.getRandom().nextInt(options.size()));
                stack.setCount(1 + player.getRandom().nextInt(Math.max(1, Math.round(amount))));
                PlayerItems.give(player, stack);
            }
            case GLOWING -> {
                if (GhoulService.isGhoul(player)) {
                    GhoulService.setGlow(player, GhoulService.state(player).glow() + amount);
                    return;
                }
                var level = context.level();
                var pos = context.target().position();
                var area = new AABB(pos, pos).inflate(radius);
                for (var entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                    if (entity instanceof Player other && GhoulService.isGhoul(other)) {
                        continue;
                    }
                    RadiationService.expose(entity, amount);
                }
                var spread = radius / 3;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1, pos.z, 12, spread, 0.5, spread, 0);
            }
        }
    }

    public enum Effect implements StringRepresentable {

        ACTION_POINTS("action_points"),
        CURRENCY("currency"),
        GLOWING("glowing");

        public static final Codec<Effect> CODEC = StringRepresentable.fromEnum(Effect::values);

        private final String name;

        Effect(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}

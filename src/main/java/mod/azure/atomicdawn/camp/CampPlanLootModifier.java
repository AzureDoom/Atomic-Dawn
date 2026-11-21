package mod.azure.atomicdawn.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.item.CampPlanItem;
import mod.azure.atomicdawn.registry.ADDataComponents;
import mod.azure.atomicdawn.registry.ADItems;

public class CampPlanLootModifier extends LootModifier {

    public static final MapCodec<CampPlanLootModifier> CODEC =
        RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance)
                .and(
                    Codec.floatRange(0F, 1F)
                        .optionalFieldOf("chance", 0.08F)
                        .forGetter(modifier -> modifier.chance)
                )
                .apply(instance, CampPlanLootModifier::new)
        );

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER = DeferredRegister.create(
        NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
        AtomicDawn.MOD_ID
    );

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<CampPlanLootModifier>> TYPE =
        REGISTER.register("camp_plans", () -> CODEC);

    private final float chance;

    public CampPlanLootModifier(
        Optional<Holder<LootItemCondition>> condition,
        int priority,
        float chance
    ) {
        super(condition, priority);
        this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
        @NotNull ObjectArrayList<ItemStack> generatedLoot,
        @NotNull LootContext context
    ) {
        var random = context.getRandom();
        var registries = context.getLevel().registryAccess();
        for (var i = 0; i < generatedLoot.size(); i++) {
            var stack = generatedLoot.get(i);
            if (stack.is(ADItems.CAMP_PLAN.get()) && stack.get(ADDataComponents.CAMP_PLAN.get()) == null) {
                var plan = CampPlans.random(registries, random);
                if (plan.isPresent()) {
                    stack.set(ADDataComponents.CAMP_PLAN.get(), plan.get());
                } else {
                    generatedLoot.set(i, ItemStack.EMPTY);
                }
            }
        }
        generatedLoot.removeIf(ItemStack::isEmpty);
        if (!context.getQueriedLootTableId().getPath().startsWith("chests/")) {
            return generatedLoot;
        }
        if (random.nextFloat() >= chance) {
            return generatedLoot;
        }
        CampPlans.random(registries, random).ifPresent(plan -> generatedLoot.add(CampPlanItem.stack(plan)));
        return generatedLoot;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

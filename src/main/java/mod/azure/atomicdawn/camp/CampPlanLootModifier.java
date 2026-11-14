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
        if (!context.getQueriedLootTableId().getPath().startsWith("chests/")) {
            return generatedLoot;
        }
        var random = context.getRandom();
        if (random.nextFloat() >= chance) {
            return generatedLoot;
        }
        var plans = CampBuildable.all(context.getLevel().registryAccess())
            .stream()
            .flatMap(holder -> holder.value().plan().stream())
            .distinct()
            .toList();
        if (!plans.isEmpty()) {
            generatedLoot.add(CampPlanItem.stack(plans.get(random.nextInt(plans.size()))));
        }
        return generatedLoot;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

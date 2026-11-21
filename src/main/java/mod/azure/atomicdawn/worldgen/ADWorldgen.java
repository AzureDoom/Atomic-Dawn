package mod.azure.atomicdawn.worldgen;

import dev.worldgen.lithostitched.api.event.AddWorldgenModifiersEvent;
import dev.worldgen.lithostitched.api.util.InjectionType;
import dev.worldgen.lithostitched.api.worldgen.modifier.WorldgenModifier;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.List;
import java.util.function.BiConsumer;

import mod.azure.atomicdawn.AtomicDawn;

public final class ADWorldgen {

    private static final Identifier OVERWORLD_RULE = Identifier.withDefaultNamespace("overworld");

    private ADWorldgen() {}

    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> AddWorldgenModifiersEvent.EVENT.register(ADWorldgen::addModifiers));
    }

    private static void addModifiers(RegistryAccess registries, BiConsumer<Identifier, WorldgenModifier> consumer) {
        var rules = registries.lookupOrThrow(Registries.MATERIAL_RULE);
        List<Holder<MaterialRule>> targets = rules.listElements()
            .filter(holder -> holder.key().identifier().equals(OVERWORLD_RULE))
            .<Holder<MaterialRule>>map(holder -> holder)
            .toList();
        if (targets.isEmpty()) {
            AtomicDawn.LOGGER.warn(
                "Could not find material rule {} for Atomic Dawn surface rules. Available: {}",
                OVERWORLD_RULE,
                rules.listElements().map(holder -> holder.key().identifier().toString()).toList()
            );
            return;
        }
        consumer.accept(
            AtomicDawn.id("wasteland_material_rules"),
            WorldgenModifier.builder()
                .setMaterialRule(
                    HolderSet.direct(targets),
                    Holder.direct(ADSurfaceRules.makeRules(registries.lookupOrThrow(Registries.BIOME))),
                    InjectionType.PREPEND
                )
        );
    }
}

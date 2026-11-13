package mod.azure.atomicdawn.worldgen;

import net.minecraft.core.registries.Registries;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import terrablender.api.MaterialRuleManager;
import terrablender.api.Regions;

import mod.azure.atomicdawn.AtomicDawn;

public final class ADWorldgen {

    private ADWorldgen() {}

    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new ADOverworldRegion(AtomicDawn.id("wasteland_overworld"), 3));
            MaterialRuleManager.addRules(
                MaterialRuleManager.RuleCategory.OVERWORLD,
                AtomicDawn.MOD_ID,
                registries -> ADSurfaceRules.makeRules(registries.lookupOrThrow(Registries.BIOME))
            );
        });
    }
}

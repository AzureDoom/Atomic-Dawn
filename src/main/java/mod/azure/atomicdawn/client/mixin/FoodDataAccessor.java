package mod.azure.atomicdawn.client.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface FoodDataAccessor {

    @Accessor("exhaustionLevel")
    float atomicdawn$getExhaustionLevel();

    @Accessor("exhaustionLevel")
    void atomicdawn$setExhaustionLevel(float value);
}

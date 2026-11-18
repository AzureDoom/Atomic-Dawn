package mod.azure.atomicdawn.client.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import mod.azure.atomicdawn.camp.CampBlueprint;
import mod.azure.atomicdawn.camp.CampNbt;
import mod.azure.atomicdawn.camp.CampSlotService;

public record CampBlueprintPreview(
    CampBlueprint source,
    Direction facing,
    List<Part> parts,
    List<Marker> markers
) {

    public record Part(
        BlockPos offset,
        BlockState state
    ) {}

    public record Marker(
        Vec3 offset,
        float width,
        float height
    ) {}

    public static CampBlueprintPreview of(CampBlueprint blueprint, Direction facing) {
        var rotation = CampSlotService.rotation(Direction.NORTH, facing);
        var parts = new ArrayList<Part>();
        var markers = new ArrayList<Marker>();
        for (var entry : blueprint.entries()) {
            for (var part : entry.parts()) {
                part.state()
                    .filter(state -> !state.isAir())
                    .ifPresent(state -> parts.add(new Part(part.offset().rotate(rotation), state.rotate(rotation))));
            }
            for (var entity : entry.entities()) {
                var dimensions = BuiltInRegistries.ENTITY_TYPE.getOptional(entity.type())
                    .map(type -> type.getDimensions())
                    .orElse(null);
                var width = dimensions == null ? 0.6F : dimensions.width();
                var height = dimensions == null ? 1.8F : dimensions.height();
                markers.add(new Marker(CampNbt.rotate(entity.offset(), rotation), width, height));
            }
        }
        return new CampBlueprintPreview(blueprint, facing, List.copyOf(parts), List.copyOf(markers));
    }

    public boolean matches(CampBlueprint blueprint, Direction direction) {
        return source == blueprint && facing == direction;
    }
}

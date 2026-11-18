package mod.azure.atomicdawn.client.camp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import mod.azure.atomicdawn.AtomicDawn;
import mod.azure.atomicdawn.block.MultiblockBenchBlock;
import mod.azure.atomicdawn.camp.CampModuleBlock;
import mod.azure.atomicdawn.powerarmor.PowerArmorStationBlock;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = AtomicDawn.MOD_ID, value = Dist.CLIENT)
public final class CampBoundaryRenderer {

    private static final Identifier WHITE = AtomicDawn.id("textures/misc/camp_white.png");

    private static final int VALID = 0x7CFF4A;

    private static final int INVALID = 0xFF4A3A;

    private static final int CLAIM = 0xFFC13A;

    private static final int SCRAP = 0xFF7A2A;

    private static final int SELECTION = 0x4AC8FF;

    private static final int ENTITY = 0xB07CFF;

    private static final int MODEL_LIMIT = 512;

    private static final int GHOST_LIMIT = 4096;

    private static final int SEGMENTS = 128;

    private static final float WALL_ALPHA = 0.32F;

    private static final float LINE_ALPHA = 0.85F;

    private static final float LINE_HEIGHT = 0.12F;

    private static final double NEARBY_RANGE_SQR = 160.0 * 160.0;

    private record Wall(
        Vec3 center,
        float radius,
        float down,
        float up,
        int color
    ) {}

    private record Box(
        AABB bounds,
        int color,
        float alpha
    ) {

        private Box(BlockPos pos, int color, float alpha) {
            this(new AABB(pos), color, alpha);
        }
    }

    private CampBoundaryRenderer() {}

    private static RenderType type() {
        return RenderTypes.entityTranslucent(WHITE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || CampClientState.get() != null) {
            return;
        }
        if ((holdingModule(player) || CampWorkshopClient.active()) && player.tickCount % 40 == 0) {
            CampClientState.request();
        }
    }

    @SubscribeEvent
    public static void onSubmit(SubmitCustomGeometryEvent event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        var state = CampClientState.get();
        if (player == null || level == null || state == null) {
            return;
        }
        var holding = holdingModule(player);
        var workshop = CampWorkshopClient.active();
        if (!holding && !workshop) {
            return;
        }
        BlockPos modulePreview = null;
        BlockState modulePreviewState = null;
        var walls = new ArrayList<Wall>();
        var boxes = new ArrayList<Box>();
        var models = new ArrayList<CampModulePlacementClient.GhostPart>();
        var radius = (float) state.radius();
        var up = (float) state.up();
        var down = (float) state.down();
        var camp = workshop ? CampWorkshopClient.workshopCamp(player) : CampWorkshopClient.camp(player);
        if (camp != null) {
            walls.add(new Wall(Vec3.atBottomCenterOf(camp.origin()), radius, down, up, CLAIM));
        }
        if (holding) {
            var target = moduleTarget(minecraft);
            if (target != null) {
                var deployable = CampClientState.canDeployAt(target);
                var color = deployable ? VALID : INVALID;
                walls.add(new Wall(Vec3.atBottomCenterOf(target), radius, down, up, color));
                modulePreview = target;
                modulePreviewState = ((BlockItem) (isModule(player.getMainHandItem().getItem())
                    ? player.getMainHandItem().getItem()
                    : player.getOffhandItem().getItem())).getBlock()
                    .defaultBlockState()
                    .setValue(CampModuleBlock.FACING, CampModulePlacementClient.facing());
                for (var ghost : CampModulePlacementClient.ghosts(target, deployable)) {
                    addShapeBoxes(boxes, level, ghost.pos(), ghost.state(), ghost.valid() ? VALID : INVALID);
                }
            }
            for (var other : state.nearby()) {
                if (other.origin().distSqr(player.blockPosition()) < NEARBY_RANGE_SQR) {
                    walls.add(new Wall(Vec3.atBottomCenterOf(other.origin()), radius, down, up, INVALID));
                }
            }
        }
        if (workshop) {
            var hovered = CampWorkshopClient.hoveredObject();
            if (hovered.isPresent()) {
                var armed = CampWorkshopClient.scrapArmed(hovered.get().pos());
                for (var part : hovered.get().parts()) {
                    boxes.add(new Box(part, SCRAP, armed ? 0.55F : 0.25F));
                }
            }
            if (CampWorkshopClient.selecting()) {
                addSelection(boxes);
            } else if (CampWorkshopClient.selectedBlueprint() != null) {
                addBlueprint(boxes, models, level);
            } else if (CampWorkshopClient.selected() != null) {
                var target = CampWorkshopClient.target();
                if (target != null) {
                    var color = CampWorkshopClient.failure(target).isEmpty() ? VALID : INVALID;
                    addGhostBoxes(boxes, level, target, CampWorkshopClient.previewState(target), color);
                }
            }
        }
        if (walls.isEmpty() && boxes.isEmpty() && models.isEmpty()) {
            return;
        }
        var camera = minecraft.gameRenderer.mainCamera().position();
        for (var ghost : models) {
            var ghostPose = event.getPoseStack();
            ghostPose.pushPose();
            ghostPose.translate(
                ghost.pos().getX() - camera.x,
                ghost.pos().getY() - camera.y,
                ghost.pos().getZ() - camera.z
            );
            var model = new BlockModelRenderState();
            minecraft.getBlockModelResolver().update(model, ghost.state(), BlockDisplayContext.create());
            model.submit(ghostPose, event.getSubmitNodeCollector(), 0x00F000F0, OverlayTexture.NO_OVERLAY, 0);
            ghostPose.popPose();
        }
        if (modulePreview != null && modulePreviewState != null) {
            var modulePose = event.getPoseStack();
            modulePose.pushPose();
            modulePose.translate(
                modulePreview.getX() - camera.x,
                modulePreview.getY() - camera.y,
                modulePreview.getZ() - camera.z
            );
            var model = new BlockModelRenderState();
            minecraft.getBlockModelResolver().update(model, modulePreviewState, BlockDisplayContext.create());
            model.submit(modulePose, event.getSubmitNodeCollector(), 0x00F000F0, OverlayTexture.NO_OVERLAY, 0);
            modulePose.popPose();
        }
        var partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var time = (level.getGameTime() + partial) * 0.05F;
        var pose = event.getPoseStack();
        pose.pushPose();
        event.getSubmitNodeCollector()
            .submitCustomGeometry(pose, type(), (transform, consumer) -> {
                for (var wall : walls) {
                    drawWall(consumer, transform, camera, wall, time);
                }
                for (var box : boxes) {
                    drawBox(consumer, transform, camera, box);
                }
            });
        pose.popPose();
    }

    private static void addSelection(ArrayList<Box> boxes) {
        var first = CampWorkshopClient.cornerA();
        if (first == null) {
            var hit = CampWorkshopClient.hit();
            if (hit != null) {
                boxes.add(new Box(hit.getBlockPos(), SELECTION, 0.3F));
            }
            return;
        }
        var second = CampWorkshopClient.cornerB();
        if (second == null) {
            var hit = CampWorkshopClient.hit();
            second = hit == null ? first : hit.getBlockPos();
        }
        var bounds = new AABB(first).minmax(new AABB(second));
        boxes.add(new Box(bounds, SELECTION, CampWorkshopClient.hasSelection() ? 0.18F : 0.1F));
        boxes.add(new Box(first, SELECTION, 0.45F));
        if (CampWorkshopClient.hasSelection()) {
            boxes.add(new Box(CampWorkshopClient.cornerB(), SELECTION, 0.45F));
        }
    }

    private static void addBlueprint(
        ArrayList<Box> boxes,
        ArrayList<CampModulePlacementClient.GhostPart> models,
        Level level
    ) {
        var ghosts = CampWorkshopClient.blueprintGhosts();
        var anchor = CampWorkshopClient.blueprintAnchor();
        var preview = CampWorkshopClient.blueprintPreview();
        if (anchor == null || preview == null) {
            return;
        }
        var ready = CampWorkshopClient.blueprintFailure().isEmpty();
        var drawModels = ghosts.size() <= MODEL_LIMIT;
        var count = 0;
        for (var ghost : ghosts) {
            if (count++ >= GHOST_LIMIT) {
                break;
            }
            if (drawModels && ghost.valid()) {
                models.add(ghost);
            }
            var color = ghost.valid() ? (ready ? VALID : CLAIM) : INVALID;
            addShapeBoxes(boxes, level, ghost.pos(), ghost.state(), color);
        }
        var base = Vec3.atBottomCenterOf(anchor);
        for (var marker : preview.markers()) {
            var center = base.add(marker.offset());
            var half = marker.width() / 2.0;
            boxes.add(
                new Box(
                    new AABB(
                        center.x - half,
                        center.y,
                        center.z - half,
                        center.x + half,
                        center.y + marker.height(),
                        center.z + half
                    ),
                    ENTITY,
                    0.25F
                )
            );
        }
    }

    private static void addGhostBoxes(
        ArrayList<Box> boxes,
        Level level,
        BlockPos target,
        @Nullable BlockState state,
        int color
    ) {
        if (state == null) {
            boxes.add(new Box(target, color, 0.4F));
            return;
        }
        var alpha = 0.22F;
        if (state.getBlock() instanceof MultiblockBenchBlock) {
            var master = MultiblockBenchBlock.masterPos(target, state);
            var facing = state.getValue(MultiblockBenchBlock.FACING);
            for (var layer = 0; layer < MultiblockBenchBlock.DEPTH; layer++) {
                for (var row = 0; row < MultiblockBenchBlock.HEIGHT; row++) {
                    for (var column = 0; column < MultiblockBenchBlock.WIDTH; column++) {
                        boxes.add(
                            new Box(MultiblockBenchBlock.cellPos(master, facing, column, row, layer), color, alpha)
                        );
                    }
                }
            }
            return;
        }
        if (state.getBlock() instanceof PowerArmorStationBlock) {
            var master = PowerArmorStationBlock.masterPos(target, state);
            var facing = state.getValue(PowerArmorStationBlock.FACING);
            for (var layer = 0; layer < PowerArmorStationBlock.SIZE; layer++) {
                for (var row = 0; row < PowerArmorStationBlock.SIZE; row++) {
                    for (var column = 0; column < PowerArmorStationBlock.SIZE; column++) {
                        var pos = PowerArmorStationBlock.cellPos(master, facing, column, row, layer);
                        var cell = state.setValue(PowerArmorStationBlock.COLUMN, column)
                            .setValue(PowerArmorStationBlock.ROW, row)
                            .setValue(PowerArmorStationBlock.LAYER, layer);
                        cell.getShape(level, pos).forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
                            boxes.add(
                                new Box(
                                    new AABB(
                                        pos.getX() + x0,
                                        pos.getY() + y0,
                                        pos.getZ() + z0,
                                        pos.getX() + x1,
                                        pos.getY() + y1,
                                        pos.getZ() + z1
                                    ),
                                    color,
                                    alpha
                                )
                            );
                        });
                    }
                }
            }
            return;
        }
        boxes.add(new Box(target, color, alpha));
        if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.HALF)) {
            var dy = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? 1 : -1;
            boxes.add(new Box(target.offset(0, dy, 0), color, alpha));
        }
    }

    private static void addShapeBoxes(ArrayList<Box> boxes, Level level, BlockPos pos, BlockState state, int color) {
        var alpha = 0.22F;
        var shape = state.getShape(level, pos);
        if (shape.isEmpty()) {
            boxes.add(new Box(pos, color, alpha));
            return;
        }
        shape.forAllBoxes(
            (x0, y0, z0, x1, y1, z1) -> boxes.add(
                new Box(
                    new AABB(
                        pos.getX() + x0,
                        pos.getY() + y0,
                        pos.getZ() + z0,
                        pos.getX() + x1,
                        pos.getY() + y1,
                        pos.getZ() + z1
                    ),
                    color,
                    alpha
                )
            )
        );
    }

    private static boolean holdingModule(LocalPlayer player) {
        return CampModulePlacementClient.holdingModule(player);
    }

    private static boolean isModule(Item item) {
        return CampModulePlacementClient.isModule(item);
    }

    private static @Nullable BlockPos moduleTarget(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        var level = minecraft.level;
        if (level == null) {
            return null;
        }
        var clicked = hit.getBlockPos();
        return level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
    }

    private static void drawWall(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera, Wall wall, float time) {
        var cx = (float) (wall.center().x - camera.x);
        var cy = (float) (wall.center().y - camera.y);
        var cz = (float) (wall.center().z - camera.z);
        var red = (wall.color() >> 16) & 0xFF;
        var green = (wall.color() >> 8) & 0xFF;
        var blue = wall.color() & 0xFF;
        var bottom = cy - wall.down();
        var top = cy + wall.up();
        var pulse = cy + (time % 1F) * wall.up();
        for (var segment = 0; segment < SEGMENTS; segment++) {
            var a0 = segment / (float) SEGMENTS * Mth.TWO_PI;
            var a1 = (segment + 1) / (float) SEGMENTS * Mth.TWO_PI;
            var x0 = cx + Mth.cos(a0) * wall.radius();
            var z0 = cz + Mth.sin(a0) * wall.radius();
            var x1 = cx + Mth.cos(a1) * wall.radius();
            var z1 = cz + Mth.sin(a1) * wall.radius();
            var stripe = segment % 8 == 0 ? 1.6F : 1F;
            var mid = alpha(WALL_ALPHA * stripe);
            quad(consumer, pose, x0, z0, x1, z1, bottom, cy, 0, mid, red, green, blue);
            quad(consumer, pose, x0, z0, x1, z1, cy, top, mid, 0, red, green, blue);
            var line = alpha(LINE_ALPHA);
            quad(consumer, pose, x0, z0, x1, z1, cy, cy + LINE_HEIGHT, line, line, red, green, blue);
            var glow = alpha(LINE_ALPHA * 0.5F * (1F - (pulse - cy) / Math.max(1F, wall.up())));
            quad(consumer, pose, x0, z0, x1, z1, pulse, pulse + LINE_HEIGHT, glow, glow, red, green, blue);
        }
    }

    private static int alpha(float value) {
        return Math.round(255 * Mth.clamp(value, 0F, 1F));
    }

    private static void quad(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x0,
        float z0,
        float x1,
        float z1,
        float y0,
        float y1,
        int alphaBottom,
        int alphaTop,
        int red,
        int green,
        int blue
    ) {
        vertex(consumer, pose, x0, y0, z0, 0F, 0F, red, green, blue, alphaBottom);
        vertex(consumer, pose, x1, y0, z1, 1F, 0F, red, green, blue, alphaBottom);
        vertex(consumer, pose, x1, y1, z1, 1F, 1F, red, green, blue, alphaTop);
        vertex(consumer, pose, x0, y1, z0, 0F, 1F, red, green, blue, alphaTop);
    }

    private static final int[][] FACES = {
        { 0, 1, 5, 4 },
        { 3, 7, 6, 2 },
        { 0, 3, 2, 1 },
        { 4, 5, 6, 7 },
        { 0, 4, 7, 3 },
        { 1, 2, 6, 5 } };

    private static final float[][] UV = { { 0F, 0F }, { 1F, 0F }, { 1F, 1F }, { 0F, 1F } };

    private static void drawBox(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera, Box box) {
        var inflate = 0.004F;
        var x0 = (float) (box.bounds().minX - camera.x) - inflate;
        var y0 = (float) (box.bounds().minY - camera.y) - inflate;
        var z0 = (float) (box.bounds().minZ - camera.z) - inflate;
        var x1 = (float) (box.bounds().maxX - camera.x) + inflate;
        var y1 = (float) (box.bounds().maxY - camera.y) + inflate;
        var z1 = (float) (box.bounds().maxZ - camera.z) + inflate;
        var corners = new float[][] {
            { x0, y0, z0 },
            { x1, y0, z0 },
            { x1, y0, z1 },
            { x0, y0, z1 },
            { x0, y1, z0 },
            { x1, y1, z0 },
            { x1, y1, z1 },
            { x0, y1, z1 } };
        var red = (box.color() >> 16) & 0xFF;
        var green = (box.color() >> 8) & 0xFF;
        var blue = box.color() & 0xFF;
        var a = alpha(box.alpha());
        for (var face : FACES) {
            for (var i = 0; i < 4; i++) {
                var corner = corners[face[i]];
                vertex(consumer, pose, corner[0], corner[1], corner[2], UV[i][0], UV[i][1], red, green, blue, a);
            }
        }
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(0x00F000F0)
            .setNormal(pose, 0F, 1F, 0F);
    }
}

package cn.mcbowser.client.render;

import cn.mcbowser.block.entity.DisplayPanelBlockEntity;
import cn.mcbowser.client.browser.DisplayBrowserManager;
import cn.mcbowser.screen.DisplayStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.keksuccino.rinku.RinkuBrowser;
import de.keksuccino.rinku.Rinku;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class DisplayPanelRenderer implements BlockEntityRenderer<DisplayPanelBlockEntity, DisplayPanelRenderState> {
    private static final float DEPTH_OFFSET = 0.002F;

    @Override
    public DisplayPanelRenderState createRenderState() {
        return new DisplayPanelRenderState();
    }

    @Override
    public void extractRenderState(DisplayPanelBlockEntity blockEntity, DisplayPanelRenderState state,
                                   float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.browser = null;
        if (blockEntity.getLevel() == null) return;
        DisplayStructure structure = DisplayStructure.find(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (structure == null || !structure.origin().equals(blockEntity.getBlockPos())) return;
        DisplayBrowserManager.Session session = DisplayBrowserManager.find(blockEntity.getLevel(), structure);
        if (session == null && blockEntity.isDisplayEnabled() && Rinku.isInitialized()) {
            session = DisplayBrowserManager.getOrCreate(blockEntity.getLevel(), structure);
        }
        if (session == null || !session.isEnabled() || !session.browser().isTextureReady()) return;
        state.facing = structure.facing();
        state.width = structure.width();
        state.height = structure.height();
        state.browser = session.browser();
    }

    @Override
    public void submit(DisplayPanelRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        RinkuBrowser browser = state.browser;
        if (browser == null) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(browser.getTextureIdentifier()),
                (pose, buffer) -> drawScreen(pose, buffer, state.facing, state.width, state.height));
    }

    private static void drawScreen(PoseStack.Pose pose, VertexConsumer buffer, Direction facing, int width, int height) {
        Direction right = facing.getClockWise();
        float ox = 0.5F - right.getStepX() * 0.5F + facing.getStepX() * (0.5F + DEPTH_OFFSET);
        float oz = 0.5F - right.getStepZ() * 0.5F + facing.getStepZ() * (0.5F + DEPTH_OFFSET);
        float rx = right.getStepX() * width;
        float rz = right.getStepZ() * width;
        float nx = facing.getStepX();
        float nz = facing.getStepZ();

        // UVs are vertically flipped because CEF's off-screen buffer uses a top-left origin.
        vertex(pose, buffer, ox, height, oz, 1, 0, nx, nz);
        vertex(pose, buffer, ox + rx, height, oz + rz, 0, 0, nx, nz);
        vertex(pose, buffer, ox + rx, 0, oz + rz, 0, 1, nx, nz);
        vertex(pose, buffer, ox, 0, oz, 1, 1, nx, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                               float u, float v, float nx, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, nx, 0, nz);
    }

    @Override
    public AABB getRenderBoundingBox(DisplayPanelBlockEntity blockEntity) {
        if (blockEntity.getLevel() == null) return new AABB(blockEntity.getBlockPos());
        DisplayStructure structure = DisplayStructure.find(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (structure == null) return new AABB(blockEntity.getBlockPos());
        return new AABB(structure.origin()).expandTowards(
                structure.facing().getClockWise().getUnitVec3().scale(structure.width() - 1)
                        .add(0, structure.height() - 1, 0)).inflate(1);
    }
}

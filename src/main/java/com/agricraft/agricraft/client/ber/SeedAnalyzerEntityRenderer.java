package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.entity.SeedAnalyzerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class SeedAnalyzerEntityRenderer implements BlockEntityRenderer<SeedAnalyzerBlockEntity, SeedAnalyzerEntityRenderer.State> {
    private final ItemModelResolver itemModels;

    public SeedAnalyzerEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SeedAnalyzerBlockEntity analyzer, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(analyzer, state, partialTick, camera, crumbling);
        itemModels.updateForTopItem(state.seed, analyzer.getSeed(), ItemDisplayContext.GROUND, analyzer.getLevel(), null, 0);
        state.angle = 720.0F * (System.currentTimeMillis() & 0x3FFFL) / 0x3FFFL;
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.seed.isEmpty()) {
            poses.pushPose();
            poses.translate(0.5, 0.5, 0.5);
            poses.mulPose(Axis.YP.rotationDegrees(state.angle));
            state.seed.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.popPose();
        }
    }

    public static final class State extends BlockEntityRenderState {
        private final ItemStackRenderState seed = new ItemStackRenderState();
        private float angle;
    }
}

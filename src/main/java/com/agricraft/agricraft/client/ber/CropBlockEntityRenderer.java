package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.api.AgriClientApi;
import com.agricraft.agricraft.client.AgriCraftClient;
import com.agricraft.agricraft.client.ClientUtil;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.agricraft.agricraft.compat.mysticalagriculture.MysticalAgriculturePlugin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CropBlockEntityRenderer implements BlockEntityRenderer<CropBlockEntity, CropBlockEntityRenderer.State> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModels;

    public CropBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModels = context.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CropBlockEntity crop, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(crop, state, partialTick, camera, crumbling);
        state.sticks = null;
        if (crop.hasCropSticks()) {
            String model = ClientUtil.getModelForSticks(crop.getBlockState().getValue(CropBlock.STICK_VARIANT));
            state.sticks = AgriCraftClient.getBlockModel(Identifier.parse(crop.isCrossCropSticks() ? model.replace("crop", "cross_crop") : model));
        }
        state.nativePlant.clear();
        state.plant = null;
        if (crop.hasPlant()) {
            var growth = crop.getGrowthStage();
            var nativeState = MysticalAgriculturePlugin.cropState(crop.getPlantId(), growth.index(), growth.total());
            if (nativeState == null) {
                state.plant = AgriClientApi.get().getPlantModel(crop.getPlantId(), growth.index());
            } else {
                blockModels.update(state.nativePlant, nativeState, DISPLAY_CONTEXT);
            }
        }
        state.weeds = crop.hasWeeds() ? AgriClientApi.get().getWeedModel(crop.getWeedId().toString(), crop.getWeedGrowthStage().index()) : null;
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        submitModel(state.sticks, state, poses, collector);
        submitModel(state.plant, state, poses, collector);
        if (!state.nativePlant.isEmpty()) {
            state.nativePlant.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        submitModel(state.weeds, state, poses, collector);
    }

    private static void submitModel(BlockStateModel model, State state, PoseStack poses, SubmitNodeCollector collector) {
        if (model != null) {
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(state.blockPos.asLong()), parts);
            collector.submitBlockModel(poses, RenderTypes.cutoutMovingBlock(), parts, new int[0],
                    state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
    }

    public static final class State extends BlockEntityRenderState {
        private BlockStateModel sticks;
        private BlockStateModel plant;
        private final BlockModelRenderState nativePlant = new BlockModelRenderState();
        private BlockStateModel weeds;
    }
}

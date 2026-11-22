package com.agricraft.agricraft.client;

import com.agricraft.agricraft.AgriClientApiImpl;
import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.AgriClientApi;
import com.agricraft.agricraft.client.ber.CropBlockEntityRenderer;
import com.agricraft.agricraft.client.ber.SeedAnalyzerEntityRenderer;
import com.agricraft.agricraft.client.bewlr.AgriSeedModel;
import com.agricraft.agricraft.client.gui.MagnifyingGlassOverlay;
import com.agricraft.agricraft.client.gui.SeedAnalyzerScreen;
import com.agricraft.agricraft.common.item.SeedBagItem;
import com.agricraft.agricraft.common.item.TrowelItem;
import com.agricraft.agricraft.common.registry.AgriBlockEntities;
import com.mojang.serialization.MapCodec;
import com.agricraft.agricraft.common.registry.AgriMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = AgriApi.MOD_ID, value = Dist.CLIENT)
public class AgriCraftClient {
    private static final Pattern GROWTH_STAGE_MODEL = Pattern.compile(".+_stage[0-9]+");
    private static volatile Map<Identifier, StandaloneModelKey<BlockStateModel>> blockModels = Map.of();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        AgriClientApi.set(new AgriClientApiImpl());
    }

    @SubscribeEvent
    public static void registerMenuScreen(RegisterMenuScreensEvent event) {
        event.register(AgriMenuTypes.SEED_ANALYZER_MENU.get(), SeedAnalyzerScreen::new);
    }

    @SubscribeEvent
    public static void loadModels(ModelEvent.RegisterStandalone event) {
        Map<Identifier, StandaloneModelKey<BlockStateModel>> models = new HashMap<>();
        for (String directory : List.of("crop", "weed")) {
            FileToIdConverter files = FileToIdConverter.json("models/" + directory);
            for (Identifier resource : files.listMatchingResources(Minecraft.getInstance().getResourceManager()).keySet()) {
                Identifier stageModel = files.fileToId(resource);
                if (GROWTH_STAGE_MODEL.matcher(stageModel.getPath()).matches()) {
                    registerBlockModel(event, models, stageModel.withPrefix(directory + "/"));
                }
            }
        }
        registerBlockModel(event, models, AgriClientApi.UNKNOWN_PLANT);
        for (String variant : List.of("wooden", "iron", "obsidian")) {
            registerBlockModel(event, models, Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "block/" + variant + "_crop_sticks"));
            registerBlockModel(event, models, Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "block/" + variant + "_cross_crop_sticks"));
        }
        blockModels = Map.copyOf(models);
    }

    private static void registerBlockModel(ModelEvent.RegisterStandalone event,
                                           Map<Identifier, StandaloneModelKey<BlockStateModel>> models, Identifier id) {
        StandaloneModelKey<BlockStateModel> key = new StandaloneModelKey<>(id::toString);
        event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(id));
        models.put(id, key);
    }

    public static BlockStateModel getBlockModel(Identifier id) {
        StandaloneModelKey<BlockStateModel> key = blockModels.get(id);
        return key == null ? null : Minecraft.getInstance().getModelManager().getStandaloneModel(key);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AgriBlockEntities.CROP.get(), CropBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(AgriBlockEntities.SEED_ANALYZER.get(), SeedAnalyzerEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "seed"), AgriSeedModel.Unbaked.CODEC);
    }

    @SubscribeEvent
    public static void registerItemProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "trowel"), TrowelProperty.CODEC);
        event.register(Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "seed_bag"), SeedBagProperty.CODEC);
    }

    private record TrowelProperty() implements RangeSelectItemModelProperty {
        private static final MapCodec<TrowelProperty> CODEC = MapCodec.unit(new TrowelProperty());

        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            return stack.getItem() instanceof TrowelItem trowel && trowel.hasPlant(stack) ? 1 : 0;
        }

        @Override
        public MapCodec<TrowelProperty> type() {
            return CODEC;
        }
    }

    private record SeedBagProperty() implements RangeSelectItemModelProperty {
        private static final MapCodec<SeedBagProperty> CODEC = MapCodec.unit(new SeedBagProperty());

        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            return SeedBagItem.isFilled(stack) ? 1 : SeedBagItem.isEmpty(stack) ? 0.5F : 0;
        }

        @Override
        public MapCodec<SeedBagProperty> type() {
            return CODEC;
        }
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "magnifying_glass_info"), MagnifyingGlassOverlay::renderOverlay);
    }
}

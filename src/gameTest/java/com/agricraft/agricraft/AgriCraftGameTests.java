package com.agricraft.agricraft;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.AgriCraftConfig;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.block.CropState;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.agricraft.agricraft.common.block.entity.SeedAnalyzerBlockEntity;
import com.agricraft.agricraft.common.handler.VanillaSeedConversion;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.registry.AgriBlocks;
import com.agricraft.agricraft.common.registry.AgriDataComponents;
import com.agricraft.agricraft.compat.mysticalagriculture.MysticalAgriculturePlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Optional;

@EventBusSubscriber(modid = AgriApi.MOD_ID)
public final class AgriCraftGameTests {
    private static final Identifier COLLISION = id("crop_stick_collision_config");
    private static final Identifier VANILLA = id("wheat_planting_and_harvest");
    private static final Identifier MYSTICAL = id("mystical_planting_and_harvest");
    private static final Identifier HARVESTER = id("mystical_harvester");
    private static final Identifier EMPTY = id("empty");

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(COLLISION, AgriCraftGameTests::collision);
            registry.register(VANILLA, AgriCraftGameTests::vanilla);
            if (ModList.get().isLoaded(MysticalAgriculturePlugin.MOD_ID)) {
                registry.register(MYSTICAL, AgriCraftGameTests::mystical);
                registry.register(HARVESTER, MysticalHarvesterGameTest::run);
            }
        });
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("test_environment"), new TestEnvironmentDefinition.Functions(Optional.empty(), Optional.empty()));
        var data = new TestData<>(environment, EMPTY, 200, 1, true);
        event.registerTest(COLLISION, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, COLLISION), data));
        event.registerTest(VANILLA, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, VANILLA), data));
        if (ModList.get().isLoaded(MysticalAgriculturePlugin.MOD_ID)) {
            event.registerTest(MYSTICAL, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, MYSTICAL), data));
            event.registerTest(HARVESTER, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, HARVESTER), data));
        }
    }

    private static void collision(GameTestHelper helper) {
        var setting = AgriCraftConfig.CROP_STICKS_COLLIDE;
        helper.assertTrue(!setting.getDefault(), "Crop sticks should be walk-through by default");
        boolean original = setting.get();
        var level = helper.getLevel();
        var relativePos = new BlockPos(2, 2, 2);
        var pos = helper.absolutePos(relativePos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var movement = new Vec3(0, 0, 2);
        double startZ = pos.getZ() - 0.5;

        helper.setBlock(relativePos.below(), Blocks.FARMLAND);
        try {
            for (CropState cropState : CropState.values()) {
                var state = AgriBlocks.CROP.get().defaultBlockState().setValue(CropBlock.CROP_STATE, cropState);
                helper.setBlock(relativePos, state);
                if (cropState.hasPlant()) {
                    helper.getBlockEntity(relativePos, CropBlockEntity.class)
                            .plantGenome(new AgriGenome(Identifier.withDefaultNamespace("wheat")));
                }
                for (boolean enabled : new boolean[]{false, true}) {
                    setting.set(enabled);
                    boolean shouldCollide = enabled && cropState.hasSticks();
                    boolean hasCollisionShape = !helper.getBlockState(relativePos).getCollisionShape(level, pos).isEmpty();
                    helper.assertTrue(hasCollisionShape == shouldCollide,
                            "Wrong collision shape for " + cropState + " with collision " + enabled);
                    player.setPos(pos.getX() + 0.35, pos.getY(), startZ);
                    player.move(MoverType.SELF, movement);
                    helper.assertTrue((player.getZ() < startZ + movement.z - 0.01) == shouldCollide,
                            "Wrong player movement for " + cropState + " with collision " + enabled);
                    if (cropState.hasSticks()) {
                        helper.assertTrue(!state.getShape(level, pos).isEmpty(), "Crop sticks should still be selectable");
                    }
                }
            }
        } finally {
            setting.set(original);
        }
        helper.succeed();
    }

    private static void vanilla(GameTestHelper helper) {
        checkCrop(helper, "minecraft:wheat", "minecraft:wheat_seeds", "minecraft:wheat", new BlockPos(1, 2, 1));
        helper.succeed();
    }

    private static void mystical(GameTestHelper helper) {
        checkCrop(helper, "mysticalagriculture:inferium", "mysticalagriculture:inferium_seeds", "mysticalagriculture:inferium_essence", new BlockPos(1, 2, 1));
        checkCrop(helper, "mysticalagriculture:diamond", "mysticalagriculture:diamond_seeds", "mysticalagriculture:diamond_essence", new BlockPos(4, 2, 1));
        helper.succeed();
    }

    private static void checkCrop(GameTestHelper helper, String plantId, String seedId, String productId, BlockPos relativePos) {
        var level = helper.getLevel();
        helper.assertTrue(AgriApi.get().getPlant(Identifier.parse(plantId), level.registryAccess()).isPresent(), "Plant definition missing: " + plantId);
        helper.setBlock(relativePos.below(), Blocks.FARMLAND);
        helper.setBlock(relativePos, AgriBlocks.CROP.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var seed = BuiltInRegistries.ITEM.getOptional(Identifier.parse(seedId)).orElseThrow();
        var seedStack = new ItemStack(seed);
        player.setItemInHand(InteractionHand.MAIN_HAND, seedStack);
        var cropPos = helper.absolutePos(relativePos);
        var hit = new BlockHitResult(Vec3.atCenterOf(cropPos), Direction.UP, cropPos, false);
        var crop = helper.getBlockEntity(relativePos, CropBlockEntity.class);
        helper.getBlockState(relativePos).useItemOn(seedStack, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(crop.hasPlant() && Identifier.parse(plantId).equals(crop.getPlantId()), "Crop sticks did not accept " + seedId);
        helper.assertTrue(seedStack.isEmpty(), "Planting must consume one seed");

        var analyzerOffset = relativePos.south(2);
        helper.setBlock(analyzerOffset, AgriBlocks.SEED_ANALYZER.get());
        var analyzerPos = helper.absolutePos(analyzerOffset);
        var analyzer = helper.getBlockEntity(analyzerOffset, SeedAnalyzerBlockEntity.class);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(seed));
        var analyzerHit = new BlockHitResult(Vec3.atCenterOf(analyzerPos), Direction.UP, analyzerPos, false);
        helper.assertTrue(VanillaSeedConversion.onRightClick(player, InteractionHand.MAIN_HAND, analyzerPos, analyzerHit),
                "Seed analyzer did not accept " + seedId);
        helper.assertTrue(analyzer.getSeed().has(AgriDataComponents.GENOME) && plantId.equals(AgriSeedItem.getSpecies(analyzer.getSeed())),
                "Seed analyzer did not retain the genome for " + seedId);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        int stages = crop.getGrowthStage().total();
        for (int i = 0; i < 256 && !crop.isFullyGrown(); i++) {
            crop.applyGrowthTick();
        }
        helper.assertTrue(crop.canBeHarvested(), "Mature crop is not harvestable: " + plantId);
        // Wheat inherits a probabilistic product table, so allow repeated harvests.
        int attempts = plantId.equals("minecraft:wheat") ? 8 : 1;
        var product = BuiltInRegistries.ITEM.getOptional(Identifier.parse(productId)).orElseThrow();
        var dropArea = new AABB(cropPos).inflate(0.75);
        for (int i = 0; i < attempts; i++) {
            crop.setGrowthStage(new AgriGrowthStage(stages - 1, stages));
            helper.getBlockState(relativePos).useWithoutItem(level, player, hit);
            if (!level.getEntitiesOfClass(ItemEntity.class, dropArea, item -> item.getItem().is(product)).isEmpty()) {
                break;
            }
        }
        helper.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, dropArea, item -> item.getItem().is(product)).isEmpty(),
                "Missing harvest product " + productId);
        helper.assertTrue(crop.hasPlant() && crop.getGrowthStage().index() < stages - 1,
                "Harvest must leave the genome and reset the growth stage");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, path);
    }
}

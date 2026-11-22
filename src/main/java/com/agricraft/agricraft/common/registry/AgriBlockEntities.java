package com.agricraft.agricraft.common.registry;

import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.agricraft.agricraft.common.block.entity.SeedAnalyzerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.ApiStatus;

import static com.agricraft.agricraft.common.registry.AgriRegistries.BLOCK_ENTITY_TYPES;

public interface AgriBlockEntities {

	DeferredHolder<BlockEntityType<?>, BlockEntityType<CropBlockEntity>> CROP = BLOCK_ENTITY_TYPES.register("crop", () -> new BlockEntityType<>(CropBlockEntity::new, AgriBlocks.CROP.get()));
	DeferredHolder<BlockEntityType<?>, BlockEntityType<SeedAnalyzerBlockEntity>> SEED_ANALYZER = BLOCK_ENTITY_TYPES.register("seed_analyzer", () -> new BlockEntityType<>(SeedAnalyzerBlockEntity::new, AgriBlocks.SEED_ANALYZER.get()));

	@ApiStatus.Internal
	static void register() {}

}

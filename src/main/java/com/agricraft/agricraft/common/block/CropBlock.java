package com.agricraft.agricraft.common.block;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.AgriCraftConfig;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.client.ClientUtil;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.item.CropSticksItem;
import com.agricraft.agricraft.common.registry.AgriItems;
import com.agricraft.agricraft.compat.mysticalagriculture.MysticalAgriculturePlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("deprecation")
public class CropBlock extends Block implements EntityBlock, BonemealableBlock, SimpleFluidloggedBlock {

	public static final VoxelShape SINGLE_STICKS = Stream.of(
			Block.box(2, -3, 2, 3, 14, 3),
			Block.box(13, -3, 2, 14, 14, 3),
			Block.box(2, -3, 13, 3, 14, 14),
			Block.box(13, -3, 13, 14, 14, 14)
	).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
	public static final VoxelShape CROSS_STICKS = Stream.of(
			SINGLE_STICKS,
			Block.box(0, 11, 2, 16, 12, 3),
			Block.box(0, 11, 13, 16, 12, 14),
			Block.box(2, 11, 0, 3, 12, 16),
			Block.box(13, 11, 0, 14, 12, 16)
	).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
	public static final EnumProperty<CropStickVariant> STICK_VARIANT = EnumProperty.create("variant", CropStickVariant.class, CropStickVariant.values());
	public static final EnumProperty<CropState> CROP_STATE = EnumProperty.create("crop", CropState.class, CropState.values());
	public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 16);

	public CropBlock(Properties properties) {
		super(properties
				.mapColor(MapColor.PLANT)
				.pushReaction(PushReaction.DESTROY)
				.isRedstoneConductor((state, getter, pos) -> false)
				.randomTicks()
				.noOcclusion()
				.dynamicShape()
				.forceSolidOff()
				.noTerrainParticles()
				.lightLevel(blockState -> {
					int light = blockState.getValue(LIGHT);
					if (blockState.getValue(LAVALOGGED)) {
						int lava = Blocks.LAVA.defaultBlockState().getLightEmission();
						if (lava > light) {
							return lava;
						}
					}
					return light;
				})
				.sound(SoundType.CROP));
		this.registerDefaultState(this.stateDefinition.any().setValue(LAVALOGGED, false)
				.setValue(WATERLOGGED, false)
				.setValue(STICK_VARIANT, CropStickVariant.WOODEN)
				.setValue(CROP_STATE, CropState.SINGLE_STICKS)
				.setValue(LIGHT, 0));
	}

	public static void spawnItem(Level level, BlockPos pos, ItemStack stack) {
		level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
	}

	public static InteractionResult applyCropSticks(Level level, BlockPos pos, BlockState state, CropStickVariant variant) {
		if (variant == null) {
			return InteractionResult.FAIL;
		}
		if (level.isClientSide()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		CropState cropState = state.getValue(CROP_STATE);
		BlockState newState = state;
		if (cropState == CropState.PLANT) {
			newState = state.setValue(CROP_STATE, CropState.PLANT_STICKS);
		} else if (cropState == CropState.SINGLE_STICKS && state.getValue(STICK_VARIANT) == variant) {
			newState = state.setValue(CROP_STATE, CropState.DOUBLE_STICKS);
		}
		if (newState == state) {
			return InteractionResult.FAIL;
		} else {
			level.setBlock(pos, newState, 3);
			if (cropState.hasSticks()) {
				variant.playSound(level, pos);
			} else {
				SoundType sound = Blocks.WHEAT.getSoundType(Blocks.WHEAT.defaultBlockState(), level, pos, null);
				level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
			}
			return InteractionResult.SUCCESS;
		}
	}

	public static InteractionResult removeCropSticks(Level level, BlockPos pos, BlockState state) {
		CropStickVariant stickVariant = state.getValue(STICK_VARIANT);
		if (level.isClientSide()) {
			return InteractionResult.PASS;
		}
		CropState cropState = state.getValue(CROP_STATE);
		BlockState newState = state;
		if (!cropState.hasPlant()) {
			newState = state.setValue(CROP_STATE, CropState.SINGLE_STICKS);
		}
		if (newState == state) {
			return InteractionResult.FAIL;
		} else {
			level.setBlock(pos, newState, 3);
			if (cropState == CropState.DOUBLE_STICKS) {
				stickVariant.playSound(level, pos);
			}
			return InteractionResult.SUCCESS;
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LAVALOGGED);
		builder.add(WATERLOGGED);
		builder.add(STICK_VARIANT);
		builder.add(CROP_STATE);
		builder.add(LIGHT);
	}

	@Override
	@NotNull
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		CropState cropState = state.getValue(CROP_STATE);
		if (cropState == CropState.DOUBLE_STICKS) {
			return CROSS_STICKS;
		}
		if (level.getBlockEntity(pos) instanceof CropBlockEntity cbe) {
			if (!cbe.hasPlant() && !cbe.hasWeeds()) {
				return SINGLE_STICKS;
			}
			// shape is dependant of the plant and the weed
			return cropState.hasSticks() ? Shapes.join(SINGLE_STICKS, cbe.getShape(), BooleanOp.OR) : cbe.getShape();
		}
		return super.getShape(state, level, pos, context);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		CropState cropState = state.getValue(CROP_STATE);
		boolean collides = AgriCraftConfig.SPEC.isLoaded()
				? AgriCraftConfig.CROP_STICKS_COLLIDE.get()
				: AgriCraftConfig.CROP_STICKS_COLLIDE.getDefault();
		if (collides && cropState.hasSticks()) {
			return cropState == CropState.DOUBLE_STICKS ? CROSS_STICKS : SINGLE_STICKS;
		}
		return Shapes.empty();
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = this.defaultBlockState();
		if (context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.LAVA)) {
			state = state.setValue(LAVALOGGED, true);
		} else if (context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER)) {
			state = state.setValue(WATERLOGGED, true);
		}
		ItemStack stack = context.getItemInHand();
		if (stack.getItem() instanceof CropSticksItem) {
			state = this.blockStateCropStick(state, CropStickVariant.fromItem(stack));
		} else if (stack.getItem() instanceof AgriSeedItem) {
			if (!AgriCraftConfig.PLANT_OFF_CROP_STICKS.get()) {
				return null;
			}
			state = this.blockStatePlant(state);
		}
		return state;
	}

	public BlockState blockStateCropStick(BlockState state, CropStickVariant variant) {
		return state.setValue(STICK_VARIANT, variant);  // crop_state should already be single_sticks
	}

	public BlockState blockStatePlant(BlockState state) {
		return state.setValue(CROP_STATE, CropState.PLANT);  // stick_variant should already be wooden
	}

	@Override
	@NotNull
	public FluidState getFluidState(BlockState pState) {
		if (pState.getValue(LAVALOGGED)) {
			return Fluids.LAVA.getSource(false);
		}
		if (pState.getValue(WATERLOGGED)) {
			return Fluids.WATER.getSource(false);
		}
		return super.getFluidState(pState);
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CropBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		Optional<AgriCrop> optional = AgriApi.get().getCrop(level, pos);
		if (optional.isEmpty()) {
			return InteractionResult.FAIL;
		}
		AgriCrop crop = optional.get();
		if (crop.isCrossCropSticks()) {
			// remove crop sticks
			InteractionResult result = removeCropSticks(level, pos, state);
			if (result == InteractionResult.SUCCESS) {
				if (!player.isCreative()) {
					spawnItem(level, pos, CropStickVariant.toItem(state.getValue(STICK_VARIANT)));
				}
				return InteractionResult.CONSUME;
			}
		} else if (crop.hasPlant() && crop.canBeHarvested()) {
			// harvest
			crop.getHarvestProducts(itemStack -> spawnItem(level, pos, itemStack));
			crop.setGrowthStage(crop.getPlant().getGrowthStageAfterHarvest());
			crop.getPlant().onHarvest(crop, player);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.FAIL;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		Optional<AgriCrop> optional = AgriApi.get().getCrop(level, pos);
		if (optional.isEmpty()) {
			return InteractionResult.FAIL;
		}
		AgriCrop crop = optional.get();
		// plant pre logic
		if (crop.hasPlant()) {
			Optional<InteractionResult> result = crop.getPlant().onRightClickPre(crop, heldItem, player);
			if (result.isPresent()) {
				return result.get();
			}
		}
		// block logic
		InteractionResult result = rightClickLogic(heldItem, state, level, pos, player, hand, crop);
		// plant post logic
		if (crop.hasPlant()) {
			Optional<InteractionResult> override = crop.getPlant().onRightClickPost(crop, heldItem, player);
			if (override.isPresent()) {
				return override.get();
			}
		}
		return result;
	}

	protected InteractionResult rightClickLogic(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, AgriCrop crop) {
		// do nothing from off hand
		if (hand == InteractionHand.OFF_HAND/* || heldItem.isEmpty()*/) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		// TODO: @Ketheroth replace with item tag
		if (heldItem.is(AgriItems.CLIPPER.get()) || heldItem.is(AgriItems.IRON_RAKE.get()) || heldItem.is(AgriItems.WOODEN_RAKE.get()) || heldItem.is(AgriItems.TROWEL.get())) {
			return InteractionResult.PASS;
		}
		if (AgriApi.get().getFertilizerAdapter(heldItem).isPresent()) {
			if (!MysticalAgriculturePlugin.canFertilize(crop.hasPlant() ? crop.getPlantId() : null, heldItem)) {
				return InteractionResult.FAIL;
			}
			return AgriApi.get().getFertilizerAdapter(heldItem).get().valueOf(heldItem).map(fertilizer -> {
				if (crop.acceptsFertilizer(fertilizer)) {
					InteractionResult result = fertilizer.applyFertilizer(level, pos, crop, heldItem, level.getRandom(), player);
					if (result == InteractionResult.CONSUME || result == InteractionResult.SUCCESS) {
						crop.onApplyFertilizer(fertilizer, level.getRandom());
						if (crop.hasPlant()) {
							crop.getPlant().onFertilized(crop, heldItem, level.getRandom());
						}
					}
					return result;
				}
				return InteractionResult.CONSUME;
			}).orElse(InteractionResult.TRY_WITH_EMPTY_HAND);
		}
		// placement of crop sticks or creation of cross crop
		if (heldItem.getItem() instanceof CropSticksItem) {
			InteractionResult result = applyCropSticks(level, pos, state, CropStickVariant.fromItem(heldItem));
			if (result == InteractionResult.SUCCESS) {
				if (!player.isCreative()) {
					player.getItemInHand(hand).shrink(1);
				}
				return result;
			}
		}
		// planting from seed
		Optional<AgriGenome> genome = AgriApi.get().getGenomeAdapter(heldItem).flatMap(adapter -> adapter.valueOf(heldItem));
		if (genome.isPresent()) {
			if (!crop.isCrossCropSticks() && !crop.hasPlant()) {
				crop.plantGenome(genome.get());
				if (!player.isCreative()) {
					player.getItemInHand(hand).shrink(1);
				}
				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	@NotNull
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		if (level.getBlockEntity(pos) instanceof AgriCrop crop) {
			if (crop.hasPlant()) {
				// prioritize the plant if there is any
				return AgriSeedItem.toStack(crop.getGenome());
			}
			if (crop.hasCropSticks()) {
				return CropStickVariant.toItem(state.getValue(STICK_VARIANT));
			}
		}
		return super.getCloneItemStack(level, pos, state, includeData);
	}

	@Override
	protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
		this.spawnDestroyParticles(level, state, pos);
	}

	@Override
	public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(LAVALOGGED)) {
			ticks.scheduleTick(pos, Fluids.LAVA, Fluids.LAVA.getTickDelay(level));
		} else if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (!state.canSurvive(level, pos)) {
			if (level instanceof LevelAccessor accessor) {
				this.spawnDestroyParticles(accessor, state, pos);
			}
			if (state.getValue(LAVALOGGED)) {
				return Fluids.LAVA.defaultFluidState().createLegacyBlock();
			} else if (state.getValue(WATERLOGGED)) {
				return Fluids.WATER.defaultFluidState().createLegacyBlock();
			}
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return AgriApi.get().getSoil(level, pos.below(), level.registryAccess()).isPresent();
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return AgriApi.get().getCrop(level, pos).map(crop -> crop.hasPlant() && crop.isFertile() && !crop.isFullyGrown()).orElse(false);
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return AgriApi.get().getFertilizer(new ItemStack(Items.BONE_MEAL)).flatMap(fertilizer ->
				AgriApi.get().getCrop(level, pos).map(crop -> !crop.isFullyGrown() && crop.acceptsFertilizer(fertilizer))
		).orElse(false);
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		// transfert the right click with a bonemeal to the block entity
		AgriApi.get().getFertilizer(new ItemStack(Items.BONE_MEAL)).ifPresent(fertilizer ->
				AgriApi.get().getCrop(level, pos).ifPresent(crop -> fertilizer.applyFertilizer(level, pos, crop, new ItemStack(Items.BONE_MEAL), random, null))
		);
	}

	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(STICK_VARIANT) == CropStickVariant.WOODEN && level.getFluidState(pos).is(Fluids.LAVA)) {
			this.spawnDestroyParticles(level, state, pos);
			level.destroyBlock(pos, true);
			return;
		}
		AgriApi.get().getCrop(level, pos).ifPresent(agriCrop -> {
			if (agriCrop.hasPlant()) {
				agriCrop.getPlant().onRandomTick(agriCrop, random);
			}
			agriCrop.applyGrowthTick();
		});
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		AgriApi.get().getCrop(level, pos).ifPresent(crop -> {
			if (crop.hasPlant()) {
				crop.getPlant().spawnParticles(crop, random);
			}
		});
	}

	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		AgriApi.get().getCrop(level, pos).ifPresent(crop -> {
			if (crop.hasPlant()) {
				crop.getPlant().onBroken(crop, player);
			}
		});
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = new ArrayList<>();
		BlockEntity tile = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (tile == null) {
			return drops;
		}
		CropState cropState = state.getValue(CROP_STATE);
		if (tile instanceof AgriCrop crop) {
			if (cropState.hasSticks() && !(crop.hasWeeds() && AgriCraftConfig.WEEDS_DESTROY_CROP_STICKS.get())) {
				drops.add(CropStickVariant.toItem(state.getValue(STICK_VARIANT), cropState == CropState.DOUBLE_STICKS ? 2 : 1));
			}
			// ask the block entity for the harvest products
			crop.getHarvestProducts(drops::add);
			if (crop.hasPlant() && (crop.isFullyGrown() || !AgriCraftConfig.ONLY_MATURE_SEED_DROPS.get())) {
				double dropChance = crop.getPlant().getSeedDropChance(crop.getGrowthStage());
				double dropBonus = crop.getPlant().getBonusSeedDropChance(crop.getGrowthStage());
				if (dropChance + dropBonus * crop.getGrowthStage().index() >= params.getLevel().getRandom().nextDouble()) {
					drops.add(AgriSeedItem.toStack(crop.getGenome()));
				}
			}
		}
		return drops;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean moving) {
		super.entityInside(state, level, pos, entity, effects, moving);
		AgriApi.get().getCrop(level, pos).ifPresent(crop -> {
			if (crop.hasPlant()) {
				crop.getPlant().onEntityCollision(crop, entity);
			}
		});
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return state.getValue(CROP_STATE).hasPlant();
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return state.getValue(CROP_STATE).hasPlant();
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return this.getDirectSignal(state, level, pos, direction);
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return this.getDirectSignal(state, level, pos, direction);
	}

	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(CROP_STATE).hasPlant() ? AgriApi.get().getCrop(level, pos).map(crop -> {
			if (crop.getPlant() != null) {
				return crop.getPlant().getRedstonePower(crop);
			}
			return 0;
		}).orElse(0) : 0;
	}

	private void spawnDestroyParticles(LevelAccessor level, BlockState state, BlockPos pos) {
		if (level.isClientSide() && level.getBlockEntity(pos) instanceof AgriCrop crop) {
			// we handle the break particles ourselves to mimic the used model and spawn their particles instead of ours
			CropState cropState = state.getValue(CROP_STATE);
			if (cropState.hasSticks()) {
				ClientUtil.spawnParticlesForSticks(state.getValue(STICK_VARIANT), level, state, pos);
			}
			if (crop.hasPlant()) {
				String plantModelId = crop.getPlantId().toString().replace(":", ":crop/") + "_stage" + crop.getGrowthStage().index();
				ClientUtil.spawnParticlesForPlant(plantModelId, level, state, pos);
			}
		}
	}

}

package com.agricraft.agricraft.common.registry;

import com.agricraft.agricraft.common.block.CropStickVariant;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.common.item.ClipperItem;
import com.agricraft.agricraft.common.item.CropSticksItem;
import com.agricraft.agricraft.common.item.DebuggerItem;
import com.agricraft.agricraft.common.item.JournalItem;
import com.agricraft.agricraft.common.item.MagnifyingGlassItem;
import com.agricraft.agricraft.common.item.RakeItem;
import com.agricraft.agricraft.common.item.SeedBagItem;
import com.agricraft.agricraft.common.item.TrowelItem;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.ApiStatus;

import static com.agricraft.agricraft.common.registry.AgriRegistries.ITEMS;

public interface AgriItems {


	DeferredItem<DebuggerItem> DEBUGGER = ITEMS.registerItem("debugger", properties -> new DebuggerItem(properties));

	DeferredItem<JournalItem> JOURNAL = ITEMS.registerItem("journal", properties -> new JournalItem(properties.component(AgriDataComponents.JOURNAL_DATA.get(), new JournalItem.Data())));
	DeferredItem<BlockItem> SEED_ANALYZER = ITEMS.registerSimpleBlockItem(AgriBlocks.SEED_ANALYZER);
	DeferredItem<ClipperItem> CLIPPER = ITEMS.registerItem("clipper", properties -> new ClipperItem(properties.stacksTo(1)));
	DeferredItem<MagnifyingGlassItem> MAGNIFYING_GLASS = ITEMS.registerItem("magnifying_glass", properties -> new MagnifyingGlassItem(properties.component(AgriDataComponents.MAGNIFYING.get(), Unit.INSTANCE).stacksTo(1)));
	DeferredItem<RakeItem> WOODEN_RAKE = ITEMS.registerItem("wooden_rake", properties -> new RakeItem(properties, RakeItem.WOOD_LOGIC));
	DeferredItem<RakeItem> IRON_RAKE = ITEMS.registerItem("iron_rake", properties -> new RakeItem(properties, RakeItem.IRON_LOGIC));
	DeferredItem<TrowelItem> TROWEL = ITEMS.registerItem("trowel", properties -> new TrowelItem(properties.stacksTo(1)));
	DeferredItem<CropSticksItem> WOODEN_CROP_STICKS = ITEMS.registerItem("wooden_crop_sticks", properties -> new CropSticksItem(AgriBlocks.CROP.get(), properties, CropStickVariant.WOODEN));
	DeferredItem<CropSticksItem> IRON_CROP_STICKS = ITEMS.registerItem("iron_crop_sticks", properties -> new CropSticksItem(AgriBlocks.CROP.get(), properties, CropStickVariant.IRON));
	DeferredItem<CropSticksItem> OBSIDIAN_CROP_STICKS = ITEMS.registerItem("obsidian_crop_sticks", properties -> new CropSticksItem(AgriBlocks.CROP.get(), properties, CropStickVariant.OBSIDIAN));
	DeferredItem<SeedBagItem> SEED_BAG = ITEMS.registerItem("seed_bag", properties -> new SeedBagItem(properties.component(AgriDataComponents.SEED_BAG_DATA.get(), new SeedBagItem.Data()).stacksTo(1)));
	DeferredItem<AgriSeedItem> SEED = ITEMS.registerItem("seed", properties -> new AgriSeedItem(properties));

	DeferredItem<Item> COAL_PEBBLE = ITEMS.registerItem("coal_pebble", properties -> new Item(properties));
	DeferredItem<Item> COPPER_NUGGET = ITEMS.registerItem("copper_nugget", properties -> new Item(properties));
	DeferredItem<Item> DIAMOND_SHARD = ITEMS.registerItem("diamond_shard", properties -> new Item(properties));
	DeferredItem<Item> EMERALD_SHARD = ITEMS.registerItem("emerald_shard", properties -> new Item(properties));
	DeferredItem<Item> QUARTZ_SHARD = ITEMS.registerItem("quartz_shard", properties -> new Item(properties));
	DeferredItem<Item> NETHERITE_SLIVER = ITEMS.registerItem("netherite_sliver", properties -> new Item(properties.fireResistant()));
	DeferredItem<Item> AMATHYLLIS_PETAL = ITEMS.registerItem("amathyllis_petal", properties -> new Item(properties));

	static void addItemsToTabs(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CreativeModeTab.Output output) {
		if (itemDisplayParameters.hasPermissions()) {
			output.accept(DEBUGGER.get());
		}
		output.accept(JOURNAL.get().getDefaultInstance());
		output.accept(SEED_ANALYZER.get());
		output.accept(CLIPPER.get());
		output.accept(MAGNIFYING_GLASS.get());
		output.accept(WOODEN_RAKE.get());
		output.accept(IRON_RAKE.get());
		output.accept(TROWEL.get());
		output.accept(WOODEN_CROP_STICKS.get());
		output.accept(IRON_CROP_STICKS.get());
		output.accept(OBSIDIAN_CROP_STICKS.get());
		output.accept(SEED_BAG.get());

		output.accept(COAL_PEBBLE.get());
		output.accept(COPPER_NUGGET.get());
		output.accept(DIAMOND_SHARD.get());
		output.accept(EMERALD_SHARD.get());
		output.accept(QUARTZ_SHARD.get());
		output.accept(NETHERITE_SLIVER.get());
		output.accept(AMATHYLLIS_PETAL.get());
	}

	@ApiStatus.Internal
	static void register() {}

}

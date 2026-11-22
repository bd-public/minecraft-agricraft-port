package com.agricraft.agricraft.api;

import com.agricraft.agricraft.api.plant.AgriPlant;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.stream.Stream;

public class TagUtils {

	public static List<Item> items(ExtraCodecs.TagOrElementLocation tag) {
		if (!tag.tag()) {
			return List.of(BuiltInRegistries.ITEM.getValue(tag.id()));
		} else {
			return BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, tag.id()))
					.map(HolderSet.ListBacked::stream)
					.map(str -> str.map(Holder::value))
					.map(Stream::toList)
					.orElse(List.of());
		}
	}

	public static List<Block> blocks(ExtraCodecs.TagOrElementLocation tag) {
		if (!tag.tag()) {
			return List.of(BuiltInRegistries.BLOCK.getValue(tag.id()));
		} else {
			return BuiltInRegistries.BLOCK.get(TagKey.create(Registries.BLOCK, tag.id()))
					.map(HolderSet.ListBacked::stream)
					.map(str -> str.map(Holder::value))
					.map(Stream::toList)
					.orElse(List.of());
		}
	}

	public static List<Fluid> fluids(ExtraCodecs.TagOrElementLocation tag) {
		if (!tag.tag()) {
			return List.of(BuiltInRegistries.FLUID.getValue(tag.id()));
		} else {
			return BuiltInRegistries.FLUID.get(TagKey.create(Registries.FLUID, tag.id()))
					.map(HolderSet.ListBacked::stream)
					.map(str -> str.map(Holder::value))
					.map(Stream::toList)
					.orElse(List.of());
		}
	}

	public static Stream<Identifier> plants(ExtraCodecs.TagOrElementLocation tag) {
		if (!tag.tag()) {
			return Stream.of(tag.id());
		} else {
			return AgriApi.get().getPlantRegistry().flatMap(registry ->
							registry.get(TagKey.create(AgriPlant.REGISTRY_KEY, tag.id()))
									.map(named -> named.stream().map(holder -> registry.getKey(holder.value())))
					)
					.orElse(Stream.empty());
		}
	}

}

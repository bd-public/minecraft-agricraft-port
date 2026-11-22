package com.agricraft.agricraft.common.registry;

import com.agricraft.agricraft.common.item.crafting.MagnifyingHelmetRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.ApiStatus;

import static com.agricraft.agricraft.common.registry.AgriRegistries.RECIPE_SERIALIZERS;

public interface AgriRecipeSerializers {

	DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MagnifyingHelmetRecipe>> MAGNIFYING_HELMET = RECIPE_SERIALIZERS.register("magnifying_helmet", () -> new RecipeSerializer<>(MapCodec.unit(MagnifyingHelmetRecipe::new), StreamCodec.unit(new MagnifyingHelmetRecipe())));

	@ApiStatus.Internal
	static void register() {}

}

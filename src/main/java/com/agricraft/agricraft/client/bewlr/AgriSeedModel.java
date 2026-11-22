package com.agricraft.agricraft.client.bewlr;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.agricraft.agricraft.compat.mysticalagriculture.MysticalAgriculturePlugin;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;

public class AgriSeedModel implements ItemModel {
    private static final Identifier FALLBACK = Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "seed/unknown");

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext display, ClientLevel level, ItemOwner owner, int seed) {
        Identifier species = Identifier.tryParse(AgriSeedItem.getSpecies(stack));
        ItemStack nativeSeed = MysticalAgriculturePlugin.visualSeed(species);
        if (!nativeSeed.isEmpty()) {
            resolver.appendItemLayers(state, nativeSeed, display, level, owner, seed);
            return;
        }
        var manager = Minecraft.getInstance().getModelManager();
        Identifier model = species == null ? FALLBACK : species.withPrefix("seed/");
        if (model != FALLBACK && Minecraft.getInstance().getResourceManager().getResource(model.withPath(path -> "items/" + path + ".json")).isEmpty()) {
            model = FALLBACK;
        }
        manager.getItemModel(model).update(state, stack, resolver, display, level, owner, seed);
    }

    public static final class Unbaked implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
            return new AgriSeedModel();
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
        }
    }
}

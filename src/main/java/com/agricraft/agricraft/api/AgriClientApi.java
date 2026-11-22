package com.agricraft.agricraft.api;

import com.agricraft.agricraft.api.tools.journal.JournalPage;
import com.agricraft.agricraft.api.tools.journal.JournalPageDrawer;
import com.agricraft.agricraft.api.tools.magnifying.MagnifyingInspector;
import com.agricraft.agricraft.client.AgriCraftClient;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface AgriClientApi {

	String UNKNOWN_SEED = "agricraft:unknown";
	Identifier UNKNOWN_PLANT = Identifier.fromNamespaceAndPath(AgriApi.MOD_ID, "crop/unknown");

	static AgriClientApi get() {
		return InstanceHolder.instance;
	}

	static void set(AgriClientApi instance) {
		InstanceHolder.instance = instance;
	}

	<T extends JournalPage> void registerJournalPageDrawer(Identifier id, JournalPageDrawer<T> drawer);

	<T extends JournalPage> JournalPageDrawer<T> getJournalPageDrawer(T page);

	/**
	 * Register a predicate to allow the magnifying glass overlay to render
	 *
	 * @param predicate the predicate
	 */
	void registerMagnifyingAllowingPredicate(Predicate<Player> predicate);

	Collection<Predicate<Player>> getMagnifyingAllowingPredicates();

	/**
	 * Register a magnifying inspector
	 *
	 * @param inspector the inspector to add
	 */
	void registerMagnifyingInspector(MagnifyingInspector inspector);

	Stream<MagnifyingInspector> getMagnifyingInspectors();



    default BlockStateModel getPlantModel(Identifier plantId, int stage) {
        return getPlantModel(plantId.toString(), stage);
    }

    default BlockStateModel getPlantModel(String plantId, int stage) {
        return getStageModel(Identifier.tryParse(plantId), "crop/", stage);
    }

    default BlockStateModel getWeedModel(String weedId, int stage) {
        return getStageModel(Identifier.tryParse(weedId), "weed/", stage);
    }

    private static BlockStateModel getStageModel(Identifier species, String directory, int stage) {
        if (species != null) {
            BlockStateModel model = AgriCraftClient.getBlockModel(species.withPath(path -> directory + path + "_stage" + stage));
            if (model != null) {
                return model;
            }
        }
        return AgriCraftClient.getBlockModel(UNKNOWN_PLANT);
    }

    @ApiStatus.Internal
    final class InstanceHolder {
        private static AgriClientApi instance;
        private InstanceHolder() {}
    }
}

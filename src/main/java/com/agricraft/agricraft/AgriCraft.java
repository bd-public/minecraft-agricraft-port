package com.agricraft.agricraft;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.config.AgriCraftConfig;
import com.agricraft.agricraft.common.commands.DumpRegistriesCommand;
import com.agricraft.agricraft.common.commands.GiveSeedCommand;
import com.agricraft.agricraft.common.handler.VanillaSeedConversion;
import com.agricraft.agricraft.common.registry.AgriRegistries;
import com.agricraft.agricraft.compat.mysticalagriculture.MysticalAgriculturePlugin;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.resource.JarContentsPackResources;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(AgriApi.MOD_ID)
public class AgriCraft {

	public static final Logger LOGGER = LogUtils.getLogger();

	public AgriCraft(IEventBus bus, ModContainer modContainer) {
		AgriRegistries.register(bus);
		AgriApi.set(new AgriApiImpl());
		LOGGER.info("Intializing API for " + AgriApi.MOD_ID);
		bus.addListener(AgriCraft::onAddPackFinders);
		NeoForge.EVENT_BUS.addListener(AgriCraft::onRegisterCommands);
		NeoForge.EVENT_BUS.addListener(AgriCraft::onRightClick);
		modContainer.registerConfig(ModConfig.Type.COMMON, AgriCraftConfig.SPEC);
		if (FMLEnvironment.getDist().isClient()) {
			modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		}
	}


	public static void onRegisterCommands(RegisterCommandsEvent event) {
		GiveSeedCommand.register(event.getDispatcher(), event.getBuildContext());
		DumpRegistriesCommand.register(event.getDispatcher(), event.getBuildContext());
	}

	public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
		if (VanillaSeedConversion.onRightClick(event.getEntity(), event.getHand(), event.getPos(), event.getHitVec())) {
			event.setUseItem(TriState.FALSE);
			event.setCanceled(true);
		}
	}


    public static void onAddPackFinders(AddPackFindersEvent event) {
        PackType packType = event.getPackType();
        MysticalAgriculturePlugin.addPack(event);
        String directory = packType == PackType.SERVER_DATA ? "datapacks" : "resourcepacks";
        IModFileInfo agricraft = ModList.get().getModFileById(AgriApi.MOD_ID);
        for (IModInfo mod : ModList.get().getMods()) {
            String modId = mod.getModId();
            if (modId.equals(MysticalAgriculturePlugin.MOD_ID)) {
                continue;
            }
            String prefix = directory + "/" + modId;
            var contents = agricraft.getFile().getContents();
            if (contents.get(prefix + "/pack.mcmeta") == null) {
                continue;
            }
            String id = "mod/agricraft_" + directory + "_" + modId;
            var location = new PackLocationInfo(id, Component.translatable("agricraft." + directory + "." + modId),
                    PackSource.BUILT_IN, Optional.of(new KnownPack(AgriApi.MOD_ID, id, agricraft.versionString())));
            var resources = new JarContentsPackResources.JarContentsResourcesSupplier(contents, prefix);
            var selection = new PackSelectionConfig(AgriCraftConfig.REGISTER_PACKS_BY_DEFAULT.get(), Pack.Position.TOP, false);
            Pack pack = Pack.readMetaAndCreate(location, resources, packType, selection);
            if (pack != null) {
                event.addRepositorySource(consumer -> consumer.accept(pack));
            }
        }
    }
}

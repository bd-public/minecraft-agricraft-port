package com.agricraft.agricraft.compat.mysticalagriculture;

import com.agricraft.agricraft.AgriCraft;
import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriProduct;
import com.agricraft.agricraft.api.codecs.AgriRequirement;
import com.agricraft.agricraft.api.codecs.AgriSeed;
import com.agricraft.agricraft.api.codecs.AgriSoil;
import com.agricraft.agricraft.api.codecs.AgriSoilCondition;
import com.agricraft.agricraft.api.codecs.AgriSoilVariant;
import com.agricraft.agricraft.api.config.AgriCraftConfig;
import com.agricraft.agricraft.api.fertilizer.AgriFertilizer;
import com.agricraft.agricraft.api.fertilizer.AgriFertilizerParticle;
import com.agricraft.agricraft.api.fertilizer.AgriFertilizerVariant;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.blakebr0.mysticalagriculture.api.MysticalAgricultureAPI;
import com.blakebr0.mysticalagriculture.api.crop.Crop;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MysticalAgriculturePlugin {
    public static final String MOD_ID = "mysticalagriculture";
    public static final Identifier FERTILIZER = id("mystical_fertilizer");
    private static final Identifier INFERIUM = id("inferium");

    private MysticalAgriculturePlugin() {
    }

    public static void addPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !isLoaded()) {
            return;
        }
        var location = new PackLocationInfo("mod/agricraft_datapacks_mysticalagriculture",
                Component.literal("AgriCraft ? Mystical Agriculture"), PackSource.BUILT_IN, Optional.empty());
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(location, new Pack.ResourcesSupplier() {
                @Override
                public PackResources openPrimary(PackLocationInfo info) {
                    return new CropPack(info, Map.of());
                }

                @Override
                public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                    return new CropPack(info, Integration.resources());
                }
            }, PackType.SERVER_DATA, new PackSelectionConfig(AgriCraftConfig.REGISTER_PACKS_BY_DEFAULT.get(), Pack.Position.BOTTOM, false));
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    public static ItemStack visualSeed(Identifier species) {
        return isLoaded() ? Integration.visualSeed(species) : ItemStack.EMPTY;
    }

    public static Component cropName(Identifier species, Component fallback) {
        return isLoaded() ? Integration.cropName(species, fallback) : fallback;
    }

    public static @Nullable BlockState cropState(Identifier species, int stage, int totalStages) {
        return isLoaded() ? Integration.cropState(species, stage, totalStages) : null;
    }

    public static boolean canGrow(Identifier species, Level level, BlockPos pos) {
        return !isLoaded() || Integration.canGrow(species, level, pos);
    }

    public static boolean canFertilize(Identifier species, ItemStack stack) {
        return !isLoaded() || Integration.canFertilize(species, stack);
    }

    private static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // Keep the optional mod's classes out of the common entry points.
    private static final class Integration {
        private static Map<Identifier, byte[]> resources() {
            var registry = MysticalAgricultureAPI.getCropRegistry();
            Map<Identifier, byte[]> files = new HashMap<>();
            for (Crop crop : registry.getCrops()) {
                if (!crop.isEnabled() || crop.getSeedsItem() == null || crop.getEssenceItem() == null) {
                    continue;
                }
                Identifier seed = BuiltInRegistries.ITEM.getKey(crop.getSeedsItem());
                Identifier essence = BuiltInRegistries.ITEM.getKey(crop.getEssenceItem());
                AgriPlant plant = new AgriPlant.Builder().mods(MOD_ID)
                        .seeds(AgriSeed.builder().item(seed.toString()).build())
                        .products(AgriProduct.builder().item(essence.toString()).count(1, 1, 1).build())
                        .stages16().harvest(3).cloneable(false).chances(0.65, 0.025, 0.1)
                        .requirement(AgriRequirement.builder()
                                .humidity(AgriSoilCondition.Humidity.DAMP, AgriSoilCondition.Type.EQUAL_OR_HIGHER, 0.2)
                                .acidity(AgriSoilCondition.Acidity.SLIGHTLY_ACIDIC, AgriSoilCondition.Type.EQUAL, 0.2)
                                .nutrients(AgriSoilCondition.Nutrients.HIGH, AgriSoilCondition.Type.EQUAL_OR_HIGHER, 0.2)
                                .build())
                        .build();
                add(files, "plant", crop.getId(), AgriPlant.CODEC, plant);
            }
            registry.getTiers().forEach(tier -> {
                Block farmland = tier.getFarmlandBlock();
                if (farmland == null) {
                    return;
                }
                Identifier farmlandId = BuiltInRegistries.BLOCK.getKey(farmland);
                AgriSoil soil = AgriSoil.builder().mods(MOD_ID)
                        .variants(AgriSoilVariant.builder().block(farmlandId.toString()).build())
                        .humidity(AgriSoilCondition.Humidity.WET)
                        .acidity(AgriSoilCondition.Acidity.SLIGHTLY_ACIDIC)
                        .nutrients(AgriSoilCondition.Nutrients.HIGH).build();
                add(files, "soil", farmlandId, AgriSoil.CODEC, soil);
            });
            AgriFertilizer fertilizer = new AgriFertilizer.Builder().mods(MOD_ID)
                    .variants(new AgriFertilizerVariant.Builder().item(FERTILIZER.toString()).build())
                    .potency(5).triggerMutation(false).triggerWeeds(false)
                    .particles(new AgriFertilizerParticle("minecraft:happy_villager", 0.6, 0.4, 0.6, 2, java.util.List.of("positive")))
                    .build();
            add(files, "fertilizer", FERTILIZER, AgriFertilizer.CODEC, fertilizer);
            return Map.copyOf(files);
        }

        private static <T> void add(Map<Identifier, byte[]> files, String registry, Identifier id, Codec<T> codec, T value) {
            codec.encodeStart(JsonOps.INSTANCE, value)
                    .resultOrPartial(error -> AgriCraft.LOGGER.error("Could not encode Mystical Agriculture {} {}: {}", registry, id, error))
                    .ifPresent(json -> files.put(id.withPath(path -> AgriApi.MOD_ID + "/" + registry + "/" + path + ".json"),
                            json.toString().getBytes(StandardCharsets.UTF_8)));
        }

        private static ItemStack visualSeed(Identifier species) {
            Crop crop = find(species);
            return crop == null || crop.getSeedsItem() == null ? ItemStack.EMPTY : new ItemStack(crop.getSeedsItem());
        }

        private static Component cropName(Identifier species, Component fallback) {
            Crop crop = find(species);
            return crop == null ? fallback : crop.getDisplayName();
        }

        private static @Nullable BlockState cropState(Identifier species, int stage, int totalStages) {
            Crop crop = find(species);
            if (crop == null || crop.getCropBlock() == null) {
                return null;
            }
            var block = crop.getCropBlock();
            int age = totalStages > 1 ? stage * block.getMaxAge() / (totalStages - 1) : block.getMaxAge();
            return block.getStateForAge(age);
        }

        private static boolean canGrow(Identifier species, Level level, BlockPos pos) {
            Crop crop = find(species);
            if (crop == null) {
                return true;
            }
            Block crux = crop.getCruxBlock();
            if (crux != null && !level.getBlockState(pos.below(2)).is(crux)) {
                return false;
            }
            if (!INFERIUM.equals(species) && MysticalAgricultureAPI.getConfigValues().isRequiresEffectiveFarmlandEnabled()
                    && !crop.getTier().isEffectiveFarmland(level.getBlockState(pos.below()).getBlock())) {
                return false;
            }
            return crop.getRequiredBiomes().isEmpty()
                    || level.getBiome(pos).unwrapKey().map(key -> crop.getRequiredBiomes().contains(key.identifier())).orElse(false);
        }

        private static boolean canFertilize(Identifier species, ItemStack stack) {
            Crop crop = find(species);
            if (crop == null) {
                return !stack.is(BuiltInRegistries.ITEM.getValue(FERTILIZER));
            }
            if (stack.is(net.minecraft.world.item.Items.BONE_MEAL)) {
                return false;
            }
            return !stack.is(BuiltInRegistries.ITEM.getValue(FERTILIZER)) || crop.getTier().isFertilizable();
        }

        private static @Nullable Crop find(Identifier id) {
            var registry = MysticalAgricultureAPI.getCropRegistry();
            return registry == null || id == null ? null : registry.getCropById(id);
        }
    }

    private static final class CropPack extends AbstractPackResources {
        private final Map<Identifier, byte[]> files;
        private final Set<String> namespaces;

        private CropPack(PackLocationInfo location, Map<Identifier, byte[]> files) {
            super(location);
            this.files = files;
            this.namespaces = files.keySet().stream().map(Identifier::getNamespace).collect(java.util.stream.Collectors.toUnmodifiableSet());
        }

        @Override
        public @Nullable IoSupplier<InputStream> getRootResource(String... path) {
            if (path.length != 1 || !PACK_META.equals(path[0])) {
                return null;
            }
            var version = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
            var metadata = new PackMetadataSection(location().title(), new InclusiveRange<>(version));
            JsonObject root = new JsonObject();
            root.add("pack", PackMetadataSection.SERVER_TYPE.codec().encodeStart(JsonOps.INSTANCE, metadata).getOrThrow());
            return () -> new ByteArrayInputStream(root.toString().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier location) {
            byte[] resource = type == PackType.SERVER_DATA ? files.get(location) : null;
            return resource == null ? null : () -> new ByteArrayInputStream(resource);
        }

        @Override
        public void listResources(PackType type, String namespace, String directory, ResourceOutput output) {
            if (type == PackType.SERVER_DATA) {
                String prefix = directory.isEmpty() ? "" : directory + "/";
                files.forEach((id, bytes) -> {
                    if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                        output.accept(id, () -> new ByteArrayInputStream(bytes));
                    }
                });
            }
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return type == PackType.SERVER_DATA ? namespaces : Set.of();
        }

        @Override
        public void close() {
        }
    }
}

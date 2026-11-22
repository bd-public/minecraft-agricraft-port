package com.agricraft.agricraft;

import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.agricraft.agricraft.common.registry.AgriBlocks;
import com.blakebr0.mysticalagriculture.block.HarvesterBlock;
import com.blakebr0.mysticalagriculture.init.ModBlocks;
import com.blakebr0.mysticalagriculture.lib.ModCrops;
import com.blakebr0.mysticalagriculture.tileentity.HarvesterTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

final class MysticalHarvesterGameTest {
    private MysticalHarvesterGameTest() {
    }

    static void run(GameTestHelper helper) {
        var level = helper.getLevel();
        var machinePos = new BlockPos(1, 2, 2);
        var cropPos = new BlockPos(2, 2, 1);
        helper.setBlock(machinePos, ModBlocks.HARVESTER.get().defaultBlockState().setValue(HarvesterBlock.FACING, Direction.EAST));
        helper.setBlock(cropPos.below(), Blocks.FARMLAND);
        helper.setBlock(cropPos, AgriBlocks.CROP.get());

        var machine = helper.getBlockEntity(machinePos, HarvesterTileEntity.class);
        var crop = helper.getBlockEntity(cropPos, CropBlockEntity.class);
        var genome = new AgriGenome(ModCrops.INFERIUM.getId());
        crop.plantGenome(genome);
        int stages = crop.getGrowthStage().total();
        crop.setGrowthStage(new AgriGrowthStage(stages - 1, stages));

        var absoluteMachinePos = helper.absolutePos(machinePos);
        HarvesterTileEntity.tick(level, absoluteMachinePos, helper.getBlockState(machinePos), machine);
        helper.assertTrue(crop.canBeHarvested(), "The unpowered Harvester harvested a crop");

        int energy = 1000;
        machine.getEnergy().set(energy);
        for (int i = 0; i < machine.getOperationTime() * 2 && crop.canBeHarvested(); i++) {
            HarvesterTileEntity.tick(level, absoluteMachinePos, helper.getBlockState(machinePos), machine);
        }
        var expected = ModCrops.INFERIUM.getEssenceItem();
        var harvested = machine.getInventory().getResource(1).toStack();
        helper.assertTrue(harvested.is(expected), "The Harvester did not collect Inferium Essence");
        helper.assertTrue(machine.getEnergy().getAmountAsInt() == energy - machine.getFuelUsage(), "Unexpected Harvester fuel cost");
        helper.assertTrue(crop.hasPlant() && crop.getGenome() == genome && crop.getGrowthStage().index() == crop.getPlant().getGrowthStageAfterHarvest().index(),
                "The Harvester must retain the genome and reset the crop after harvest");
        helper.succeed();
    }
}

package com.agricraft.agricraft.mixin;

import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.common.block.CropBlock;
import com.blakebr0.cucumber.energy.CEnergyStorage;
import com.blakebr0.mysticalagriculture.tileentity.HarvesterTileEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "com.blakebr0.mysticalagriculture.tileentity.HarvesterTileEntity", remap = false)
public abstract class MysticalHarvesterMixin {
    @Shadow @Final private CEnergyStorage energy;

    @Shadow public abstract int getFuelUsage();

    @Shadow private void addItemToInventory(ItemStack stack, Level level, BlockPos pos) {
        throw new AssertionError();
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;")
    )
    private static BlockState harvestAgriCraftCrop(Level level, BlockPos cropPos, Operation<BlockState> original,
                                                   Level tickLevel, BlockPos machinePos, BlockState machineState, HarvesterTileEntity machine) {
        BlockState state = original.call(level, cropPos);
        if (state.getBlock() instanceof CropBlock
                && level.getBlockEntity(cropPos) instanceof AgriCrop crop && crop.hasPlant() && crop.canBeHarvested()) {
            ((MysticalHarvesterMixin) (Object) machine).harvest(crop, level, cropPos);
        }
        return state;
    }

    private void harvest(AgriCrop crop, Level level, BlockPos pos) {
        try (var transaction = Transaction.openRoot()) {
            int fuel = this.getFuelUsage();
            if (this.energy.extract(fuel, transaction) != fuel) {
                return;
            }
            transaction.commit();
        }
        List<ItemStack> products = new ArrayList<>();
        crop.getHarvestProducts(products::add);
        crop.setGrowthStage(crop.getPlant().getGrowthStageAfterHarvest());
        crop.getPlant().onHarvest(crop, null);
        products.forEach(stack -> this.addItemToInventory(stack, level, pos));
    }
}

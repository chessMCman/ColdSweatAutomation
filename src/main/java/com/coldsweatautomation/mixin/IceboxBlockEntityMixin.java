package com.coldsweatautomation.mixin;

import com.coldsweatautomation.ColdSweatAutomation;
import com.coldsweatautomation.container.BigIceboxContainer;
import com.momosoftworks.coldsweat.common.blockentity.IceboxBlockEntity;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Expands the Icebox from 9 food slots to 108 (plus the fuel slot, for 109 total).
 */
@Mixin(IceboxBlockEntity.class)
public abstract class IceboxBlockEntityMixin
{
    private static final int[] FOOD_SLOTS = new int[BigIceboxContainer.FOOD_SLOTS];
    static
    {
        for (int i = 0; i < FOOD_SLOTS.length; i++) FOOD_SLOTS[i] = i + BigIceboxContainer.FIRST_FOOD_SLOT;
    }

    /**
     * 1 fuel slot (index 0) + 108 food slots (indices 1..108).
     */
    @Overwrite
    public int getContainerSize()
    {   return BigIceboxContainer.FIRST_FOOD_SLOT + BigIceboxContainer.FOOD_SLOTS;
    }

    /**
     * Expose the food slots on the top/bottom (so hoppers and the auto-generated sided handler see
     * all 108 slots); the fuel slot remains on the horizontal faces.
     */
    @Overwrite
    public int[] getSlotsForFace(Direction side)
    {   return side.getAxis() == Direction.Axis.Y ? FOOD_SLOTS : new int[]{0};
    }

    /**
     * Cool filled waterskins in every food slot, not just slots 1..9.
     */
    @ModifyConstant(
        method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
        constant = @Constant(intValue = 10),
        remap = false
    )
    private int csa$foodLoopBound(int original)
    {   return BigIceboxContainer.FIRST_FOOD_SLOT + BigIceboxContainer.FOOD_SLOTS;
    }

    /**
     * Open the expanded 108-slot container instead of the vanilla 9-slot one.
     */
    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    private void csa$createMenu(int id, Inventory playerInv, CallbackInfoReturnable<AbstractContainerMenu> cir)
    {   cir.setReturnValue(new BigIceboxContainer(id, playerInv, (IceboxBlockEntity) (Object) this));
    }

    /**
     * Cold Sweat's own tick only cools slots 1..9; sweep the remaining expanded food slots (10..108)
     * with the same per-item rule so every slot cools down consistently.
     */
    @Inject(
        method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
        at = @At("RETURN"),
        remap = false
    )
    private void csa$coolExpandedFoodSlots(Level level, BlockState state, BlockPos pos, CallbackInfo ci)
    {
        if (level.isClientSide()) return;
        IceboxBlockEntity self = (IceboxBlockEntity) (Object) this;
        if (self.ticksExisted % 20 != 0) return;
        boolean changed = false;
        for (int slot = 1; slot < BigIceboxContainer.FIRST_FOOD_SLOT + BigIceboxContainer.FOOD_SLOTS; slot++)
        {
            ItemStack stack = self.getItem(slot);
            if (stack.isEmpty()) continue;

            double temp = stack.getOrDefault(ModItemComponents.WATER_TEMPERATURE.value(), 0d);
            if (temp <= -100.0d) continue;

            double cooled = CSMath.roundDownNearest(temp - 1.0d, 1.0d);
            cooled = Math.max(cooled, -100.0d);
            stack.set(ModItemComponents.WATER_TEMPERATURE.value(), cooled);
            self.setItem(slot, stack);
            changed = true;
        }
        double _dbgMax = Double.NaN;
        for (int _t = 1; _t < self.getContainerSize(); _t++) { if (self.getItem(_t).isEmpty()) continue; double _x = self.getItem(_t).getOrDefault(ModItemComponents.WATER_TEMPERATURE.value(), 0d); if (Double.isNaN(_dbgMax) || _x > _dbgMax) _dbgMax = _x; }
        if (changed)
        {
            self.setChanged();
            com.momosoftworks.coldsweat.util.world.WorldHelper.syncBlockEntityData(self);
        }
        // no debug logging
    }
}

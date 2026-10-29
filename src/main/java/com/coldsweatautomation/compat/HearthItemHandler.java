package com.coldsweatautomation.compat;

import com.momosoftworks.coldsweat.common.blockentity.BoilerBlockEntity;
import com.momosoftworks.coldsweat.common.blockentity.HearthBlockEntity;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.init.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * An {@code IItemHandler} wrapping a Cold Sweat hearth-like block entity ({@code HearthBlockEntity},
 * {@code BoilerBlockEntity} or {@code IceboxBlockEntity}).
 *
 * <p>Insertion and extraction are delegated to {@code WorldlyContainer#canPlaceItemThroughFace} and
 * {@code WorldlyContainer#canTakeItemThroughFace}, so the existing per-block validation rules are
 * preserved:</p>
 * <ul>
 *   <li>Slot 0 (fuel): only valid fuel items may be inserted ({@code getItemFuel(...) != 0}).</li>
 *   <li>Slots 1..N (products): accept waterskins/drinkables (boiler) or food (icebox), and can be
 *       extracted once processed.</li>
 * </ul>
 *
 * <p>Every side sees the full container, which is what lets AE2 and Create push fuel in and pull
 * products out from whichever face is convenient.</p>
 */
public class HearthItemHandler implements IItemHandler
{
    /** The temperature a filled waterskin reaches inside a boiler. */
    private static final double BOILER_MAX_TEMPERATURE = 50;

    private final WorldlyContainer container;

    public HearthItemHandler(WorldlyContainer container)
    {   this.container = container;
    }

    @Override
    public int getSlots()
    {   return this.container.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {   return this.container.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot < 0 || slot >= this.getSlots()) return stack;
        if (!this.container.canPlaceItemThroughFace(slot, stack, null)) return stack;

        ItemStack existing = this.container.getItem(slot);
        int limit = Math.min(this.container.getMaxStackSize(), stack.getMaxStackSize());

        if (!existing.isEmpty())
        {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) return stack;
            int space = limit - existing.getCount();
            if (space <= 0) return stack;

            int move = Math.min(space, stack.getCount());
            if (!simulate)
            {
                existing.grow(move);
                this.container.setItem(slot, existing);
                this.container.setChanged();
                if (slot == 0 && this.container instanceof HearthBlockEntity hb) hb.checkForFuel();
                this.container.setChanged();
                if (slot == 0 && this.container instanceof HearthBlockEntity hb) hb.checkForFuel();
            }
            ItemStack remaining = stack.copy();
            remaining.shrink(move);
            return remaining;
        }

        int move = Math.min(limit, stack.getCount());
        if (!simulate)
        {
            ItemStack put = stack.copy();
            put.setCount(move);
            this.container.setItem(slot, put);
                this.container.setChanged();
                if (slot == 0 && this.container instanceof HearthBlockEntity hb) hb.checkForFuel();
            this.container.setChanged();
        }
        ItemStack remaining = stack.copy();
        remaining.shrink(move);
        return remaining;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount <= 0) return ItemStack.EMPTY;
        if (slot < 0 || slot >= this.getSlots()) return ItemStack.EMPTY;

        ItemStack existing = this.container.getItem(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        if (!this.container.canTakeItemThroughFace(slot, existing, Direction.NORTH)) return ItemStack.EMPTY;
        // Only hand out products that are actually finished, so a bus doesn't pull a half-heated
        // waterskin out of the boiler before it has been warmed up.
        if (!this.isFinished(existing)) return ItemStack.EMPTY;

        int move = Math.min(amount, existing.getCount());
        if (!simulate)
        {
            ItemStack remaining = existing.copy();
            remaining.shrink(move);
            this.container.setItem(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
            this.container.setChanged();
            this.container.setChanged();
        }
        ItemStack result = existing.copy();
        result.setCount(move);
        return result;
    }

    /**
     * Whether the given stack has finished being processed.
     */
    private boolean isFinished(ItemStack stack)
    {
        if (this.container instanceof BoilerBlockEntity && stack.is(ModItems.FILLED_WATERSKIN))
        {
            double temperature = stack.getOrDefault(ModItemComponents.WATER_TEMPERATURE, 0d);
            return temperature >= BOILER_MAX_TEMPERATURE;
        }
        return true;
    }

    @Override
    public int getSlotLimit(int slot)
    {   return this.container.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {   return this.container.canPlaceItemThroughFace(slot, stack, null);
    }
}

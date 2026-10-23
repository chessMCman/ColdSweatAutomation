package com.coldsweatautomation.container;

import com.coldsweatautomation.core.ModMenus;
import com.momosoftworks.coldsweat.common.blockentity.IceboxBlockEntity;
import com.momosoftworks.coldsweat.data.tag.ModItemTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

/**
 * Icebox menu with 108 food slots (a 9x12 grid) instead of the vanilla 9.
 *
 * <p>It uses its own {@link com.coldsweatautomation.core.ModMenus#BIG_ICEBOX} menu type rather than
 * Cold Sweat's icebox menu, because NeoForge's {@code RegisterMenuScreensEvent} refuses duplicate
 * registrations - and the vanilla icebox screen can only ever be 176x172, which is far too small
 * for 108 slots.</p>
 */
public class BigIceboxContainer extends AbstractContainerMenu
{
    public static final int FOOD_SLOTS = 108;

    /** Columns/rows of the food grid (12 x 9 = 108). */
    public static final int GRID_COLUMNS = 12;
    public static final int GRID_ROWS = 9;
    /** Size of one cell (standard vanilla slot spacing). */
    public static final int CELL = 18;
    /** Origin of the food grid. */
    public static final int GRID_X = 8;
    public static final int GRID_Y = 16;
    /** Fuel slot + gauge, centered below the food grid. */
    public static final int FUEL_X = 99;
    public static final int FUEL_Y = 180;
    /** Origin of the player inventory block (centered, 3 rows + hotbar below). */
    public static final int INV_X = 35;
    public static final int INV_Y = 210;

    /** Index of the fuel slot. */
    public static final int FUEL_SLOT = 0;
    /** First food slot index. */
    public static final int FIRST_FOOD_SLOT = 1;
    /** First player-inventory slot index. */
    public static final int PLAYER_START = FIRST_FOOD_SLOT + FOOD_SLOTS;
    /** First hotbar slot index. */
    public static final int HOTBAR_START = PLAYER_START + 27;
    /** Total slot count. */
    public static final int TOTAL_SLOTS = HOTBAR_START + 9;

    public final IceboxBlockEntity te;

    public BigIceboxContainer(final int windowId, final Inventory playerInv, final IceboxBlockEntity te)
    {
        super(ModMenus.BIG_ICEBOX.get(), windowId);
        this.te = te;

        // Fuel slot (below the food grid, next to the fuel gauge)
        this.addSlot(new Slot(te, FUEL_SLOT, FUEL_X, FUEL_Y)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {   return te.getItemFuel(stack) > 0;
            }
        });

        // 108 food slots, arranged in a 18-wide x 6-tall grid
        for (int s = FIRST_FOOD_SLOT; s < FIRST_FOOD_SLOT + FOOD_SLOTS; s++)
        {
            int k = s - FIRST_FOOD_SLOT;
            this.addSlot(new Slot(te, s, GRID_X + (k % GRID_COLUMNS) * CELL, GRID_Y + (k / GRID_COLUMNS) * CELL)
            {
                @Override
                public boolean mayPlace(ItemStack stack)
                {   return stack.is(ModItemTags.ICEBOX_VALID);
                }
            });
        }

        // Main player inventory (27 slots)
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {   this.addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * CELL, INV_Y + row * CELL));
            }
        }

        // Player hotbar (9 slots)
        for (int col = 0; col < 9; col++)
        {   this.addSlot(new Slot(playerInv, col, INV_X + col * CELL, INV_Y + 56));
        }
        // Grid: 12 x 9 = 108 slots
    }

    public BigIceboxContainer(final int windowId, final Inventory playerInv, final FriendlyByteBuf data)
    {   this(windowId, playerInv, getTileEntity(playerInv, data));
    }

    public int getFuel()
    {   return this.te.getFuel();
    }

    private static IceboxBlockEntity getTileEntity(final Inventory playerInv, final FriendlyByteBuf data)
    {
        Objects.requireNonNull(playerInv, "Player inventory cannot be null");
        Objects.requireNonNull(data, "PacketBuffer inventory cannot be null");
        final BlockEntity te = playerInv.player.level().getBlockEntity(data.readBlockPos());
        if (te instanceof IceboxBlockEntity)
        {   return (IceboxBlockEntity) te;
        }
        throw new IllegalStateException("Tile Entity is not correct");
    }

    @Override
    public boolean stillValid(Player player)
    {   return player.distanceToSqr(this.te.getBlockPos().getX(), this.te.getBlockPos().getY(), this.te.getBlockPos().getZ()) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < PLAYER_START)
            {
                // From the fuel/food slots into the player inventory
                if (!this.moveItemStackTo(stack, PLAYER_START, TOTAL_SLOTS, true))
                {   return ItemStack.EMPTY;
                }
            }
            else if (this.slots.get(FIRST_FOOD_SLOT).mayPlace(stack))
            {
                // Into the food slots
                if (!this.moveItemStackTo(stack, FIRST_FOOD_SLOT, FIRST_FOOD_SLOT + FOOD_SLOTS, false))
                {   return ItemStack.EMPTY;
                }
            }
            else if (this.slots.get(FUEL_SLOT).mayPlace(stack))
            {
                // Into the fuel slot
                if (!this.moveItemStackTo(stack, FUEL_SLOT, FIRST_FOOD_SLOT, false))
                {   return ItemStack.EMPTY;
                }
            }
            else if (index >= HOTBAR_START)
            {
                // From the hotbar into the main inventory
                if (!this.moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false))
                {   return ItemStack.EMPTY;
                }
            }
            else
            {
                // From the main inventory into the hotbar
                if (!this.moveItemStackTo(stack, HOTBAR_START, TOTAL_SLOTS, false))
                {   return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty())
            {   slot.set(ItemStack.EMPTY);
            }
            else
            {   slot.setChanged();
            }

            if (stack.getCount() == result.getCount())
            {   return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }

        return result;
    }
}

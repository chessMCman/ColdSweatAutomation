package com.coldsweatautomation.core;

import com.coldsweatautomation.ColdSweatAutomation;
import com.coldsweatautomation.container.BigIceboxContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, ColdSweatAutomation.MOD_ID);

    /**
     * The expanded 108-slot icebox menu. It is a separate menu type from Cold Sweat's icebox
     * because NeoForge's screen registration rejects duplicates, and Cold Sweat's screen is far
     * too small to show 108 slots.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<BigIceboxContainer>> BIG_ICEBOX =
            MENU_TYPES.register("big_icebox", () -> IMenuTypeExtension.create(BigIceboxContainer::new));
}

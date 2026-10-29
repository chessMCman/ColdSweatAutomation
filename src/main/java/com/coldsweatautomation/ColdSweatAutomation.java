package com.coldsweatautomation;

import com.coldsweatautomation.core.ModMenus;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ColdSweatAutomation.MOD_ID)
public class ColdSweatAutomation
{
    public static final String MOD_ID = "cold_sweat_automation";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Whether Create (机械动力) is installed. */
    public static boolean createLoaded;
    /** Whether Applied Energistics 2 (AE2) is installed. */
    public static boolean ae2Loaded;

    public ColdSweatAutomation(IEventBus modBus, ModContainer modContainer)
    {
        createLoaded = ModList.get().isLoaded("create");
        ae2Loaded = ModList.get().isLoaded("ae2");

        // Both integrations are driven by the same generic IItemHandler capability (see
        // AutomationEvents), so the only mod-specific behaviour is which ones we report as active.
        if (createLoaded && ae2Loaded)
        {   LOGGER.info("[Cold Sweat Automation] Create and AE2 detected — enabling automation for both.");
        }
        else if (createLoaded)
        {   LOGGER.info("[Cold Sweat Automation] Create detected — enabling Create funnel/chute automation.");
        }
        else if (ae2Loaded)
        {   LOGGER.info("[Cold Sweat Automation] AE2 detected — enabling AE2 bus/interface automation.");
        }
        else
        {   LOGGER.info("[Cold Sweat Automation] Neither Create nor AE2 installed — only the icebox expansion (108 slots) is active.");
        }

        // Registry
        ModMenus.MENU_TYPES.register(modBus);

        // Events
        modBus.addListener(AutomationEvents::registerCapabilities);
    }
}

package com.coldsweatautomation.core;

import com.coldsweatautomation.ColdSweatAutomation;
import com.coldsweatautomation.client.gui.BigIceboxScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ColdSweatAutomation.MOD_ID, value = Dist.CLIENT)
public class ModScreens
{
    @SubscribeEvent
    public static void clientSetup(RegisterMenuScreensEvent event)
    {
        event.register(ModMenus.BIG_ICEBOX.get(), BigIceboxScreen::new);
    }
}

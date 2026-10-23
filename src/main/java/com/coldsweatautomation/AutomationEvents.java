package com.coldsweatautomation;

import com.coldsweatautomation.compat.HearthItemHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers an {@code IItemHandler} capability on Cold Sweat's hearth-like block entities so that
 * AE2 (ME Interface / Export Bus / Import Bus) and Create (Funnel / Chute) can automate them.
 *
 * <p>The handler exposes the whole container (fuel slot + product slots) uniformly on every side,
 * which removes the vanilla {@code WorldlyContainer} asymmetry that only allowed fuel input on the
 * horizontal sides and product extraction on the top/bottom (where the smokestack sits).</p>
 */
public final class AutomationEvents
{
    private AutomationEvents() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        // The item handler is what AE2 and Create talk to. Without either of them there is nothing
        // to automate, so it is skipped entirely (the icebox expansion works without it).
        if (!ColdSweatAutomation.createLoaded && !ColdSweatAutomation.ae2Loaded) return;

        register(event, "icebox");
        register(event, "boiler");
        register(event, "hearth");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(RegisterCapabilitiesEvent event, String name)
    {
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE
                .getOptional(ResourceLocation.fromNamespaceAndPath("cold_sweat", name)).orElse(null);
        if (type == null) return;

        BlockEntityType<BlockEntity> beType = (BlockEntityType) type;
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, beType, (be, side) ->
                be instanceof WorldlyContainer container ? new HearthItemHandler(container) : null);
    }
}

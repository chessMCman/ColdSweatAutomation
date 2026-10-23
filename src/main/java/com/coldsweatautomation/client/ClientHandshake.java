package com.coldsweatautomation.client;

import com.coldsweatautomation.ColdSweatAutomation;
import com.coldsweatautomation.network.HandshakePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client side of the mod presence handshake.
 *
 * <p>After joining, the client says hello and waits for the server's answer. A server that does
 * not have this mod installed will never answer, so the client closes the connection itself.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ColdSweatAutomation.MOD_ID, value = Dist.CLIENT)
public final class ClientHandshake
{
    /** How long (in ticks) the client waits for the server's answer. */
    private static final int TIMEOUT = 100;

    private static int ticks = -1;

    private ClientHandshake() {}

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        ticks = 0;
        PacketDistributor.sendToServer(new HandshakePayload(false));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {   ticks = -1;
    }

    /**
     * Handles the server's answer.
     */
    public static void handleAck(HandshakePayload payload)
    {
        if (payload.isAck())
        {   ticks = -1;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        if (ticks < 0) return;

        ticks++;
        if (ticks > TIMEOUT)
        {
            ticks = -1;
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null)
            {
                mc.getConnection().getConnection().disconnect(
                        Component.translatable("cold_sweat_automation.message.server_required"));
            }
        }
    }
}

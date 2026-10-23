package com.coldsweatautomation.network;

import com.coldsweatautomation.client.ClientHandshake;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Registers the handshake payload.
 *
 * <p>The payload is registered once in both directions - NeoForge does not allow registering the
 * same payload type twice (not even once per direction).</p>
 *
 * <p>Dispatch is done by payload <em>content</em>, not by {@code FMLEnvironment.dist}: in
 * single-player the integrated server also runs inside the client process, so the dist check
 * would misroute the server's handling of the client's hello and kick the player.</p>
 */
public final class NetworkEvents
{
    private NetworkEvents() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event)
    {
        event.registrar("1").playBidirectional(HandshakePayload.TYPE, HandshakePayload.CODEC, (payload, context) ->
        {
            if (payload.isAck())
            {
                // The server answered our hello - acks are only ever sent to clients
                ClientHandshake.handleAck(payload);
            }
            else if (context.player() instanceof ServerPlayer player)
            {
                // A client said hello - hellos are only ever sent to servers
                ServerHandshake.handleHello(player, payload);
            }
        });
    }
}

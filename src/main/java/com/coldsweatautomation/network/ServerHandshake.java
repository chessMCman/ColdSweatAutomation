package com.coldsweatautomation.network;

import com.coldsweatautomation.ColdSweatAutomation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server side of the mod presence handshake.
 *
 * <p>Every player has to send a hello packet shortly after joining, otherwise they are kicked -
 * which means clients without this mod cannot stay connected.</p>
 */
@EventBusSubscriber(modid = ColdSweatAutomation.MOD_ID)
public final class ServerHandshake
{
    /** How long (in ticks) a player has to answer before being kicked. */
    private static final int TIMEOUT = 100;

    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    private static final Set<UUID> VERIFIED = new HashSet<>();

    private ServerHandshake() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // The hello can theoretically arrive before the login event (especially in single-player),
        // in which case the player is already verified
        if (!VERIFIED.contains(player.getUUID()))
        {   PENDING.put(player.getUUID(), 0);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        PENDING.remove(event.getEntity().getUUID());
        VERIFIED.remove(event.getEntity().getUUID());
    }

    /**
     * Handles the hello packet of a joining client.
     */
    public static void handleHello(ServerPlayer player, HandshakePayload payload)
    {
        if (payload.isAck()) return;
        VERIFIED.add(player.getUUID());
        PENDING.remove(player.getUUID());
        // Let the client know that this server has the mod installed
        PacketDistributor.sendToPlayer(player, new HandshakePayload(true));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (PENDING.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticks = entry.getValue() + 1;
            entry.setValue(ticks);

            if (ticks > TIMEOUT)
            {
                iterator.remove();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null)
                {   player.connection.disconnect(Component.translatable("cold_sweat_automation.message.client_required"));
                }
            }
        }
    }
}

package com.coldsweatautomation.network;

import com.coldsweatautomation.ColdSweatAutomation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Handshake payload used to make sure this mod is installed on <em>both</em> the client and the
 * server.
 *
 * <ul>
 *   <li>The client sends {@code ack = false} (a hello) right after joining.</li>
 *   <li>The server answers with {@code ack = true}.</li>
 * </ul>
 *
 * If either side never receives the expected answer the connection is closed, so a client can only
 * join a server that has this mod installed, and a server can only keep clients that have it.
 */
public class HandshakePayload implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<HandshakePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ColdSweatAutomation.MOD_ID, "handshake"));

    public static final StreamCodec<FriendlyByteBuf, HandshakePayload> CODEC =
            CustomPacketPayload.codec(HandshakePayload::encode, HandshakePayload::decode);

    private final boolean ack;

    public HandshakePayload(boolean ack)
    {   this.ack = ack;
    }

    public boolean isAck()
    {   return this.ack;
    }

    public void encode(FriendlyByteBuf buffer)
    {   buffer.writeBoolean(this.ack);
    }

    public static HandshakePayload decode(FriendlyByteBuf buffer)
    {   return new HandshakePayload(buffer.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<HandshakePayload> type()
    {   return TYPE;
    }
}

package pl.doublejump.client.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Serwer -> klient. Serwer deklaruje, że obsługuje podwójny skok i pod jakimi warunkami.
 * Kolejność pól na łączu: VarInt wersja, boolean włączony, float maksymalna siła, VarInt min. ticki w powietrzu.
 */
public record HelloPayload(int version, boolean enabled, float maxStrength, int minAirTicks)
        implements CustomPacketPayload {

    public static final int PROTOCOL_VERSION = 1;

    public static final CustomPacketPayload.Type<HelloPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("doublejump", "hello"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HelloPayload::version,
            ByteBufCodecs.BOOL, HelloPayload::enabled,
            ByteBufCodecs.FLOAT, HelloPayload::maxStrength,
            ByteBufCodecs.VAR_INT, HelloPayload::minAirTicks,
            HelloPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package pl.doublejump.client.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Klient -> serwer. Zgłoszenie wykonania drugiego skoku (serwer go waliduje).
 * Na łączu: jeden float (siła).
 */
public record JumpPayload(float strength) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JumpPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("doublejump", "jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JumpPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, JumpPayload::strength,
            JumpPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

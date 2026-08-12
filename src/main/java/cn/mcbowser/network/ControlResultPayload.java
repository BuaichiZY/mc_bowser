package cn.mcbowser.network;

import cn.mcbowser.McBowser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ControlResultPayload(BlockPos origin, boolean granted, String ownerName) implements CustomPacketPayload {
    public static final Type<ControlResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(McBowser.MOD_ID, "control_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ControlResultPayload> STREAM_CODEC = StreamCodec.ofMember(
            ControlResultPayload::encode, ControlResultPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        BlockPos.STREAM_CODEC.encode(buffer, origin);
        ByteBufCodecs.BOOL.encode(buffer, granted);
        ByteBufCodecs.STRING_UTF8.encode(buffer, ownerName);
    }

    private static ControlResultPayload decode(RegistryFriendlyByteBuf buffer) {
        return new ControlResultPayload(BlockPos.STREAM_CODEC.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

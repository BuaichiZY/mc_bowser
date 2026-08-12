package cn.mcbowser.network;

import cn.mcbowser.McBowser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DisplayActionPayload(BlockPos origin, int action, String value, long timestamp) implements CustomPacketPayload {
    public static final int TOGGLE_DISPLAY = 0;
    public static final int REQUEST_CONTROL = 1;
    public static final int RELEASE_CONTROL = 2;
    public static final int CONTROL_HEARTBEAT = 3;
    public static final int UPDATE_URL = 4;
    public static final int PLAY_COMPATIBLE_MEDIA = 5;

    public static final Type<DisplayActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(McBowser.MOD_ID, "display_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DisplayActionPayload> STREAM_CODEC = StreamCodec.ofMember(
            DisplayActionPayload::encode, DisplayActionPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        BlockPos.STREAM_CODEC.encode(buffer, origin);
        ByteBufCodecs.VAR_INT.encode(buffer, action);
        ByteBufCodecs.STRING_UTF8.encode(buffer, value);
        ByteBufCodecs.LONG.encode(buffer, timestamp);
    }

    private static DisplayActionPayload decode(RegistryFriendlyByteBuf buffer) {
        return new DisplayActionPayload(BlockPos.STREAM_CODEC.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.LONG.decode(buffer));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

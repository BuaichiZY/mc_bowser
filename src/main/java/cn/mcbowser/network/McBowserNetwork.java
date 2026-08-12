package cn.mcbowser.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class McBowserNetwork {
    private McBowserNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");
        registrar.playToServer(DisplayActionPayload.TYPE, DisplayActionPayload.STREAM_CODEC, DisplayControlLocks::handle);
        registrar.playToClient(ControlResultPayload.TYPE, ControlResultPayload.STREAM_CODEC);
    }
}

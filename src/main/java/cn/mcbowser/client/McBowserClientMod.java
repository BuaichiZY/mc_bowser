package cn.mcbowser.client;

import cn.mcbowser.McBowser;
import cn.mcbowser.client.render.DisplayPanelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import cn.mcbowser.network.ControlResultPayload;

@EventBusSubscriber(modid = McBowser.MOD_ID, value = Dist.CLIENT)
public final class McBowserClientMod {
    private McBowserClientMod() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(McBowser.DISPLAY_PANEL_BLOCK_ENTITY.get(), context -> new DisplayPanelRenderer());
    }

    @SubscribeEvent
    public static void registerClientPackets(RegisterClientPayloadHandlersEvent event) {
        event.register(ControlResultPayload.TYPE, ClientControlState::handle);
    }
}

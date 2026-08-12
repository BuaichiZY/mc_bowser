package cn.mcbowser.client;

import cn.mcbowser.McBowser;
import cn.mcbowser.client.gui.BrowserControlScreen;
import cn.mcbowser.client.browser.DisplayBrowserManager;
import cn.mcbowser.screen.DisplayStructure;
import de.keksuccino.rinku.Rinku;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import cn.mcbowser.network.DisplayActionPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = McBowser.MOD_ID, value = Dist.CLIENT)
public final class McBowserClient {
    private McBowserClient() {}

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.hitResult == null) return;
        if (!(minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)) return;
        if (!minecraft.level.getBlockState(hit.getBlockPos()).is(McBowser.DISPLAY_PANEL.get())) return;

        DisplayStructure structure = DisplayStructure.find(minecraft.level, hit.getBlockPos());
        if (structure == null) {
            if (minecraft.player != null) {
                minecraft.player.sendOverlayMessage(Component.translatable("message.mc_bowser.invalid_structure"));
            }
            event.setCanceled(true);
            return;
        }

        if (!Rinku.isInitialized()) {
            if (minecraft.player != null) {
                String messageKey = Rinku.isInitializationAllowed()
                        ? "message.mc_bowser.browser_initializing"
                        : "message.mc_bowser.browser_unavailable";
                minecraft.player.sendSystemMessage(Component.translatable(messageKey));
            }
            event.setCanceled(true);
            return;
        }

        try {
            DisplayBrowserManager.Session session = DisplayBrowserManager.getOrCreate(minecraft.level, structure);
            if (minecraft.player != null && minecraft.player.isShiftKeyDown()) {
                ClientPacketDistributor.sendToServer(new DisplayActionPayload(
                        structure.origin(), DisplayActionPayload.REQUEST_CONTROL, "", 0L));
            } else {
                ClientPacketDistributor.sendToServer(new DisplayActionPayload(
                        structure.origin(), DisplayActionPayload.TOGGLE_DISPLAY, "", 0L));
            }
        } catch (RuntimeException | LinkageError error) {
            McBowser.LOGGER.error("Failed to open the Rinku browser screen", error);
            minecraft.setScreen(null);
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable("message.mc_bowser.browser_open_failed"));
            }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        DisplayBrowserManager.tick();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        DisplayBrowserManager.closeAll();
    }
}

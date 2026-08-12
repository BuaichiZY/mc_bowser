package cn.mcbowser.client;

import cn.mcbowser.client.browser.DisplayBrowserManager;
import cn.mcbowser.client.gui.BrowserControlScreen;
import cn.mcbowser.network.ControlResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientControlState {
    private ClientControlState() {}

    public static void handle(ControlResultPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!payload.granted()) {
            if (minecraft.player != null) minecraft.player.sendSystemMessage(
                    Component.translatable("message.mc_bowser.control_busy", payload.ownerName()));
            return;
        }
        DisplayBrowserManager.Session session = DisplayBrowserManager.findByOrigin(payload.origin());
        if (session != null) minecraft.setScreen(new BrowserControlScreen(session));
    }
}

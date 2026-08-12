package cn.mcbowser.client.gui;

import cn.mcbowser.client.browser.DisplayBrowserManager;
import cn.mcbowser.client.browser.BilibiliMediaBridge;
import cn.mcbowser.screen.DisplayStructure;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import cn.mcbowser.network.DisplayActionPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Controller for the same browser texture that remains visible on the blocks. */
public final class BrowserControlScreen extends Screen {
    private static final int MARGIN = 16;
    private static final int BAR_Y = 16;
    private static final int BAR_HEIGHT = 20;
    private static final int BROWSER_Y = 44;

    private final DisplayBrowserManager.Session session;
    private EditBox urlBox;
    private org.cef.handler.CefDisplayHandler addressHandler;
    private int heartbeatTicks;

    public BrowserControlScreen(DisplayBrowserManager.Session session) {
        super(Component.translatable("screen.mc_bowser.browser"));
        this.session = session;
    }

    public DisplayStructure structure() {
        return session.structure();
    }

    @Override
    protected void init() {
        super.init();
        int x = MARGIN;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> session.browser().goBack())
                .bounds(x, BAR_Y, 24, BAR_HEIGHT).build());
        x += 28;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> session.browser().goForward())
                .bounds(x, BAR_Y, 24, BAR_HEIGHT).build());
        x += 28;
        addRenderableWidget(Button.builder(Component.literal("R"), button -> session.browser().reload())
                .bounds(x, BAR_Y, 24, BAR_HEIGHT).build());
        x += 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.mc_bowser.compat_play"),
                        button -> BilibiliMediaBridge.play(session))
                .bounds(x, BAR_Y, 72, BAR_HEIGHT).build());
        x += 76;
        urlBox = addRenderableWidget(new EditBox(font, x, BAR_Y, Math.max(80, width - x - MARGIN), BAR_HEIGHT, Component.literal("URL")));
        urlBox.setMaxLength(2048);
        urlBox.setValue(currentUrl());
        registerAddressHandler();
    }

    private void registerAddressHandler() {
        if (addressHandler != null) return;
        addressHandler = new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                if (frame != null && frame.isMain() && minecraft != null && session.browser().isSame(browser)) {
                    minecraft.execute(() -> {
                        if (urlBox != null && !urlBox.isFocused()) urlBox.setValue(url);
                        ClientPacketDistributor.sendToServer(new DisplayActionPayload(session.structure().origin(),
                                DisplayActionPayload.UPDATE_URL, url, 0L));
                    });
                }
            }
        };
        de.keksuccino.rinku.Rinku.getClient().addDisplayHandler(addressHandler);
    }

    private String currentUrl() {
        String url = session.browser().getURL();
        return url == null || url.isBlank() ? DisplayBrowserManager.HOME_URL : url;
    }

    private int browserX() { return MARGIN; }
    private int browserWidth() { return Math.max(1, width - MARGIN * 2); }
    private int browserHeight() { return Math.max(1, height - BROWSER_Y - MARGIN); }
    private boolean overBrowser(double x, double y) {
        return x >= browserX() && x < browserX() + browserWidth() && y >= BROWSER_Y && y < BROWSER_Y + browserHeight();
    }
    private int browserMouseX(double x) {
        return (int) ((x - browserX()) / browserWidth() * session.browser().getRenderer().getTextureWidth());
    }
    private int browserMouseY(double y) {
        return (int) ((y - BROWSER_Y) / browserHeight() * session.browser().getRenderer().getTextureHeight());
    }

    private void navigate() {
        String url = urlBox.getValue().trim();
        if (url.isEmpty()) return;
        if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) url = "https://" + url;
        session.browser().loadURL(url);
        ClientPacketDistributor.sendToServer(new DisplayActionPayload(session.structure().origin(),
                DisplayActionPayload.UPDATE_URL, url, 0L));
        setFocused(null);
        session.browser().setFocus(true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!session.browser().isTextureReady()) return;
        RenderPipeline pipeline = session.browser().getRenderer().isTransparent()
                ? RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA : RenderPipelines.GUI_TEXTURED;
        graphics.blit(pipeline, session.browser().getTextureIdentifier(), browserX(), BROWSER_Y, 0, 0,
                browserWidth(), browserHeight(), browserWidth(), browserHeight());
    }

    @Override
    public void onClose() {
        if (addressHandler != null && de.keksuccino.rinku.Rinku.isInitialized()) {
            de.keksuccino.rinku.Rinku.getClient().removeDisplayHandler(addressHandler);
        }
        addressHandler = null;
        session.browser().setFocus(true);
        if (minecraft != null && minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new DisplayActionPayload(session.structure().origin(),
                    DisplayActionPayload.RELEASE_CONTROL, "", 0L));
        }
        // The session deliberately stays alive so its animated texture keeps playing in-world.
        super.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        if (++heartbeatTicks >= 40) {
            heartbeatTicks = 0;
            ClientPacketDistributor.sendToServer(new DisplayActionPayload(session.structure().origin(),
                    DisplayActionPayload.CONTROL_HEARTBEAT, "", 0L));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (!overBrowser(event.x(), event.y())) return false;
        session.browser().sendMousePress(browserMouseX(event.x()), browserMouseY(event.y()), event.button());
        session.browser().setFocus(true);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (super.mouseReleased(event)) return true;
        session.browser().sendMouseRelease(browserMouseX(event.x()), browserMouseY(event.y()), event.button());
        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (overBrowser(x, y)) session.browser().sendMouseMove(browserMouseX(x), browserMouseY(y));
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (super.mouseScrolled(x, y, horizontal, vertical)) return true;
        if (!overBrowser(x, y)) return false;
        session.browser().sendMouseWheel(browserMouseX(x), browserMouseY(y), vertical, 0);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (urlBox != null && urlBox.isFocused() && (event.key() == 257 || event.key() == 335)) {
            navigate();
            return true;
        }
        if (super.keyPressed(event)) return true;
        if (urlBox != null && urlBox.isFocused()) return true;
        session.browser().sendKeyPress(event.key(), event.scancode(), event.modifiers());
        session.browser().setFocus(true);
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (super.keyReleased(event)) return true;
        if (urlBox != null && urlBox.isFocused()) return true;
        session.browser().sendKeyRelease(event.key(), event.scancode(), event.modifiers());
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (super.charTyped(event)) return true;
        if (urlBox != null && urlBox.isFocused()) return true;
        if (event.codepoint() == 0) return false;
        session.browser().sendKeyTyped((char) event.codepoint(), 0);
        return true;
    }
}

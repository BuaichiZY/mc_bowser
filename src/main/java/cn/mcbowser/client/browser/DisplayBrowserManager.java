package cn.mcbowser.client.browser;

import cn.mcbowser.McBowser;
import cn.mcbowser.block.entity.DisplayPanelBlockEntity;
import cn.mcbowser.screen.DisplayStructure;
import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Path;

/** Owns the Chromium instances shown by in-world display structures. */
public final class DisplayBrowserManager {
    public static final String HOME_URL = "https://www.bilibili.com";
    private static final Map<Key, Session> SESSIONS = new ConcurrentHashMap<>();
    private static boolean popupHandlerInstalled;
    private static boolean loadHandlerInstalled;
    private static boolean audioHandlerInstalled;

    private DisplayBrowserManager() {}

    public static Session getOrCreate(Level level, DisplayStructure structure) {
        Key key = Key.of(level, structure);
        Session existing = SESSIONS.get(key);
        if (existing != null) return existing;

        installPopupHandler();
        installLoadHandler();
        installAudioHandler();
        int pixelsPerBlock = Math.max(96, Math.min(160,
                Math.min(1920 / structure.width(), 1080 / structure.height())));
        RinkuBrowser browser = Rinku.createBrowser(
                HOME_URL,
                false,
                structure.width() * pixelsPerBlock,
                structure.height() * pixelsPerBlock
        );
        browser.useBrowserControls(true);
        Session created = new Session(key, structure, browser);
        SESSIONS.put(key, created);
        return created;
    }

    public static Session find(Level level, DisplayStructure structure) {
        return SESSIONS.get(Key.of(level, structure));
    }

    public static Session findByOrigin(BlockPos origin) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        return SESSIONS.get(new Key(minecraft.level.dimension().identifier(), origin));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        for (Map.Entry<Key, Session> entry : SESSIONS.entrySet()) {
            Session session = entry.getValue();
            if (level == null || !session.key.dimension.equals(level.dimension().identifier())
                    || DisplayStructure.find(level, session.structure.origin()) == null) {
                session.close();
                SESSIONS.remove(entry.getKey(), session);
            } else if (level.getBlockEntity(session.structure.origin()) instanceof DisplayPanelBlockEntity panel) {
                session.sync(panel, level.getGameTime());
            }
        }
    }

    public static void closeAll() {
        SESSIONS.values().forEach(Session::close);
        SESSIONS.clear();
    }

    private static void installPopupHandler() {
        if (popupHandlerInstalled) return;
        Rinku.getClient().getHandle().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
                boolean managedByMcBowser = SESSIONS.values().stream()
                        .anyMatch(session -> session.browser().isSame(browser));
                if (!managedByMcBowser) return false;
                // Embedded OSR browsers have nowhere to display a native popup. Keep links
                // (including Bilibili video cards) in the same in-world browser instead.
                if (targetUrl != null && !targetUrl.isBlank()) browser.loadURL(targetUrl);
                return true;
            }
        });
        popupHandlerInstalled = true;
    }

    private static void installLoadHandler() {
        if (loadHandlerInstalled) return;
        Rinku.getClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (frame == null || !frame.isMain() || !isManagedBrowser(browser)) return;
                String url = browser.getURL();
                if (url == null || !url.toLowerCase(java.util.Locale.ROOT).contains("bilibili.com")) return;
                // Bilibili commonly opens cards in a new tab. OSR has no native tab/window,
                // so force those navigations into this browser while leaving SPA links alone.
                browser.executeJavaScript("""
                        (() => {
                          if (window.__mcBowserLinksPatched) return;
                          window.__mcBowserLinksPatched = true;
                          window.open = function(url) {
                            if (url) window.location.assign(String(url));
                            return window;
                          };
                          document.addEventListener('click', event => {
                            const node = event.target instanceof Element ? event.target.closest('a[href]') : null;
                            if (!node) return;
                            if (node.target === '_blank' || event.ctrlKey || event.metaKey || event.shiftKey) {
                              event.preventDefault();
                              event.stopImmediatePropagation();
                              window.location.assign(node.href);
                            }
                          }, true);
                        })();
                        """, url, 0);
            }
        });
        loadHandlerInstalled = true;
    }

    static boolean isManagedBrowser(CefBrowser browser) {
        return SESSIONS.values().stream().anyMatch(session -> session.browser().isSame(browser));
    }

    private static void installAudioHandler() {
        if (audioHandlerInstalled) return;
        Rinku.getClient().addAudioHandler(new BrowserAudioPlayer());
        audioHandlerInstalled = true;
    }

    public static final class Session {
        private final Key key;
        private final DisplayStructure structure;
        private final RinkuBrowser browser;
        private String synchronizedUrl = HOME_URL;
        private String synchronizedMediaUrl = "";
        private long synchronizedMediaStart;
        private boolean enabled;
        private double pendingMediaSeek = -1.0;

        private Session(Key key, DisplayStructure structure, RinkuBrowser browser) {
            this.key = key;
            this.structure = structure;
            this.browser = browser;
        }

        public DisplayStructure structure() { return structure; }
        public RinkuBrowser browser() { return browser; }
        public boolean isEnabled() { return enabled; }

        public void playMedia(Path media, double elapsedSeconds) {
            pendingMediaSeek = elapsedSeconds;
            browser.loadURL(media.toUri().toASCIIString());
        }

        public void sync(DisplayPanelBlockEntity panel, long clientGameTime) {
            enabled = panel.isDisplayEnabled();
            browser.setAudioMuted(!enabled);
            if (!enabled) return;
            browser.setFocus(true);
            String mediaUrl = panel.getCompatibleMediaUrl();
            if (!mediaUrl.isBlank()) {
                if (!mediaUrl.equals(synchronizedMediaUrl) || panel.getMediaStartedAt() != synchronizedMediaStart) {
                    synchronizedMediaUrl = mediaUrl;
                    synchronizedMediaStart = panel.getMediaStartedAt();
                    BilibiliMediaBridge.playSynchronized(this, mediaUrl, synchronizedMediaStart, clientGameTime);
                }
                applyPendingMediaSeek();
                return;
            }
            synchronizedMediaUrl = "";
            String url = panel.getBrowserUrl();
            if (url != null && !url.isBlank() && !url.equals(synchronizedUrl)) {
                synchronizedUrl = url;
                browser.loadURL(url);
            }
            applyPendingMediaSeek();
        }

        private void applyPendingMediaSeek() {
            if (pendingMediaSeek < 0.0 || browser.isLoading()) return;
            double seek = pendingMediaSeek;
            pendingMediaSeek = -1.0;
            String script = "const v=document.querySelector('video');if(v){v.currentTime=" + seek
                    + ";v.muted=false;v.play().catch(()=>{});}";
            browser.executeJavaScript(script, browser.getURL(), 0);
        }

        public void close() {
            try {
                browser.close();
            } catch (RuntimeException error) {
                McBowser.LOGGER.warn("Failed to close display browser at {}", structure.origin(), error);
            }
        }
    }

    private record Key(Identifier dimension, BlockPos origin) {
        static Key of(Level level, DisplayStructure structure) {
            return new Key(level.dimension().identifier(), structure.origin().immutable());
        }
    }
}

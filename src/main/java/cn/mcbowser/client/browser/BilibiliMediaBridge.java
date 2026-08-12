package cn.mcbowser.client.browser;

import cn.mcbowser.McBowser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import cn.mcbowser.network.DisplayActionPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Comparator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Transcodes public Bilibili media to the royalty-free WebM codecs available in stock CEF. */
public final class BilibiliMediaBridge {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "MC Bowser media bridge");
        thread.setDaemon(true);
        return thread;
    });
    private static final Set<DisplayBrowserManager.Session> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Map<DisplayBrowserManager.Session, Request> PENDING = new ConcurrentHashMap<>();

    private BilibiliMediaBridge() {}

    public static void play(DisplayBrowserManager.Session session) {
        String url = session.browser().getURL();
        if (!isPublicBilibiliUrl(url)) {
            message("message.mc_bowser.compat_bilibili_only");
            return;
        }
        ClientPacketDistributor.sendToServer(new DisplayActionPayload(session.structure().origin(),
                DisplayActionPayload.PLAY_COMPATIBLE_MEDIA, url, 0L));
    }

    public static void playSynchronized(DisplayBrowserManager.Session session, String url, long startedAt, long clientGameTime) {
        if (!ACTIVE.add(session)) {
            PENDING.put(session, new Request(url, startedAt, clientGameTime));
            return;
        }
        message("message.mc_bowser.compat_started");
        long prepareStartedNanos = System.nanoTime();
        CompletableFuture.supplyAsync(() -> {
            try {
                return prepare(url);
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }, WORKER).whenComplete((media, failure) -> {
            ACTIVE.remove(session);
            Request next = PENDING.remove(session);
            if (next != null) {
                playSynchronized(session, next.url(), next.startedAt(), next.clientGameTime());
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (failure != null) {
                    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                    McBowser.LOGGER.error("Bilibili compatibility playback failed for {}", url, cause);
                    message("message.mc_bowser.compat_failed", conciseMessage(cause));
                    return;
                }
                try {
                    double preparationSeconds = (System.nanoTime() - prepareStartedNanos) / 1_000_000_000.0;
                    double elapsedSeconds = Math.max(0.0, (clientGameTime - startedAt) / 20.0 + preparationSeconds);
                    session.playMedia(media, elapsedSeconds);
                    message("message.mc_bowser.compat_ready");
                } catch (RuntimeException error) {
                    McBowser.LOGGER.error("Failed to load transcoded media {}", media, error);
                    message("message.mc_bowser.compat_failed", conciseMessage(error));
                }
            });
        });
    }

    private static Path prepare(String url) throws IOException, InterruptedException {
        Settings settings = loadSettings();
        Files.createDirectories(settings.cacheDirectory());
        String cacheKey = sha256(url).substring(0, 24);
        Path webm = settings.cacheDirectory().resolve("bilibili-" + cacheKey + ".webm");
        if (Files.isRegularFile(webm) && Files.size(webm) > 0) {
            Files.setLastModifiedTime(webm, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
            pruneCache(settings.cacheDirectory(), webm);
            return webm;
        }
        Path sourceTemplate = settings.cacheDirectory().resolve("bilibili-" + cacheKey + "-source.%(ext)s");

        List<String> download = List.of(
                settings.ytDlp().toString(),
                "--no-playlist", "--no-part", "--force-overwrites",
                "--ffmpeg-location", settings.ffmpeg().getParent().toString(),
                "--format", "bv*[height<=480]+ba/b[height<=480]",
                "--merge-output-format", "mkv",
                "--output", sourceTemplate.toString(),
                "--quiet", "--no-warnings", "--print", "after_move:filepath",
                url
        );
        List<String> output = run(download, Duration.ofMinutes(30));
        Path source = output.stream().map(String::trim).filter(line -> !line.isBlank())
                .map(Path::of).filter(Files::isRegularFile).reduce((first, second) -> second)
                .orElseThrow(() -> new IOException("yt-dlp did not produce a media file"));

        List<String> transcode = List.of(
                settings.ffmpeg().toString(), "-hide_banner", "-loglevel", "error", "-y",
                "-i", source.toString(),
                "-c:v", "libvpx-vp9", "-deadline", "realtime", "-cpu-used", "8",
                "-row-mt", "1", "-tile-columns", "2", "-crf", "37", "-b:v", "0",
                "-c:a", "libopus", "-b:a", "128k",
                webm.toString()
        );
        try {
            run(transcode, Duration.ofMinutes(30));
        } finally {
            Files.deleteIfExists(source);
        }
        if (!Files.isRegularFile(webm) || Files.size(webm) == 0) {
            throw new IOException("FFmpeg did not produce a playable WebM file");
        }
        pruneCache(settings.cacheDirectory(), webm);
        return webm;
    }

    private static void pruneCache(Path cacheDirectory, Path current) throws IOException {
        try (var files = Files.list(cacheDirectory)) {
            List<Path> cached = files.filter(path -> path.getFileName().toString().startsWith("bilibili-")
                            && path.getFileName().toString().endsWith(".webm"))
                    .sorted(Comparator.comparingLong(BilibiliMediaBridge::lastModified).reversed()).toList();
            for (int index = 3; index < cached.size(); index++) {
                if (!cached.get(index).equals(current)) Files.deleteIfExists(cached.get(index));
            }
        }
    }

    private static long lastModified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException ignored) { return 0L; }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Settings loadSettings() throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        Path config = minecraft.gameDirectory.toPath().resolve("config/mc_bowser/media-bridge.properties");
        Properties properties = new Properties();
        if (!Files.isRegularFile(config)) {
            throw new IOException("missing " + config);
        }
        try (var reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        Path tools = Path.of(required(properties, "tools-directory")).toAbsolutePath().normalize();
        Path cache = Path.of(required(properties, "cache-directory")).toAbsolutePath().normalize();
        Path ytDlp = tools.resolve("yt-dlp.exe");
        Path ffmpeg;
        try (var files = Files.walk(tools)) {
            ffmpeg = files.filter(path -> path.getFileName().toString().equalsIgnoreCase("ffmpeg.exe"))
                    .findFirst().orElseThrow(() -> new IOException("ffmpeg.exe not found in " + tools));
        }
        if (!Files.isRegularFile(ytDlp)) throw new IOException("yt-dlp.exe not found in " + tools);
        return new Settings(ytDlp, ffmpeg, cache);
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IOException("missing setting: " + key);
        return value.trim();
    }

    private static List<String> run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        List<String> lines = new ArrayList<>();
        Thread reader = Thread.ofPlatform().daemon().start(() -> {
            try (BufferedReader stream = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                stream.lines().limit(200).forEach(lines::add);
            } catch (IOException ignored) {
            }
        });
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("media conversion timed out");
        }
        reader.join(5_000);
        if (process.exitValue() != 0) {
            String detail = lines.isEmpty() ? "exit code " + process.exitValue() : lines.get(lines.size() - 1);
            throw new IOException(detail);
        }
        return lines;
    }

    private static boolean isPublicBilibiliUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return (lower.startsWith("https://") || lower.startsWith("http://"))
                && (lower.contains("bilibili.com/video/") || lower.contains("b23.tv/"));
    }

    private static String conciseMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        if (message == null || message.isBlank()) message = current.getClass().getSimpleName();
        return message.length() > 160 ? message.substring(0, 160) : message;
    }

    private static void message(String key, Object... arguments) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.player.sendSystemMessage(Component.translatable(key, arguments));
    }

    private record Settings(Path ytDlp, Path ffmpeg, Path cacheDirectory) {}
    private record Request(String url, long startedAt, long clientGameTime) {}
}

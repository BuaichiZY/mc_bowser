package cn.mcbowser.client;

import cn.mcbowser.McBowser;
import de.keksuccino.rinku.OSPlatform;
import de.keksuccino.rinku.binarydownload.RinkuDownloader;
import de.keksuccino.rinku.util.GameDirectoryUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Installs the optional bundled Rinku JCEF runtime before Rinku attempts network access. */
public final class OfflineJcefBootstrap {
    private static final String COMMIT = "2eb4ca2648bda91d1dfed81e9a37ba92e757aff9";
    private static final String ARCHIVE_RESOURCE = "/offline-jcef/windows_amd64.tar.gz";
    private static final String CHECKSUM_RESOURCE = "/offline-jcef/windows_amd64.tar.gz.sha256";

    private OfflineJcefBootstrap() {}

    public static void installIfBundled() {
        if (OfflineJcefBootstrap.class.getResource(ARCHIVE_RESOURCE) == null) return;
        OSPlatform platform = OSPlatform.getPlatform();
        if (platform != OSPlatform.WINDOWS_AMD64) {
            McBowser.LOGGER.warn("This MC Bowser offline build bundles Windows x64 JCEF only; current platform is {}", platform);
            return;
        }

        Path libraries = GameDirectoryUtils.getGameDirectory().toPath().resolve("rinku-libraries");
        RinkuDownloader.ArtifactDownloader embedded = (ignoredUrl, target, maxBytes) ->
                copyResource(target.getName().endsWith(".sha256") ? CHECKSUM_RESOURCE : ARCHIVE_RESOURCE, target, maxBytes);
        RinkuDownloader downloader = new RinkuDownloader(
                "https://github.com/Keksuccino/jcef-rinku/releases/download",
                COMMIT,
                platform,
                RinkuDownloader.DownloadPolicy.defaults(),
                libraries,
                embedded,
                null
        );
        try {
            RinkuDownloader.InstallationResult result = downloader.installOrUpdate(false);
            McBowser.LOGGER.info("Offline Rinku JCEF is ready at {} (installed={})",
                    result.installationDirectory(), result.downloaded());
        } catch (IOException | RuntimeException error) {
            // Do not make Minecraft unstartable: Rinku may still recover through its normal downloader.
            McBowser.LOGGER.error("Could not install bundled offline Rinku JCEF; Rinku will use its normal recovery path", error);
        }
    }

    private static void copyResource(String resource, File target, long maxBytes) throws IOException {
        Path destination = target.toPath();
        try (InputStream input = OfflineJcefBootstrap.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing bundled resource " + resource);
            Files.createDirectories(destination.getParent());
            try (OutputStream output = Files.newOutputStream(destination)) {
                byte[] buffer = new byte[128 * 1024];
                long total = 0L;
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    total += read;
                    if (total > maxBytes) throw new IOException("Bundled JCEF resource exceeds Rinku's safety limit");
                    output.write(buffer, 0, read);
                }
            }
        }
    }
}

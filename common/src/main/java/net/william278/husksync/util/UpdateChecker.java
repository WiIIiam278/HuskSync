package net.william278.husksync.util;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {

    private final static int SPIGOT_PROJECT_ID = 97144;
    private final Logger logger;
    private final VersionUtils.Version currentVersion;

    public UpdateChecker(@NotNull String currentVersion, @NotNull Logger logger) {
        this.currentVersion = VersionUtils.Version.of(currentVersion);
        this.logger = logger;
    }

    public CompletableFuture<VersionUtils.Version> fetchLatestVersion() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + SPIGOT_PROJECT_ID);
                URLConnection urlConnection = url.openConnection();
                return VersionUtils.Version.of(new BufferedReader(new InputStreamReader(urlConnection.getInputStream())).readLine());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to fetch the latest plugin version", e);
            }
            return new VersionUtils.Version();
        });
    }

    public boolean isUpdateAvailable(@NotNull VersionUtils.Version latestVersion) {
        return latestVersion.compareTo(currentVersion) > 0;
    }

    public VersionUtils.Version getCurrentVersion() {
        return currentVersion;
    }

    public CompletableFuture<Boolean> isUpToDate() {
        return fetchLatestVersion().thenApply(this::isUpdateAvailable);
    }

    public void logToConsole() {
        fetchLatestVersion().thenAccept(latestVersion -> {
            if (isUpdateAvailable(latestVersion)) {
                logger.log(Level.WARNING, "A new version of HuskSync is available: v" + latestVersion);
            } else {
                logger.log(Level.INFO, "HuskSync is up-to-date! (Running: v" + getCurrentVersion().toString() + ")");
            }
        });
    }
}
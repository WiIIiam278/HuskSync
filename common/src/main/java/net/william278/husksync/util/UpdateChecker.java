package net.william278.husksync.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

public abstract class UpdateChecker {

    private final static int SPIGOT_PROJECT_ID = 97144;

    private final VersionUtils.Version currentVersion;
    private VersionUtils.Version latestVersion;

    public UpdateChecker(String currentVersion) {
        this.currentVersion = VersionUtils.Version.of(currentVersion);

        try {
            final URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + SPIGOT_PROJECT_ID);
            URLConnection urlConnection = url.openConnection();
            this.latestVersion = VersionUtils.Version.of(new BufferedReader(new InputStreamReader(urlConnection.getInputStream())).readLine());
        } catch (IOException e) {
            log(Level.WARNING, "Failed to check for updates: An IOException occurred.");
            this.latestVersion = new VersionUtils.Version();
        } catch (Exception e) {
            log(Level.WARNING, "Failed to check for updates: An exception occurred.");
            this.latestVersion = new VersionUtils.Version();
        }
    }

    public boolean isUpToDate() {
        return this.currentVersion.compareTo(latestVersion) >= 0;
    }

    public String getLatestVersion() {
        return latestVersion.toString();
    }

    public String getCurrentVersion() {
        return currentVersion.toString();
    }

    public abstract void log(Level level, String message);

    public void logToConsole() {
        if (!isUpToDate()) {
            log(Level.WARNING, "A new version of HuskSync is available: Version "
                    + latestVersion + " (Currently running: " + currentVersion + ")");
        }
    }
}

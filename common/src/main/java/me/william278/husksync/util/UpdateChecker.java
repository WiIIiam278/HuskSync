package me.william278.husksync.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

public abstract class UpdateChecker {

    private final static int SPIGOT_PROJECT_ID = 97144;

    private final String currentVersion;
    private String latestVersion;

    public UpdateChecker(String currentVersion) {
        this.currentVersion = currentVersion;

        try {
            final URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + SPIGOT_PROJECT_ID);
            URLConnection urlConnection = url.openConnection();
            this.latestVersion = new BufferedReader(new InputStreamReader(urlConnection.getInputStream())).readLine();
        } catch (IOException e) {
            log(Level.WARNING, "Failed to check for updates: An IOException occurred.");
            this.latestVersion = "Unknown";
        } catch (Exception e) {
            log(Level.WARNING, "Failed to check for updates: An exception occurred.");
            this.latestVersion = "Unknown";
        }
    }

    public boolean isUpToDate() {
        if (latestVersion.equalsIgnoreCase("Unknown")) {
            return true;
        }
        return latestVersion.equals(currentVersion);
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public abstract void log(Level level, String message);

    public void logToConsole() {
        if (!isUpToDate()) {
            log(Level.WARNING, "A new version of HuskSync is available: Version "
                    + latestVersion + " (Currently running: " + currentVersion + ")");
        }
    }
}

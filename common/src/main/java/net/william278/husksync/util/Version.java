package net.william278.husksync.util;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    private final static String VERSION_SEPARATOR = ".";
    private final static String MINECRAFT_META_SEPARATOR = "-";
    private final static String PLUGIN_META_SEPARATOR = "+";

    private int[] versions = new int[]{};
    @NotNull
    private String metadata = "";
    @NotNull
    private String metaSeparator = "";

    protected Version() {
    }

    private Version(@NotNull String version, @NotNull String metaSeparator) {
        this.parse(version, metaSeparator);
        this.metaSeparator = metaSeparator;
    }

    @NotNull
    public static Version pluginVersion(@NotNull String versionString) {
        return new Version(versionString, PLUGIN_META_SEPARATOR);
    }

    @NotNull
    public static Version minecraftVersion(@NotNull String versionString) {
        return new Version(versionString, MINECRAFT_META_SEPARATOR);
    }

    private void parse(@NotNull String version, @NotNull String metaSeparator) {
        int metaIndex = version.indexOf(metaSeparator);
        if (metaIndex > 0) {
            this.metadata = version.substring(metaIndex + 1);
            version = version.substring(0, metaIndex);
        }
        String[] versions = version.split(Pattern.quote(VERSION_SEPARATOR));
        this.versions = Arrays.stream(versions).mapToInt(Integer::parseInt).toArray();
    }

    @Override
    public int compareTo(@NotNull Version other) {
        int length = Math.max(this.versions.length, other.versions.length);
        for (int i = 0; i < length; i++) {
            int a = i < this.versions.length ? this.versions[i] : 0;
            int b = i < other.versions.length ? other.versions[i] : 0;

            if (a < b) return -1;
            if (a > b) return 1;
        }

        return 0;
    }

    @Override
    public String toString() {
        final StringJoiner joiner = new StringJoiner(VERSION_SEPARATOR);
        for (int version : this.versions) {
            joiner.add(String.valueOf(version));
        }
        return joiner + ((!this.metadata.isEmpty()) ? (this.metaSeparator + this.metadata) : "");
    }

}

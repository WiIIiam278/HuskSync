package me.william278.husksync.util;

import java.util.Arrays;

public class VersionUtils {

    private final static char META_SEPARATOR = '+';
    private final static String VERSION_SEPARATOR = "\\.";


    public static class Version implements Comparable<Version> {
        public int[] versions = new int[]{};
        public String metadata = "";

        public Version() {
        }

        public Version(String version) {
            this.parse(version);
        }

        public static Version of(String version) {
            return new Version(version);
        }

        private void parse(String version) {
            int metaIndex = version.indexOf(META_SEPARATOR);
            if (metaIndex > 0) {
                this.metadata = version.substring(metaIndex + 1);
                version = version.substring(0, metaIndex);
            }
            String[] versions = version.split(VERSION_SEPARATOR);
            this.versions = Arrays.stream(versions).mapToInt(Integer::parseInt).toArray();
        }

        @Override
        public int compareTo(Version version) {
            int length = Math.max(this.versions.length, version.versions.length);
            for (int i = 0; i < length; i++) {
                int a = i < this.versions.length ? this.versions[i] : 0;
                int b = i < version.versions.length ? version.versions[i] : 0;

                if (a < b) return -1;
                if (a > b) return 1;
            }

            return 0;
        }

        @Override
        public String toString() {
            StringBuilder stringBuffer = new StringBuilder();
            for (int version : this.versions) {
                stringBuffer.append(version).append('.');
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
            return stringBuffer.append('+').append(this.metadata).toString();
        }
    }

}

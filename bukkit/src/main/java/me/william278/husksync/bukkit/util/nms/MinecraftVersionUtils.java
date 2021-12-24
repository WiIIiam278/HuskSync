package me.william278.husksync.bukkit.util.nms;

import me.william278.husksync.util.ThrowSupplier;
import org.bukkit.Bukkit;

public class MinecraftVersionUtils {

    public final static String CRAFTBUKKIT_PACKAGE_PATH = Bukkit.getServer().getClass().getPackage().getName();

    public final static String PACKAGE_VERSION = CRAFTBUKKIT_PACKAGE_PATH.split("\\.")[3];
    public final static String MINECRAFT_PACKAGE = compare("1.17") < 0 ?
            "net.minecraft.server.".concat(PACKAGE_VERSION) : "net.minecraft.server";
    public final static String SERVER_VERSION = Bukkit.getBukkitVersion().split("-")[0];

    public static int compare(String version) {
        if (version == null) return 1;

        String[] as = SERVER_VERSION.split("\\.");
        String[] bs = version.split("\\.");

        int length = Math.max(as.length, bs.length);
        for (int i = 0; i < length; i++) {
            int a = i < as.length ? Integer.parseInt(as[i]) : 0;
            int b = i < bs.length ? Integer.parseInt(bs[i]) : 0;

            if (a < b) return -1;
            if (a > b) return 1;
        }

        return 0;
    }

    public static Class<?> getBukkitClass(String path) {
        return ThrowSupplier.get(() -> Class.forName(CRAFTBUKKIT_PACKAGE_PATH.concat(".").concat(path)));
    }

    public static Class<?> getMinecraftClass(String path) {
        return ThrowSupplier.get(() -> Class.forName(MINECRAFT_PACKAGE.concat(".").concat(path)));
    }

}

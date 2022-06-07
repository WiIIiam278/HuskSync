package net.william278.husksync.bukkit.util.nms;

import net.william278.husksync.util.ThrowSupplier;
import net.william278.husksync.util.VersionUtils;
import org.bukkit.Bukkit;

public class MinecraftVersionUtils {

    public final static String CRAFTBUKKIT_PACKAGE_PATH = Bukkit.getServer().getClass().getPackage().getName();

    public final static String PACKAGE_VERSION = CRAFTBUKKIT_PACKAGE_PATH.split("\\.")[3];
    public final static VersionUtils.Version SERVER_VERSION
            = VersionUtils.Version.of(Bukkit.getBukkitVersion().split("-")[0]);
    public final static String MINECRAFT_PACKAGE = SERVER_VERSION.compareTo(VersionUtils.Version.of("1.17")) < 0 ?
            "net.minecraft.server.".concat(PACKAGE_VERSION) : "net.minecraft.server";

    public static Class<?> getBukkitClass(String path) {
        return ThrowSupplier.get(() -> Class.forName(CRAFTBUKKIT_PACKAGE_PATH.concat(".").concat(path)));
    }

    public static Class<?> getMinecraftClass(String path) {
        return ThrowSupplier.get(() -> Class.forName(MINECRAFT_PACKAGE.concat(".").concat(path)));
    }

}

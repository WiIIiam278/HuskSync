package net.william278.husksync.hook;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.annotation.NumberProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.StringProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import net.william278.husksync.data.VersionedUserData;
import net.william278.husksync.database.Database;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@PluginInfo(
        name = "HuskSync",
        iconName = "arrow-right-arrow-left",
        iconFamily = Family.SOLID,
        color = Color.LIGHT_BLUE
)
public class PlanDataExtension implements DataExtension {

    private Database database;

    //todo add more providers
    protected PlanDataExtension(@NotNull Database database) {
        this.database = database;
    }

    protected PlanDataExtension() {
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_JOIN,
                CallEvents.PLAYER_LEAVE
        };
    }

    private CompletableFuture<Optional<VersionedUserData>> getCurrentUserData(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            final Optional<User> optionalUser = database.getUser(uuid).join();
            if (optionalUser.isPresent()) {
                return database.getCurrentUserData(optionalUser.get()).join();
            }
            return Optional.empty();
        });
    }

    @NumberProvider(
            text = "Data Sync Time",
            description = "The last time the user had their data synced with the server.",
            iconName = "clock",
            iconFamily = Family.SOLID,
            format = FormatType.TIME_MILLISECONDS,
            priority = 1
    )
    public long getCurrentDataTimestamp(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join().map(
                        versionedUserData -> versionedUserData.versionTimestamp().getTime())
                .orElse(new Date().getTime());
    }

    @StringProvider(
            text = "Data Version ID",
            description = "ID of the data version that the user is currently using.",
            iconName = "bolt",
            iconFamily = Family.SOLID,
            priority = 2
    )
    public String getCurrentDataId(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join().map(
                        versionedUserData -> versionedUserData.versionUUID().toString()
                                .split(Pattern.quote("-"))[0])
                .orElse("unknown");
    }

    @NumberProvider(
            text = "Advancements",
            description = "The number of advancements & recipes the player has progressed in",
            iconName = "award",
            iconFamily = Family.SOLID,
            priority = 3
    )
    public long getAdvancementsCompleted(@NotNull UUID playerUUID) {
        return getCurrentUserData(playerUUID).join().map(
                        versionedUserData -> (long) versionedUserData.userData().getAdvancementData().size())
                .orElse(0L);
    }
}

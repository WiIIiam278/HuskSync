package net.william278.husksync.hook;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ElementOrder;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import com.djrapitops.plan.extension.table.TableColumnFormat;
import net.william278.husksync.data.UserDataSnapshot;
import net.william278.husksync.database.Database;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@TabInfo(
        tab = "Current Status",
        iconName = "id-card",
        iconFamily = Family.SOLID,
        elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE, ElementOrder.GRAPH}
)
@TabInfo(
        tab = "Data Snapshots",
        iconName = "clipboard-list",
        iconFamily = Family.SOLID,
        elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE, ElementOrder.GRAPH}
)
@TabOrder({"Current Status", "Data Snapshots"})
@PluginInfo(
        name = "HuskSync",
        iconName = "exchange-alt",
        iconFamily = Family.SOLID,
        color = Color.LIGHT_BLUE
)
@SuppressWarnings("unused")
public class PlanDataExtension implements DataExtension {

    private Database database;

    private static final String UNKNOWN_STRING = "N/A";

    private static final String PINNED_HTML_STRING = "&#128205;&nbsp;";

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

    private CompletableFuture<Optional<UserDataSnapshot>> getCurrentUserData(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            final Optional<User> optionalUser = database.getUser(uuid).join();
            if (optionalUser.isPresent()) {
                return database.getCurrentUserData(optionalUser.get()).join();
            }
            return Optional.empty();
        });
    }

    @BooleanProvider(
            text = "Has Synced",
            description = "Whether this user has saved, synchronised data.",
            iconName = "exchange-alt",
            iconFamily = Family.SOLID,
            conditionName = "hasSynced",
            hidden = true
    )
    @Tab("Current Status")
    public boolean getUserHasSynced(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join().isPresent();
    }

    @Conditional("hasSynced")
    @NumberProvider(
            text = "Sync Time",
            description = "The last time the user had their data synced with the server.",
            iconName = "clock",
            iconFamily = Family.SOLID,
            format = FormatType.DATE_SECOND,
            priority = 6
    )
    @Tab("Current Status")
    public long getCurrentDataTimestamp(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join().map(
                        versionedUserData -> versionedUserData.versionTimestamp().getTime())
                .orElse(new Date().getTime());
    }

    @Conditional("hasSynced")
    @StringProvider(
            text = "Version ID",
            description = "ID of the data version that the user is currently using.",
            iconName = "bolt",
            iconFamily = Family.SOLID,
            priority = 5
    )
    @Tab("Current Status")
    public String getCurrentDataId(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join()
                .map(versionedUserData -> versionedUserData.versionUUID().toString()
                        .split(Pattern.quote("-"))[0])
                .orElse(UNKNOWN_STRING);
    }

    @Conditional("hasSynced")
    @StringProvider(
            text = "Health",
            description = "The number of health points out of the max health points this player currently has.",
            iconName = "heart",
            iconFamily = Family.SOLID,
            priority = 4
    )
    @Tab("Current Status")
    public String getHealth(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join()
                .flatMap(versionedUserData -> versionedUserData.userData().getStatus())
                .map(statusData -> (int) statusData.health + "/" + (int) statusData.maxHealth)
                .orElse(UNKNOWN_STRING);
    }

    @Conditional("hasSynced")
    @NumberProvider(
            text = "Hunger",
            description = "The number of hunger points this player currently has.",
            iconName = "drumstick-bite",
            iconFamily = Family.SOLID,
            priority = 3
    )
    @Tab("Current Status")
    public long getHunger(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join()
                .flatMap(versionedUserData -> versionedUserData.userData().getStatus())
                .map(statusData -> (long) statusData.hunger)
                .orElse(0L);
    }

    @Conditional("hasSynced")
    @NumberProvider(
            text = "Experience Level",
            description = "The number of experience levels this player currently has.",
            iconName = "hat-wizard",
            iconFamily = Family.SOLID,
            priority = 2
    )
    @Tab("Current Status")
    public long getExperienceLevel(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join()
                .flatMap(versionedUserData -> versionedUserData.userData().getStatus())
                .map(statusData -> (long) statusData.expLevel)
                .orElse(0L);
    }

    @Conditional("hasSynced")
    @StringProvider(
            text = "Game Mode",
            description = "The game mode this player is currently in.",
            iconName = "gamepad",
            iconFamily = Family.SOLID,
            priority = 1
    )
    @Tab("Current Status")
    public String getGameMode(@NotNull UUID uuid) {
        return getCurrentUserData(uuid).join()
                .flatMap(versionedUserData -> versionedUserData.userData().getStatus())
                .map(status -> status.gameMode)
                .orElse(UNKNOWN_STRING);
    }

    @Conditional("hasSynced")
    @NumberProvider(
            text = "Advancements",
            description = "The number of advancements & recipes the player has progressed in.",
            iconName = "award",
            iconFamily = Family.SOLID
    )
    @Tab("Current Status")
    public long getAdvancementsCompleted(@NotNull UUID playerUUID) {
        return getCurrentUserData(playerUUID).join()
                .flatMap(versionedUserData -> versionedUserData.userData().getAdvancements())
                .map(advancementsData -> (long) advancementsData.size())
                .orElse(0L);
    }

    @Conditional("hasSynced")
    @TableProvider(tableColor = Color.LIGHT_BLUE)
    @Tab("Data Snapshots")
    public Table getDataSnapshots(@NotNull UUID playerUUID) {
        final Table.Factory dataSnapshotsTable = Table.builder()
                .columnOne("Time", new Icon(Family.SOLID, "clock", Color.NONE))
                .columnOneFormat(TableColumnFormat.DATE_SECOND)
                .columnTwo("ID", new Icon(Family.SOLID, "bolt", Color.NONE))
                .columnThree("Cause", new Icon(Family.SOLID, "flag", Color.NONE))
                .columnFour("Pinned", new Icon(Family.SOLID, "thumbtack", Color.NONE));
        database.getUser(playerUUID).join().ifPresent(user ->
                database.getUserData(user).join().forEach(versionedUserData -> dataSnapshotsTable.addRow(
                        versionedUserData.versionTimestamp().getTime(),
                        versionedUserData.versionUUID().toString().split("-")[0],
                        versionedUserData.cause().name().toLowerCase().replaceAll("_", " "),
                        versionedUserData.pinned() ? PINNED_HTML_STRING + "Pinned" : "Unpinned"
                )));
        return dataSnapshotsTable.build();
    }
}

package net.william278.husksync.data;

import net.william278.husksync.command.Permission;
import net.william278.husksync.config.Locales;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Represents a uniquely versioned and timestamped snapshot of a user's data, including why it was saved.
 *
 * @param versionUUID      The unique identifier for this user data version
 * @param versionTimestamp An epoch milliseconds timestamp of when this data was created
 * @param userData         The {@link UserData} that has been versioned
 * @param cause            The {@link DataSaveCause} that caused this data to be saved
 */
public record UserDataSnapshot(@NotNull UUID versionUUID, @NotNull Date versionTimestamp,
                               @NotNull DataSaveCause cause, boolean pinned,
                               @NotNull UserData userData) implements Comparable<UserDataSnapshot> {

    /**
     * Version {@link UserData} into a {@link UserDataSnapshot}, assigning it a random {@link UUID} and the current timestamp {@link Date}
     * </p>
     * Note that this method will set {@code cause} to {@link DataSaveCause#API}
     *
     * @param userData The {@link UserData} to version
     * @return A new {@link UserDataSnapshot}
     * @implNote This isn't used to version data that is going to be set to a database to prevent UUID collisions.<p>
     * Database implementations should instead use their own UUID generation functions.
     */
    public static UserDataSnapshot create(@NotNull UserData userData) {
        return new UserDataSnapshot(UUID.randomUUID(), new Date(),
                DataSaveCause.API, false, userData);
    }

    /**
     * Display a menu in chat to an {@link OnlineUser} about this {@link UserDataSnapshot} for a {@link User dataOwner}
     *
     * @param user      The {@link OnlineUser} to display the menu to
     * @param dataOwner The {@link User} whose data this snapshot captures a state of
     * @param locales   The {@link Locales} to use for displaying the menu
     */
    public void displayDataOverview(@NotNull OnlineUser user, @NotNull User dataOwner, @NotNull Locales locales) {
        // Title message, timestamp, owner and cause.
        locales.getLocale("data_manager_title", versionUUID().toString().split("-")[0],
                        versionUUID().toString(), dataOwner.username, dataOwner.uuid.toString())
                .ifPresent(user::sendMessage);
        locales.getLocale("data_manager_timestamp",
                        new SimpleDateFormat("MMM dd yyyy, HH:mm:ss.sss").format(versionTimestamp()))
                .ifPresent(user::sendMessage);
        if (pinned()) {
            locales.getLocale("data_manager_pinned").ifPresent(user::sendMessage);
        }
        locales.getLocale("data_manager_cause", cause().name().toLowerCase().replaceAll("_", " "))
                .ifPresent(user::sendMessage);

        // User status data, if present in the snapshot
        userData().getStatus()
                .flatMap(statusData -> locales.getLocale("data_manager_status",
                        Integer.toString((int) statusData.health),
                        Integer.toString((int) statusData.maxHealth),
                        Integer.toString(statusData.hunger),
                        Integer.toString(statusData.expLevel),
                        statusData.gameMode.toLowerCase()))
                .ifPresent(user::sendMessage);

        // Advancement and statistic data, if both are present in the snapshot
        userData().getAdvancements()
                .flatMap(advancementData -> userData().getStatistics()
                        .flatMap(statisticsData -> locales.getLocale("data_manager_advancements_statistics",
                                Integer.toString(advancementData.size()),
                                generateAdvancementPreview(advancementData, locales),
                                String.format("%.2f", (((statisticsData.untypedStatistics.getOrDefault(
                                        "PLAY_ONE_MINUTE", 0)) / 20d) / 60d) / 60d))))
                .ifPresent(user::sendMessage);

        if (user.hasPermission(Permission.COMMAND_INVENTORY.node)
            && user.hasPermission(Permission.COMMAND_ENDER_CHEST.node)) {
            locales.getLocale("data_manager_item_buttons", dataOwner.username, versionUUID().toString())
                    .ifPresent(user::sendMessage);
        }
        if (user.hasPermission(Permission.COMMAND_USER_DATA_MANAGE.node)) {
            locales.getLocale("data_manager_management_buttons", dataOwner.username, versionUUID().toString())
                    .ifPresent(user::sendMessage);
        }
        if (user.hasPermission(Permission.COMMAND_USER_DATA_DUMP.node)) {
            locales.getLocale("data_manager_system_buttons", dataOwner.username, versionUUID().toString())
                    .ifPresent(user::sendMessage);
        }
    }

    @NotNull
    private String generateAdvancementPreview(@NotNull List<AdvancementData> advancementData, @NotNull Locales locales) {
        final StringJoiner joiner = new StringJoiner("\n");
        final List<AdvancementData> advancementsToPreview = advancementData.stream().filter(dataItem ->
                !dataItem.key.startsWith("minecraft:recipes/")).toList();
        final int PREVIEW_SIZE = 8;
        for (int i = 0; i < advancementsToPreview.size(); i++) {
            joiner.add(advancementsToPreview.get(i).key);
            if (i >= PREVIEW_SIZE) {
                break;
            }
        }
        final int remainingAdvancements = advancementsToPreview.size() - PREVIEW_SIZE;
        if (remainingAdvancements > 0) {
            joiner.add(locales.getRawLocale("data_manager_advancements_preview_remaining",
                    Integer.toString(remainingAdvancements)).orElse("+" + remainingAdvancements + "…"));
        }
        return joiner.toString();
    }

    /**
     * Compare UserData by creation timestamp
     *
     * @param other the other UserData to be compared
     * @return the comparison result; the more recent UserData is greater than the less recent UserData
     */
    @Override
    public int compareTo(@NotNull UserDataSnapshot other) {
        return Long.compare(this.versionTimestamp.getTime(), other.versionTimestamp.getTime());
    }

}

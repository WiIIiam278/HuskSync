package net.william278.husksync.util;

import net.william278.husksync.config.Locales;
import net.william278.husksync.data.UserDataSnapshot;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import net.william278.paginedown.PaginatedList;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a chat-viewable paginated list of {@link UserDataSnapshot}s
 */
public class DataSnapshotList {

    // Used for displaying number ordering next to snapshots in the list
    private static final String[] CIRCLED_NUMBER_ICONS = "①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳".split("");

    @NotNull
    private final PaginatedList paginatedList;

    private DataSnapshotList(@NotNull List<UserDataSnapshot> snapshots, @NotNull User dataOwner,
                             @NotNull Locales locales) {
        final AtomicInteger snapshotNumber = new AtomicInteger(1);
        this.paginatedList = PaginatedList.of(snapshots.stream()
                        .map(snapshot -> locales.getRawLocale("data_list_item",
                                        getNumberIcon(snapshotNumber.getAndIncrement()),
                                        new SimpleDateFormat("MMM dd yyyy, HH:mm:ss.sss")
                                                .format(snapshot.versionTimestamp()),
                                        snapshot.versionUUID().toString().split("-")[0],
                                        snapshot.versionUUID().toString(),
                                        snapshot.cause().getDisplayName(),
                                        dataOwner.username,
                                        snapshot.pinned() ? "※" : "  ")
                                .orElse("• " + snapshot.versionUUID())).toList(),
                locales.getBaseChatList(6)
                        .setHeaderFormat(locales.getRawLocale("data_list_title", dataOwner.username,
                                        "%first_item_on_page_index%", "%last_item_on_page_index%", "%total_items%")
                                .orElse(""))
                        .setCommand("/husksync:userdata list " + dataOwner.username)
                        .build());
    }

    /**
     * Create a new {@link DataSnapshotList} from a list of {@link UserDataSnapshot}s
     *
     * @param snapshots The list of {@link UserDataSnapshot}s to display
     * @param user      The {@link User} who owns the {@link UserDataSnapshot}s
     * @param locales   The {@link Locales} instance
     * @return A new {@link DataSnapshotList}, to be viewed with {@link #displayPage(OnlineUser, int)}
     */
    public static DataSnapshotList create(@NotNull List<UserDataSnapshot> snapshots, @NotNull User user,
                                          @NotNull Locales locales) {
        return new DataSnapshotList(snapshots, user, locales);
    }

    /**
     * Get an icon for the given snapshot number, via {@link #CIRCLED_NUMBER_ICONS}
     *
     * @param number the snapshot number
     * @return the icon for the given snapshot number
     */
    private static String getNumberIcon(int number) {
        if (number < 1 || number > 20) {
            return String.valueOf(number);
        }
        return CIRCLED_NUMBER_ICONS[number - 1];
    }

    /**
     * Display a page of the list of {@link UserDataSnapshot} to the user
     *
     * @param onlineUser The online user to display the message to
     * @param page       The page number to display
     */
    public void displayPage(@NotNull OnlineUser onlineUser, int page) {
        onlineUser.sendMessage(paginatedList.getNearestValidPage(page));
    }

}

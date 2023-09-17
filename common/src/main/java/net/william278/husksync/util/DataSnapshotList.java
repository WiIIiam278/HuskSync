/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.util;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.user.User;
import net.william278.paginedown.PaginatedList;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a chat-viewable paginated list of {@link net.william278.husksync.data.DataSnapshot}s
 */
public class DataSnapshotList {

    // Used for displaying number ordering next to snapshots in the list
    private static final String[] CIRCLED_NUMBER_ICONS = "①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳".split("");

    @NotNull
    private final PaginatedList paginatedList;

    private DataSnapshotList(@NotNull List<DataSnapshot.Packed> snapshots, @NotNull User dataOwner,
                             @NotNull HuskSync plugin) {
        final AtomicInteger snapshotNumber = new AtomicInteger(1);
        this.paginatedList = PaginatedList.of(snapshots.stream()
                        .map(snapshot -> plugin.getLocales()
                                .getRawLocale("data_list_item",
                                        getNumberIcon(snapshotNumber.getAndIncrement()),
                                        dataOwner.getUsername(),
                                        snapshot.getId().toString(),
                                        snapshot.getShortId(),
                                        snapshot.isPinned() ? "※" : "  ",
                                        snapshot.getTimestamp().format(DateTimeFormatter
                                                .ofPattern("dd/MM/yyyy, HH:mm")),
                                        snapshot.getTimestamp().format(DateTimeFormatter
                                                .ofPattern("MMM dd yyyy, HH:mm:ss.SSS")),
                                        snapshot.getSaveCause().getDisplayName(),
                                        String.format("%.2fKiB", snapshot.getFileSize(plugin) / 1024f))
                                .orElse("• " + snapshot.getId())).toList(),
                plugin.getLocales().getBaseChatList(6)
                        .setHeaderFormat(plugin.getLocales()
                                .getRawLocale("data_list_title", dataOwner.getUsername(),
                                        "%first_item_on_page_index%", "%last_item_on_page_index%", "%total_items%")
                                .orElse(""))
                        .setCommand("/husksync:userdata list " + dataOwner.getUsername())
                        .build());
    }

    /**
     * Create a new {@link DataSnapshotList} from a list of {@link DataSnapshot}s
     *
     * @param snapshots The list of {@link DataSnapshot}s to display
     * @param user      The {@link User} who owns the {@link DataSnapshot}s
     * @param plugin    The instance of the plugin
     * @return A new {@link DataSnapshotList}, to be viewed with {@link #displayPage(CommandUser, int)}
     */
    @NotNull
    public static DataSnapshotList create(@NotNull List<DataSnapshot.Packed> snapshots, @NotNull User user,
                                          @NotNull HuskSync plugin) {
        return new DataSnapshotList(snapshots, user, plugin);
    }

    /**
     * Get an hasIcon for the given snapshot number, via {@link #CIRCLED_NUMBER_ICONS}
     *
     * @param number the snapshot number
     * @return the hasIcon for the given snapshot number
     */
    private static String getNumberIcon(int number) {
        if (number < 1 || number > 20) {
            return String.valueOf(number);
        }
        return CIRCLED_NUMBER_ICONS[number - 1];
    }

    /**
     * Display a page of the list of {@link DataSnapshot} to the user
     *
     * @param onlineUser The online user to display the message to
     * @param page       The page number to display
     */
    public void displayPage(@NotNull CommandUser onlineUser, int page) {
        onlineUser.sendMessage(paginatedList.getNearestValidPage(page));
    }

}

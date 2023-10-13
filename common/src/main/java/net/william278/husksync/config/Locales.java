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

package net.william278.husksync.config;

import de.themoep.minedown.adventure.MineDown;
import net.william278.annotaml.YamlFile;
import net.william278.paginedown.ListOptions;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loaded locales used by the plugin to display styled messages
 */
@YamlFile(rootedMap = true, header = """
        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
        ┃       HuskSync Locales       ┃
        ┃    Developed by William278   ┃
        ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
        ┣╸ See plugin about menu for international locale credits
        ┣╸ Formatted in MineDown: https://github.com/Phoenix616/MineDown
        ┗╸ Translate HuskSync: https://william278.net/docs/husksync/translations""")
public class Locales {

    /**
     * The raw set of locales loaded from yaml
     */
    @NotNull
    public Map<String, String> rawLocales = new HashMap<>();

    /**
     * Returns a raw, unformatted locale loaded from the Locales file
     *
     * @param localeId String identifier of the locale, corresponding to a key in the file
     * @return An {@link Optional} containing the locale corresponding to the id, if it exists
     */
    public Optional<String> getRawLocale(@NotNull String localeId) {
        return Optional.ofNullable(rawLocales.get(localeId)).map(StringEscapeUtils::unescapeJava);
    }

    /**
     * Returns a raw, unformatted locale loaded from the Locales file, with replacements applied
     * <p>
     * Note that replacements will not be MineDown-escaped; use {@link #escapeMineDown(String)} to escape replacements
     *
     * @param localeId     String identifier of the locale, corresponding to a key in the file
     * @param replacements An ordered array of replacement strings to fill in placeholders with
     * @return An {@link Optional} containing the replacement-applied locale corresponding to the id, if it exists
     */
    public Optional<String> getRawLocale(@NotNull String localeId, @NotNull String... replacements) {
        return getRawLocale(localeId).map(locale -> applyReplacements(locale, replacements));
    }

    /**
     * Returns a MineDown-formatted locale from the Locales file
     *
     * @param localeId String identifier of the locale, corresponding to a key in the file
     * @return An {@link Optional} containing the formatted locale corresponding to the id, if it exists
     */
    public Optional<MineDown> getLocale(@NotNull String localeId) {
        return getRawLocale(localeId).map(MineDown::new);
    }

    /**
     * Returns a MineDown-formatted locale from the Locales file, with replacements applied
     * <p>
     * Note that replacements will be MineDown-escaped before application
     *
     * @param localeId     String identifier of the locale, corresponding to a key in the file
     * @param replacements An ordered array of replacement strings to fill in placeholders with
     * @return An {@link Optional} containing the replacement-applied, formatted locale corresponding to the id, if it exists
     */
    public Optional<MineDown> getLocale(@NotNull String localeId, @NotNull String... replacements) {
        return getRawLocale(localeId, Arrays.stream(replacements).map(Locales::escapeMineDown)
                .toArray(String[]::new)).map(MineDown::new);
    }

    /**
     * Apply placeholder replacements to a raw locale
     *
     * @param rawLocale    The raw, unparsed locale
     * @param replacements An ordered array of replacement strings to fill in placeholders with
     * @return the raw locale, with inserted placeholders
     */
    @NotNull
    private String applyReplacements(@NotNull String rawLocale, @NotNull String... replacements) {
        int replacementIndexer = 1;
        for (String replacement : replacements) {
            String replacementString = "%" + replacementIndexer + "%";
            rawLocale = rawLocale.replace(replacementString, replacement);
            replacementIndexer += 1;
        }
        return rawLocale;
    }

    /**
     * Escape a string from {@link MineDown} formatting for use in a MineDown-formatted locale
     * <p>
     * Although MineDown provides {@link MineDown#escape(String)}, that method fails to escape events
     * properly when using the escaped string in a replacement, so this is used instead
     *
     * @param string The string to escape
     * @return The escaped string
     */
    @NotNull
    public static String escapeMineDown(@NotNull String string) {
        final StringBuilder value = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            boolean isEscape = c == '\\';
            boolean isColorCode = i + 1 < string.length() && (c == 167 || c == '&');
            boolean isEvent = c == '[' || c == ']' || c == '(' || c == ')';
            if (isEscape || isColorCode || isEvent) {
                value.append('\\');
            }

            value.append(c);
        }
        return value.toString();
    }

    /**
     * Truncates a String to a specified length, and appends an ellipsis if it is longer than the specified length
     *
     * @param string The string to truncate
     * @param length The maximum length of the string
     * @return The truncated string
     */
    @NotNull
    public static String truncate(@NotNull String string, int length) {
        if (string.length() > length) {
            return string.substring(0, length) + "…";
        }
        return string;
    }

    /**
     * Returns the base list options to use for a paginated chat list
     *
     * @param itemsPerPage The number of items to display per page
     * @return The list options
     */
    @NotNull
    public ListOptions.Builder getBaseChatList(int itemsPerPage) {
        return new ListOptions.Builder()
                .setFooterFormat(getRawLocale("list_footer",
                        "%previous_page_button%", "%current_page%",
                        "%total_pages%", "%next_page_button%", "%page_jumpers%").orElse(""))
                .setNextButtonFormat(getRawLocale("list_next_page_button",
                        "%next_page_index%", "%command%").orElse(""))
                .setPreviousButtonFormat(getRawLocale("list_previous_page_button",
                        "%previous_page_index%", "%command%").orElse(""))
                .setPageJumpersFormat(getRawLocale("list_page_jumpers",
                        "%page_jump_buttons%").orElse(""))
                .setPageJumperPageFormat(getRawLocale("list_page_jumper_button",
                        "%target_page_index%", "%command%").orElse(""))
                .setPageJumperCurrentPageFormat(getRawLocale("list_page_jumper_current_page",
                        "%current_page%").orElse(""))
                .setPageJumperPageSeparator(getRawLocale("list_page_jumper_separator").orElse(""))
                .setPageJumperGroupSeparator(getRawLocale("list_page_jumper_group_separator").orElse(""))
                .setItemsPerPage(itemsPerPage)
                .setEscapeItemsMineDown(false)
                .setSpaceAfterHeader(false)
                .setSpaceBeforeFooter(false);
    }

    @SuppressWarnings("unused")
    public Locales() {
    }

    /**
     * Determines the slot a system notification should be displayed in
     */
    public enum NotificationSlot {
        /**
         * Displays the notification in the action bar
         */
        ACTION_BAR,
        /**
         * Displays the notification in the chat
         */
        CHAT,
        /**
         * Displays the notification in an Advancement Toast
         */
        TOAST,
        /**
         * Does not display the notification
         */
        NONE
    }
}

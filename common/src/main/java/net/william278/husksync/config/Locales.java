package net.william278.husksync.config;

import de.themoep.minedown.adventure.MineDown;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.william278.paginedown.ListOptions;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

/**
 * Loaded locales used by the plugin to display various locales
 */
public class Locales {

    @NotNull
    private final HashMap<String, String> rawLocales;

    private Locales(@NotNull YamlDocument localesConfig) {
        this.rawLocales = new HashMap<>();
        for (String localeId : localesConfig.getRoutesAsStrings(false)) {
            rawLocales.put(localeId, localesConfig.getString(localeId));
        }
    }

    /**
     * Returns an un-formatted locale loaded from the locales file
     *
     * @param localeId String identifier of the locale, corresponding to a key in the file
     * @return An {@link Optional} containing the locale corresponding to the id, if it exists
     */
    public Optional<String> getRawLocale(@NotNull String localeId) {
        return Optional.ofNullable(rawLocales.get(localeId)).map(StringEscapeUtils::unescapeJava);
    }

    /**
     * Returns an un-formatted locale loaded from the locales file, with replacements applied
     *
     * @param localeId     String identifier of the locale, corresponding to a key in the file
     * @param replacements Ordered array of replacement strings to fill in placeholders with
     * @return An {@link Optional} containing the replacement-applied locale corresponding to the id, if it exists
     */
    public Optional<String> getRawLocale(@NotNull String localeId, @NotNull String... replacements) {
        return getRawLocale(localeId).map(locale -> applyReplacements(locale, replacements));
    }

    /**
     * Returns a MineDown-formatted locale from the locales file
     *
     * @param localeId String identifier of the locale, corresponding to a key in the file
     * @return An {@link Optional} containing the formatted locale corresponding to the id, if it exists
     */
    public Optional<MineDown> getLocale(@NotNull String localeId) {
        return getRawLocale(localeId).map(MineDown::new);
    }

    /**
     * Returns a MineDown-formatted locale from the locales file, with replacements applied
     * <p>
     * Note that replacements will be MineDown-escaped before application
     *
     * @param localeId     String identifier of the locale, corresponding to a key in the file
     * @param replacements Ordered array of replacement strings to fill in placeholders with
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
     * @param replacements Ordered array of replacement strings to fill in placeholders with
     * @return the raw locale, with inserted placeholders
     */
    private String applyReplacements(@NotNull String rawLocale, @NotNull String... replacements) {
        int replacementIndexer = 1;
        for (String replacement : replacements) {
            String replacementString = "%" + replacementIndexer + "%";
            rawLocale = rawLocale.replace(replacementString, replacement);
            replacementIndexer = replacementIndexer + 1;
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

    /**
     * Load the locales from a BoostedYaml {@link YamlDocument} locales file
     *
     * @param localesConfig The loaded {@link YamlDocument} locales.yml file
     * @return the loaded {@link Locales}
     */
    public static Locales load(@NotNull YamlDocument localesConfig) {
        return new Locales(localesConfig);
    }

}

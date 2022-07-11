package net.william278.husksync.config;

import de.themoep.minedown.MineDown;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loaded locales used by the plugin to display various locales
 */
public class Locales {

    public static final String PLUGIN_INFORMATION = """
            [HuskSync](#00fb9a bold) [| Version %version%](#00fb9a)
            [A modern, cross-server player data synchronization system](gray)
            [• Author:](white) [William278](gray show_text=&7Click to visit website open_url=https://william278.net)
            [• Contributors:](white) [HarvelsX](gray show_text=&7Code)
            [• Translators:](white) [Namiu](gray show_text=&7\\(うにたろう\\) - Japanese, ja-jp), [anchelthe](gray show_text=&7Spanish, es-es), [Ceddix](gray show_text=&7German, de-de), [mateusneresrb](gray show_text=&7Brazilian Portuguese, pt-br], [小蔡](gray show_text=&7Traditional Chinese, zh-tw), [Ghost-chu](gray show_text=&7Simplified Chinese, zh-cn), [DJelly4K](gray show_text=&7Simplified Chinese, zh-cn), [Thourgard](gray show_text=&7Ukrainian, uk-ua)
            [• Documentation:](white) [[Link]](#00fb9a show_text=&7Click to open link open_url=https://william278.net/docs/husksync/Home/)
            [• Bug reporting:](white) [[Link]](#00fb9a show_text=&7Click to open link open_url=https://github.com/WiIIiam278/HuskSync/issues)
            [• Discord support:](white) [[Link]](#00fb9a show_text=&7Click to join open_url=https://discord.gg/tVYhJfyDWG)""";

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
        if (rawLocales.containsKey(localeId)) {
            return Optional.of(rawLocales.get(localeId).replaceAll(Pattern.quote("\\n"), "\n"));
        }
        return Optional.empty();
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
     *
     * @param localeId     String identifier of the locale, corresponding to a key in the file
     * @param replacements Ordered array of replacement strings to fill in placeholders with
     * @return An {@link Optional} containing the replacement-applied, formatted locale corresponding to the id, if it exists
     */
    public Optional<MineDown> getLocale(@NotNull String localeId, @NotNull String... replacements) {
        return getRawLocale(localeId, replacements).map(MineDown::new);
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
     * Load the locales from a BoostedYaml {@link YamlDocument} locales file
     *
     * @param localesConfig The loaded {@link YamlDocument} locales.yml file
     * @return the loaded {@link Locales}
     */
    public static Locales load(@NotNull YamlDocument localesConfig) {
        return new Locales(localesConfig);
    }

    /**
     * Strips a string of basic MineDown formatting, used for displaying plugin info to console
     *
     * @param string The string to strip
     * @return The MineDown-stripped string
     */
    public String stripMineDown(@NotNull String string) {
        final String[] in = string.split("\n");
        final StringBuilder out = new StringBuilder();
        String regex = "[^\\[\\]() ]*\\[([^()]+)]\\([^()]+open_url=(\\S+).*\\)";

        for (int i = 0; i < in.length; i++) {
            Pattern pattern = Pattern.compile(regex);
            Matcher m = pattern.matcher(in[i]);

            if (m.find()) {
                out.append(in[i].replace(m.group(0), ""));
                out.append(m.group(2));
            } else {
                out.append(in[i]);
            }

            if (i + 1 != in.length) {
                out.append("\n");
            }
        }

        return out.toString();
    }

}

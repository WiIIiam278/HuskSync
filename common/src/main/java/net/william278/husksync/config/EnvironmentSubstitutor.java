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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

// XMine start - подстановка переменных среды в конфиге
/**
 * Expands environment variable references such as {@code ${DB_HOST}} inside configuration values.
 * <p>
 * This is a port of {@code io.papermc.paper.configuration.EnvironmentSubstitutor} from the XMine
 * fork of Paper, character for character in behaviour. It has to be a port rather than a call:
 * the core hooks substitution into {@code org.bukkit.configuration.file.YamlConfiguration}
 * (nothing else), HuskSync reads its config through ConfigLib and SnakeYAML Engine, and the
 * {@code common} module compiles without any server API at all. Two implementations of one syntax
 * is a cost accepted deliberately - the alternative is an operator who has to remember which
 * plugin takes which spelling.
 *
 * <h2>Syntax</h2>
 * <ul>
 *     <li>{@code ${NAME}} - replaced by the value of the environment variable {@code NAME}.
 *     If the variable is <em>not set</em>, the reference is left in place verbatim. If it is
 *     set but empty, it expands to the empty string.</li>
 *     <li>{@code ${NAME:-fallback}} - replaced by the value of {@code NAME} when it is set and
 *     non-empty, and by {@code fallback} otherwise. This mirrors the POSIX shell operator of the
 *     same name, including its treatment of an empty value as "not set". The fallback text is
 *     itself expanded, so {@code ${A:-${B:-none}}} works.</li>
 *     <li>{@code $${NAME}} - an escape: expands to the literal text {@code ${NAME}} and the
 *     variable is never looked up.</li>
 * </ul>
 *
 * <h2>Why these rules</h2>
 * Configuration files that exist today were written without any knowledge of this feature, and
 * silently corrupting one is far worse than failing to expand a reference somebody wanted
 * expanded: a missed expansion is visible in the config and trivially diagnosed, a wrong one
 * shows up as a plugin misbehaving weeks later. Every ambiguous case is therefore resolved in
 * favour of leaving the text alone:
 * <ul>
 *     <li><b>Names are upper case only</b> ({@code [A-Z_][A-Z0-9_]*}). Environment variables are
 *     upper case by universal convention (POSIX, Docker, systemd, 12-factor); placeholders that
 *     plugins define for themselves are overwhelmingly lower case ({@code ${player}},
 *     {@code ${world}}), so restricting the charset costs nothing real and removes most of the
 *     collision surface at a stroke.</li>
 *     <li><b>An unset variable is left as written</b>, rather than expanding to the empty string
 *     or throwing. An empty string turns "the operator forgot to set DATABASE_PASSWORD" into a
 *     confusing authentication failure later on; throwing would stop the whole plugin over one
 *     unrelated placeholder. Leaving {@code ${DATABASE_PASSWORD}} in place makes the mistake
 *     self-describing. Use {@code ${NAME:-}} to ask for an empty string explicitly.</li>
 *     <li><b>{@code $$} is the escape</b>, matching Docker Compose, rather than a backslash.
 *     A backslash would have to survive YAML's own escaping inside double-quoted scalars, which
 *     is exactly the kind of rule nobody gets right the first time.</li>
 *     <li><b>Substituted values are never rescanned.</b> If {@code A=${B}} then {@code ${A}}
 *     expands to the four characters {@code ${B}}, not to the value of {@code B}. This rules out
 *     both expansion loops and an injection where the content of one variable reaches into
 *     another.</li>
 * </ul>
 *
 * <h2>Logging</h2>
 * This class never logs, and never places a variable's value into an exception message. Secrets
 * are the main reason to reach for this feature at all, so nothing here may leak one.
 *
 * @see EnvironmentYaml
 */
public final class EnvironmentSubstitutor {

    private static final Pattern VARIABLE_NAME = Pattern.compile("[A-Z_][A-Z0-9_]*");

    /**
     * Guards against a fallback chain such as {@code ${A:-${A:-${A:-...}}}} nested deeply enough
     * to matter. Real configurations never come close.
     */
    private static final int MAX_DEPTH = 16;

    private EnvironmentSubstitutor() {
    }

    /**
     * Expands every environment variable reference in the given text.
     *
     * @param text        the raw configuration text
     * @param environment lookup for variable values, normally {@code System::getenv}; it must
     *                    return {@code null} for a variable that is not set
     * @return the expanded text, or {@code null} if the text contains nothing to expand - callers
     * use this to keep the original scalar and skip any bookkeeping
     */
    @Nullable
    public static String substitute(@NotNull String text, @NotNull UnaryOperator<String> environment) {
        return substitute(text, environment, 0);
    }

    @Nullable
    private static String substitute(@NotNull String text, @NotNull UnaryOperator<String> environment, int depth) {
        if (depth > MAX_DEPTH || text.indexOf('$') < 0) {
            return null; // fast path: the overwhelming majority of configuration values
        }

        final int length = text.length();
        StringBuilder result = null;
        int index = 0;
        while (index < length) {
            final char current = text.charAt(index);
            if (current != '$') {
                if (result != null) {
                    result.append(current);
                }
                index++;
                continue;
            }

            // "$${NAME}" -> literal "${NAME}", with the body copied over untouched.
            if (index + 2 < length && text.charAt(index + 1) == '$' && text.charAt(index + 2) == '{') {
                final int close = matchingBrace(text, index + 2);
                if (close >= 0) {
                    if (result == null) {
                        result = new StringBuilder(length + 16).append(text, 0, index);
                    }
                    result.append(text, index + 1, close + 1);
                    index = close + 1;
                    continue;
                }
            }

            if (index + 1 < length && text.charAt(index + 1) == '{') {
                final int close = matchingBrace(text, index + 1);
                if (close >= 0) {
                    final String replacement = resolve(text.substring(index + 2, close), environment, depth);
                    if (replacement != null) {
                        if (result == null) {
                            result = new StringBuilder(length + 16).append(text, 0, index);
                        }
                        result.append(replacement);
                        index = close + 1;
                        continue;
                    }
                }
            }

            // A lone '$', an unterminated '${', a lower-case name, an unset variable: leave it be.
            if (result != null) {
                result.append(current);
            }
            index++;
        }

        return result == null ? null : result.toString();
    }

    /**
     * Resolves the body of a single {@code ${...}} reference.
     *
     * @return the replacement text, or {@code null} to leave the reference in place verbatim
     */
    @Nullable
    private static String resolve(@NotNull String body, @NotNull UnaryOperator<String> environment, int depth) {
        final int separator = body.indexOf(":-");
        final String name = separator < 0 ? body : body.substring(0, separator);
        if (!VARIABLE_NAME.matcher(name).matches()) {
            return null;
        }

        final String value = environment.apply(name);
        if (separator < 0) {
            return value; // null - not set - leaves "${NAME}" untouched
        }
        if (value != null && !value.isEmpty()) {
            return value;
        }

        final String fallback = body.substring(separator + 2);
        final String expanded = substitute(fallback, environment, depth + 1);
        return expanded != null ? expanded : fallback;
    }

    /**
     * Finds the {@code '}'} matching the {@code '{'} at {@code open}, counting nested braces so
     * that {@code ${A:-${B}}} and {@code ${A:-{literal}}} both parse.
     *
     * @return the index of the closing brace, or {@code -1} if the reference is unterminated
     */
    private static int matchingBrace(@NotNull String text, int open) {
        int depth = 0;
        for (int index = open; index < text.length(); index++) {
            final char current = text.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

}
// XMine end - подстановка переменных среды в конфиге

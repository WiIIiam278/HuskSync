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
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.api.lowlevel.Present;
import org.snakeyaml.engine.v2.api.lowlevel.Serialize;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

// XMine start - подстановка переменных среды в конфиге
/**
 * Rewrites a YAML document, expanding the environment variable references in its scalars.
 *
 * <h2>Why the substitution is not left to SnakeYAML</h2>
 * SnakeYAML Engine - the parser ConfigLib uses - ships variable substitution of its own:
 * {@link org.snakeyaml.engine.v2.env.EnvConfig} plus {@code StandardConstructor.ConstructEnv},
 * driven by an <em>implicit</em> resolver ({@code JsonScalarResolver} maps any plain scalar
 * matching {@code ENV_FORMAT} to {@code Tag.ENV_TAG}). Worth stating plainly, because the
 * opposite is true of the older {@code org.yaml.snakeyaml} library: <b>no {@code !ENV} tag is
 * needed here</b>. That objection does not apply to this codebase.
 * <p>
 * It still cannot be used, for two independent reasons.
 * <p>
 * <b>ConfigLib gives it nowhere to plug in.</b> {@code YamlConfigurationStore} builds its loader
 * once, in a {@code private static final} field, from {@code LoadSettings.builder().build()} - no
 * {@code EnvConfig} - and its constructor then <em>clears</em> {@code tagConstructors} and
 * re-registers only NULL/BOOL/STR/SEQ/MAP/INT/FLOAT. {@code YamlConfigurationProperties} exposes
 * no hook onto any of that. So a scalar carrying {@code Tag.ENV_TAG} does not get substituted by
 * ConfigLib; it fails to load at all, with "could not determine a constructor for the tag
 * !ENV_VARIABLE". (That is a pre-existing trap, not one this class introduces: an unquoted
 * {@code port: ${DATABASE_PORT}} breaks HuskSync's config loading today.)
 * <p>
 * <b>Its semantics are not ours.</b> The XMine fork of Paper expands {@code ${NAME}} inside every
 * Bukkit YAML configuration, and an operator writes configs for a dozen plugins; one syntax has
 * to mean one thing. {@code ENV_FORMAT} is anchored, so it only ever matches a scalar that is
 * <em>entirely</em> a placeholder - {@code jdbc://${HOST}/db} would be a literal; its name group
 * is {@code \w+}, so a lower-case {@code ${player}} counts as a variable; its default group is
 * {@code \w+} too, so {@code ${HOST:-db.internal}} silently is not a placeholder at all because
 * of the dot; an unset variable becomes the empty string; and {@code $${NAME}} is not an escape.
 * Every one of those differs from {@link EnvironmentSubstitutor}, and none of them can be fixed
 * through {@code EnvConfig}, which is only consulted <em>after</em> the resolver has already
 * decided what is a placeholder and what its parts are.
 * <p>
 * What is used from SnakeYAML Engine instead is its document model: {@link Compose} parses,
 * {@link Serialize} and {@link Present} write back. Substitution happens on the composed node
 * tree, exactly as it does in the core, and for the same reason: an expanded value can then
 * neither change the structure of the document nor turn a parse error into a place where a
 * secret could be printed. A password containing a quote, a colon or a newline is re-emitted
 * correctly quoted, because the emitter quotes it; a textual search-and-replace over the file
 * would produce a broken document, or worse, a valid one with a different shape.
 *
 * <h2>Types</h2>
 * An unquoted {@code port: ${DATABASE_PORT}} has to end up an integer, or ConfigLib rejects it
 * for an {@code int} field. The tag of a substituted scalar is therefore re-resolved from the
 * expanded text, but only over the narrow whitelist in {@link #implicitTag(String)} - the values
 * that survive a YAML round trip unchanged. A quoted reference stays a string, exactly as a
 * quoted literal would.
 */
public final class EnvironmentYaml {

    /**
     * A decimal integer whose {@code toString} is byte-for-byte what was read. Anything else -
     * {@code 0755}, {@code 1_000}, {@code 0x1F}, {@code -0} - is deliberately left as a string:
     * YAML 1.1 would turn {@code 0755} into 493, and a value the operator never wrote would then
     * be what the plugin runs on.
     */
    private static final Pattern CANONICAL_INT = Pattern.compile("0|-?[1-9][0-9]*");
    private static final Pattern CANONICAL_FLOAT = Pattern.compile("-?(?:0|[1-9][0-9]*)\\.[0-9]+");

    /**
     * Mirrors the settings ConfigLib itself loads and dumps with, so that a document which passes
     * through this class parses into exactly what it would have parsed into otherwise.
     */
    private static final LoadSettings LOAD_SETTINGS = LoadSettings.builder().build();
    private static final DumpSettings DUMP_SETTINGS = DumpSettings.builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setIndent(2)
            .build();

    private EnvironmentYaml() {
    }

    /**
     * Expands the environment variable references in a YAML document.
     * <p>
     * Comments are not preserved: the caller is expected to use the result only to read values
     * from, never to write it back over the operator's file. Writing an expanded document back
     * would put the database password on disk in plain text, which is the one thing this whole
     * mechanism exists to avoid.
     *
     * @param yaml        the document text
     * @param environment lookup for variable values, normally {@code System::getenv}
     * @return the rewritten document, or {@code null} when the document holds no references at
     * all and the caller should just read the original file
     */
    @Nullable
    public static String expand(@NotNull String yaml, @NotNull UnaryOperator<String> environment) {
        // No '$' anywhere means no placeholder and no ENV-tagged scalar, so there is nothing to
        // rewrite and nothing to normalise. Taking the original path in that case keeps the
        // behaviour of a config without references identical to upstream's, comments and all.
        if (yaml.indexOf('$') < 0) {
            return null;
        }

        final Optional<Node> root = new Compose(LOAD_SETTINGS).composeString(yaml);
        if (root.isEmpty()) {
            return null;
        }

        final Node original = root.get();
        final Node substituted = substitute(original, environment, new IdentityHashMap<>());
        final Node document = substituted != null ? substituted : original;
        return new Present(DUMP_SETTINGS).emitToString(
                new Serialize(DUMP_SETTINGS).serializeOne(document).iterator()
        );
    }

    /**
     * Visits a node, rewriting the scalars beneath it.
     *
     * @param node        the node to visit
     * @param environment lookup for variable values
     * @param visited     nodes already visited, so that a YAML alias follows its anchor and a
     *                    recursive document terminates. {@code Node} compares by identity, its
     *                    {@code equals} being final, so this really is an identity map
     * @return a node that must take {@code node}'s place in its parent, or {@code null} when the
     * parent should keep the node it has
     */
    @Nullable
    private static Node substitute(@NotNull Node node, @NotNull UnaryOperator<String> environment,
                                   @NotNull Map<Node, Node> visited) {
        final Node seen = visited.get(node);
        if (seen != null) {
            return seen == node ? null : seen;
        }
        visited.put(node, node);

        if (node instanceof MappingNode mapping) {
            final List<NodeTuple> tuples = mapping.getValue();
            List<NodeTuple> rebuilt = null;
            for (int i = 0; i < tuples.size(); i++) {
                final NodeTuple tuple = tuples.get(i);
                // A key is a path segment, never a value: substituting one would move a setting
                // somewhere else behind the plugin's back. It is still visited, because a key
                // that merely looks like a placeholder carries Tag.ENV_TAG and would otherwise
                // stop ConfigLib from constructing the document at all.
                final Node key = normaliseKey(tuple.getKeyNode());
                final Node value = substitute(tuple.getValueNode(), environment, visited);
                if (key != null || value != null) {
                    if (rebuilt == null) {
                        rebuilt = new ArrayList<>(tuples);
                    }
                    rebuilt.set(i, new NodeTuple(
                            key != null ? key : tuple.getKeyNode(),
                            value != null ? value : tuple.getValueNode()
                    ));
                }
            }
            if (rebuilt == null) {
                return null;
            }
            final Node replacement = new MappingNode(mapping.getTag(), rebuilt, mapping.getFlowStyle());
            visited.put(mapping, replacement);
            return replacement;
        }

        if (node instanceof SequenceNode sequence) {
            final List<Node> values = sequence.getValue();
            List<Node> rebuilt = null;
            for (int i = 0; i < values.size(); i++) {
                final Node replacement = substitute(values.get(i), environment, visited);
                if (replacement != null) {
                    if (rebuilt == null) {
                        rebuilt = new ArrayList<>(values);
                    }
                    rebuilt.set(i, replacement);
                }
            }
            if (rebuilt == null) {
                return null;
            }
            final Node replacement = new SequenceNode(sequence.getTag(), rebuilt, sequence.getFlowStyle());
            visited.put(sequence, replacement);
            return replacement;
        }

        if (!(node instanceof ScalarNode scalar)) {
            return null;
        }

        final String expanded = EnvironmentSubstitutor.substitute(scalar.getValue(), environment);
        final Node replacement;
        if (expanded == null) {
            // Nothing was expanded. A reference left verbatim - an unset variable, a lower-case
            // name - still has to lose Tag.ENV_TAG, or the document will not construct.
            replacement = detag(scalar);
        } else {
            // An unquoted "${DATABASE_PORT}" should behave like the number it expands to. A quoted
            // one stays a string, exactly as a quoted literal would have.
            final Tag tag = scalar.isPlain() && isPlainText(scalar.getTag())
                    ? implicitTag(expanded)
                    : Tag.STR;
            replacement = new ScalarNode(tag, expanded, styleFor(tag, scalar));
        }
        if (replacement != null) {
            visited.put(scalar, replacement);
        }
        return replacement;
    }

    /**
     * Strips {@code Tag.ENV_TAG} off a mapping key that happens to look like a placeholder. Its
     * text is untouched.
     *
     * @param key the key node
     * @return the replacement key, or {@code null} to keep the one there is
     */
    @Nullable
    private static Node normaliseKey(@NotNull Node key) {
        return key instanceof ScalarNode scalar ? detag(scalar) : null;
    }

    /**
     * Re-tags a scalar that the resolver read as a placeholder but that was left unexpanded, so
     * that ConfigLib - which knows no such tag - can construct it as the plain string it is.
     *
     * @param scalar the scalar
     * @return the replacement scalar, or {@code null} to keep the one there is
     */
    @Nullable
    private static Node detag(@NotNull ScalarNode scalar) {
        if (!Tag.ENV_TAG.equals(scalar.getTag())) {
            return null;
        }
        return new ScalarNode(Tag.STR, scalar.getValue(), ScalarStyle.DOUBLE_QUOTED);
    }

    /**
     * Whether a scalar's tag marks it as text the resolver had no better guess for - the two
     * tags a {@code ${...}} reference can end up with.
     *
     * @param tag the tag the scalar was composed with
     * @return whether an expansion of it may be re-typed
     */
    private static boolean isPlainText(@NotNull Tag tag) {
        return Tag.STR.equals(tag) || Tag.ENV_TAG.equals(tag);
    }

    /**
     * Resolves the implicit YAML tag of an expanded scalar, but only where the value survives a
     * round trip unchanged. Everything else stays a string rather than hand the plugin a value
     * the operator never wrote.
     *
     * @param value the expanded scalar text
     * @return the tag to give the scalar
     */
    @NotNull
    private static Tag implicitTag(@NotNull String value) {
        if (value.isEmpty()) {
            return Tag.STR; // an empty expansion is an empty string, not null
        }
        if (CANONICAL_INT.matcher(value).matches()) {
            return Tag.INT;
        }
        if (value.equals("true") || value.equals("false")) {
            return Tag.BOOL; // not "yes"/"on": those are not booleans to a YAML 1.2 parser anyway
        }
        if (CANONICAL_FLOAT.matcher(value).matches()) {
            try {
                if (Double.toString(Double.parseDouble(value)).equals(value)) {
                    return Tag.FLOAT;
                }
            } catch (NumberFormatException ignored) {
                // fall through to Tag.STR
            }
        }
        // Everything else - timestamps, sexagesimals, "~", "yes" - stays a string on purpose:
        // the whitelist above is exactly the set of values that survive a round trip unchanged.
        return Tag.STR;
    }

    /**
     * Picks the style the expanded scalar is written in. A number or a boolean has to go out
     * unquoted to be read back as one; text is always quoted, so that a password made entirely
     * of digits, or of a {@code #}, or of a leading {@code *}, cannot come back as something
     * else.
     *
     * @param tag      the tag the scalar will carry
     * @param original the scalar being replaced
     * @return the style to emit it in
     */
    @NotNull
    private static ScalarStyle styleFor(@NotNull Tag tag, @NotNull ScalarNode original) {
        if (!Tag.STR.equals(tag)) {
            return ScalarStyle.PLAIN;
        }
        // LITERAL and FOLDED carry newlines meaningfully; anything else is safe to quote.
        return original.getScalarStyle() == ScalarStyle.LITERAL
                || original.getScalarStyle() == ScalarStyle.FOLDED
                ? original.getScalarStyle()
                : ScalarStyle.DOUBLE_QUOTED;
    }

}
// XMine end - подстановка переменных среды в конфиге

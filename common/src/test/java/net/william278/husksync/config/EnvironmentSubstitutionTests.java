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

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// XMine start - подстановка переменных среды в конфиге
@DisplayName("Environment Substitution Tests")
public class EnvironmentSubstitutionTests {

    /**
     * A stand-in for the process environment. {@code DATABASE_EMPTY} is set to the empty string on
     * purpose - "set but empty" and "not set" are different cases and both are asserted below.
     */
    private static final Map<String, String> ENVIRONMENT = Map.of(
            "DATABASE_HOST", "mariadb.internal",
            "DATABASE_PORT", "3307",
            "DATABASE_PASSWORD", "s3cr3t: \"quoted\" #and hashed",
            "DATABASE_EMPTY", "",
            "DATABASE_INDIRECT", "${DATABASE_HOST}"
    );

    private static final UnaryOperator<String> ENV = ENVIRONMENT::get;

    // The scalar-level syntax: everything EnvironmentSubstitutor promises, and nothing else.

    @ParameterizedTest(name = "{2}")
    @DisplayName("Test Scalar Substitution")
    @MethodSource("provideScalars")
    public void testScalarSubstitution(@NotNull String template, @Nullable String expected,
                                       @SuppressWarnings("unused") @NotNull String description) {
        assertEquals(expected, EnvironmentSubstitutor.substitute(template, ENV));
    }

    @NotNull
    private static Stream<Arguments> provideScalars() {
        return Stream.of(
                Arguments.of("${DATABASE_HOST}", "mariadb.internal",
                        "A set variable is expanded"),
                Arguments.of("jdbc:mariadb://${DATABASE_HOST}:${DATABASE_PORT}/husksync",
                        "jdbc:mariadb://mariadb.internal:3307/husksync",
                        "References are expanded inside a larger string"),
                Arguments.of("${DATABASE_EMPTY}", "",
                        "A variable set to the empty string expands to the empty string"),

                Arguments.of("${DATABASE_SCHEMA:-husksync}", "husksync",
                        "An unset variable falls back to its default"),
                Arguments.of("${DATABASE_EMPTY:-husksync}", "husksync",
                        "An empty variable falls back to its default, as in the POSIX shell"),
                Arguments.of("${DATABASE_HOST:-localhost}", "mariadb.internal",
                        "A set variable wins over its default"),
                Arguments.of("${DATABASE_SCHEMA:-}", "",
                        "An empty default is how one asks for the empty string"),
                Arguments.of("${A_MISSING:-${DATABASE_HOST}}", "mariadb.internal",
                        "A default is itself expanded"),
                Arguments.of("${A_MISSING:-db.internal}", "db.internal",
                        "A default may hold characters outside [A-Za-z0-9_]"),

                Arguments.of("$${DATABASE_HOST}", "${DATABASE_HOST}",
                        "$$ escapes a reference, which is then never looked up"),
                Arguments.of("$${DATABASE_MISSING}", "${DATABASE_MISSING}",
                        "The escape does not care whether the variable exists"),

                Arguments.of("${DATABASE_MISSING}", null,
                        "An unset variable is left in the text verbatim"),
                Arguments.of("host is ${DATABASE_MISSING}", null,
                        "An unset variable leaves the whole scalar alone"),

                Arguments.of("${database_host}", null,
                        "A lower-case name is not a variable"),
                Arguments.of("${Database_Host}", null,
                        "Nor is a mixed-case one"),
                Arguments.of("${0DATABASE}", null,
                        "Nor is one starting with a digit"),
                Arguments.of("${player}", null,
                        "Which is what keeps a plugin's own placeholders intact"),

                Arguments.of("${DATABASE_INDIRECT}", "${DATABASE_HOST}",
                        "An expanded value is never rescanned"),
                Arguments.of("plain text", null,
                        "Text without a reference is left alone"),
                Arguments.of("$5.00 ${", null,
                        "A lone $ and an unterminated ${ are left alone")
        );
    }

    // The document level: what the plugin actually ends up loading.

    @Test
    @DisplayName("Test Document Substitution Keeps Types")
    public void testDocumentSubstitutionKeepsTypes() {
        final Map<String, Object> loaded = expandAndParse("""
                database:
                  host: "${DATABASE_HOST}"
                  port: ${DATABASE_PORT}
                  password: "${DATABASE_PASSWORD}"
                """);
        final Object database = loaded.get("database");
        assertInstanceOf(Map.class, database);

        final Map<?, ?> credentials = (Map<?, ?>) database;
        assertEquals("mariadb.internal", credentials.get("host"));
        assertEquals(3307, ((Number) credentials.get("port")).intValue(),
                "An unquoted reference has to come back as the number it expanded to");
        assertEquals(ENVIRONMENT.get("DATABASE_PASSWORD"), credentials.get("password"),
                "A password full of YAML metacharacters has to survive being re-emitted");
    }

    @Test
    @DisplayName("Test Unset Variable Still Parses")
    public void testUnsetVariableStillParses() {
        // The reference stays in the text, so the operator sees the mistake in the config rather
        // than as a refused connection later. It must not stop the document from loading, and it
        // must not carry SnakeYAML's !ENV_VARIABLE tag any further - ConfigLib cannot construct it.
        final Map<String, Object> loaded = expandAndParse("""
                database:
                  host: ${DATABASE_MISSING}
                  port: 3306
                """);
        final Map<?, ?> credentials = (Map<?, ?>) loaded.get("database");
        assertEquals("${DATABASE_MISSING}", credentials.get("host"));
        assertEquals(3306, ((Number) credentials.get("port")).intValue());
    }

    @Test
    @DisplayName("Test Keys And Lists Are Handled")
    public void testKeysAndListsAreHandled() {
        final Map<String, Object> loaded = expandAndParse("""
                hosts:
                  - "${DATABASE_HOST}"
                  - "${DATABASE_MISSING}"
                ${DATABASE_HOST}: key
                """);
        assertEquals(java.util.List.of("mariadb.internal", "${DATABASE_MISSING}"), loaded.get("hosts"));
        assertEquals("key", loaded.get("${DATABASE_HOST}"),
                "A key is a path segment, so it is never substituted");
    }

    @Test
    @DisplayName("Test Document Without References Is Left Alone")
    public void testDocumentWithoutReferencesIsLeftAlone() {
        assertNull(EnvironmentYaml.expand("database:\n  host: localhost\n", ENV),
                "A config with nothing to expand must take the untouched upstream path");
    }

    @Test
    @DisplayName("Test Substituted Config Loads Through ConfigLib")
    public void testSubstitutedConfigLoadsThroughConfigLib() {
        final String expanded = EnvironmentYaml.expand("""
                database:
                  type: MARIADB
                  credentials:
                    host: "${DATABASE_HOST}"
                    port: ${DATABASE_PORT}
                    database: husksync
                    username: "${DATABASE_USER:-plugins}"
                    password: "${DATABASE_PASSWORD}"
                """, ENV);
        assertNotNull(expanded);

        final YamlConfigurationProperties properties = YamlConfigurationProperties.newBuilder()
                .charset(StandardCharsets.UTF_8)
                .setNameFormatter(NameFormatters.LOWER_UNDERSCORE)
                .build();
        final Settings settings = new YamlConfigurationStore<>(Settings.class, properties)
                .read(new ByteArrayInputStream(expanded.getBytes(StandardCharsets.UTF_8)));

        final Settings.DatabaseSettings.DatabaseCredentials credentials = settings.getDatabase().getCredentials();
        assertEquals("mariadb.internal", credentials.getHost());
        assertEquals(3307, credentials.getPort(), "An int field is only satisfied by a real integer");
        assertEquals("plugins", credentials.getUsername());
        assertEquals(ENVIRONMENT.get("DATABASE_PASSWORD"), credentials.getPassword());
    }

    /**
     * Runs a document through the expansion and parses the result the way ConfigLib would.
     *
     * @param yaml the document
     * @return the parsed document
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private static Map<String, Object> expandAndParse(@NotNull String yaml) {
        final String expanded = EnvironmentYaml.expand(yaml, ENV);
        assertNotNull(expanded, "The document holds references, so it must have been rewritten");
        return (Map<String, Object>) new Load(LoadSettings.builder().build()).loadFromString(expanded);
    }

}
// XMine end - подстановка переменных среды в конфиге

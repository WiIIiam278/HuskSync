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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.User;
import net.william278.toilet.web.Flusher;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.logging.Level;

import static net.william278.husksync.util.DumpProvider.BYTEBIN_URL;

/**
 * Utility class for dumping {@link DataSnapshot}s to a file or as a paste on the web
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDataDumper implements Flusher {

    private final DataSnapshot.Packed snapshot;
    private final User user;
    private final HuskSync plugin;

    /**
     * Create a {@link UserDataDumper} of the given {@link DataSnapshot}
     *
     * @param snapshot The {@link DataSnapshot} to dump
     * @param user     The {@link User} whose data is being dumped
     * @param plugin   The implementing {@link HuskSync} plugin
     * @return A {@link UserDataDumper} for the given {@link DataSnapshot}
     */
    @NotNull
    public static UserDataDumper create(@NotNull DataSnapshot.Packed snapshot, @NotNull User user,
                                        @NotNull HuskSync plugin) {
        return new UserDataDumper(snapshot, user, plugin);
    }

    @NotNull
    public String toWeb() {
        try {
            return "%s/%s".formatted(BYTEBIN_URL, uploadDump(toString(), BYTEBIN_URL, "husksync"));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "Failed to upload data.", e);
        }
        return "(Failed to upload. Try dumping to a file instead.)";
    }

    /**
     * Dump the {@link DataSnapshot} to a file and return the file name
     *
     * @return the relative path of the file the data was dumped to
     */
    @NotNull
    public String toFile() throws IOException {
        final Path filePath = getFilePath();
        try (final FileWriter writer = new FileWriter(filePath.toFile(), StandardCharsets.UTF_8, false)) {
            writer.write(toString()); // Write the data from #getString to the file using a writer
            return filePath.toString();
        } catch (IOException e) {
            throw new IOException("Failed to write dump to file", e);
        }
    }

    /**
     * Get the file path to dump the data to
     *
     * @return the file path
     * @throws IOException if the prerequisite dumps parent folder could not be created
     */
    @NotNull
    private Path getFilePath() throws IOException {
        return getDumpsFolder().resolve(getFileName());
    }

    /**
     * Get the folder to dump the data to and create it if it does not exist
     *
     * @return the dumps folder
     * @throws IOException if the folder could not be created
     */
    @NotNull
    private Path getDumpsFolder() throws IOException {
        final Path dumps = plugin.getConfigDirectory().resolve("dumps");
        if (!Files.exists(dumps)) {
            Files.createDirectory(dumps);
        }
        return dumps;
    }

    /**
     * Get the name of the file to dump the data snapshot to
     *
     * @return the file name
     */
    @NotNull
    private String getFileName() {
        return new StringJoiner("_")
                .add(user.getName())
                .add(snapshot.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")))
                .add(snapshot.getSaveCause().name().toLowerCase(Locale.ENGLISH))
                .add(snapshot.getShortId())
                + ".json";
    }

    /**
     * Dumps the data snapshot to a string
     *
     * @return the data snapshot as a string
     */
    @Override
    @NotNull
    public String toString() {
        return snapshot.asJson(plugin);
    }

}

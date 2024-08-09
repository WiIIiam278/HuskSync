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

package net.william278.husksync.database;

import com.google.common.collect.Lists;
import com.mongodb.ConnectionString;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Updates;
import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.database.mongo.MongoCollectionHelper;
import net.william278.husksync.database.mongo.MongoConnectionHandler;
import net.william278.husksync.user.User;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;

public class MongoDbDatabase extends Database {
    private MongoConnectionHandler mongoConnectionHandler;
    private MongoCollectionHelper mongoCollectionHelper;

    private final String usersTable;
    private final String userDataTable;

    public MongoDbDatabase(@NotNull HuskSync plugin) {
        super(plugin);
        this.usersTable = plugin.getSettings().getDatabase().getTableName(TableName.USERS);
        this.userDataTable = plugin.getSettings().getDatabase().getTableName(TableName.USER_DATA);
    }

    /**
     * Initialize the database and ensure tables are present; create tables if they do not exist.
     *
     * @throws IllegalStateException if the database could not be initialized
     */
    @Override
    public void initialize() throws IllegalStateException {
        final Settings.DatabaseSettings.DatabaseCredentials credentials = plugin.getSettings().getDatabase().getCredentials();
        try {
            ConnectionString URI = createConnectionURI(credentials);
            mongoConnectionHandler = new MongoConnectionHandler(URI, credentials.getDatabase());
            mongoCollectionHelper = new MongoCollectionHelper(mongoConnectionHandler);
            if (mongoCollectionHelper.getCollection(usersTable) == null) {
                mongoCollectionHelper.createCollection(usersTable);
            }
            if (mongoCollectionHelper.getCollection(userDataTable) == null) {
                mongoCollectionHelper.createCollection(userDataTable);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to establish a connection to the MongoDB database. " +
                                            "Please check the supplied database credentials in the config file", e);
        }
    }

    @NotNull
    private ConnectionString createConnectionURI(Settings.DatabaseSettings.DatabaseCredentials credentials) {
        String baseURI = plugin.getSettings().getDatabase().getMongoSettings().isUsingAtlas() ?
                "mongodb+srv://{0}:{1}@{2}/{4}{5}" : "mongodb://{0}:{1}@{2}:{3}/{4}{5}";
        baseURI = baseURI.replace("{0}", credentials.getUsername());
        baseURI = baseURI.replace("{1}", credentials.getPassword());
        baseURI = baseURI.replace("{2}", credentials.getHost());
        baseURI = baseURI.replace("{3}", String.valueOf(credentials.getPort()));
        baseURI = baseURI.replace("{4}", credentials.getDatabase());
        baseURI = baseURI.replace("{5}", plugin.getSettings().getDatabase().getMongoSettings().getParameters());
        return new ConnectionString(baseURI);
    }

    /**
     * Ensure a {@link User} has an entry in the database and that their username is up-to-date
     *
     * @param user The {@link User} to ensure
     */
    @Blocking
    @Override
    public void ensureUser(@NotNull User user) {
        try {
            getUser(user.getUuid()).ifPresentOrElse(
                    existingUser -> {
                        if (!existingUser.getUsername().equals(user.getUsername())) {
                            // Update a user's name if it has changed in the database
                            try {
                                Document filter = new Document("uuid", existingUser.getUuid());
                                Document doc = mongoCollectionHelper.getCollection(usersTable).find(filter).first();
                                if (doc == null) {
                                    throw new MongoException("User document returned null!");
                                }

                                Bson updates = Updates.set("username", user.getUsername());
                                mongoCollectionHelper.updateDocument(usersTable, doc, updates);
                            } catch (MongoException e) {
                                plugin.log(Level.SEVERE, "Failed to insert a user into the database", e);
                            }
                        }
                    },
                    () -> {
                        // Insert new player data into the database
                        try {
                            Document doc = new Document("uuid", user.getUuid()).append("username", user.getUsername());
                            mongoCollectionHelper.insertDocument(usersTable, doc);
                        } catch (MongoException e) {
                            plugin.log(Level.SEVERE, "Failed to insert a user into the database", e);
                        }
                    }
            );
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to ensure user data is in the database", e);
        }
    }

    /**
     * Get a player by their Minecraft account {@link UUID}
     *
     * @param uuid Minecraft account {@link UUID} of the {@link User} to get
     * @return An optional with the {@link User} present if they exist
     */
    @Blocking
    @Override
    public Optional<User> getUser(@NotNull UUID uuid) {
        try {
            Document filter = new Document("uuid", uuid);
            Document doc = mongoCollectionHelper.getCollection(usersTable).find(filter).first();
            if (doc != null) {
                return Optional.of(new User(uuid, doc.getString("username")));
            }
            return Optional.empty();
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to get user data from the database", e);
            return Optional.empty();
        }
    }

    /**
     * Get a user by their username (<i>case-insensitive</i>)
     *
     * @param username Username of the {@link User} to get (<i>case-insensitive</i>)
     * @return An optional with the {@link User} present if they exist
     */
    @Blocking
    @Override
    public Optional<User> getUserByName(@NotNull String username) {
        try {
            Document filter = new Document("username", username);
            Document doc = mongoCollectionHelper.getCollection(usersTable).find(filter).first();
            if (doc != null) {
                return Optional.of(new User(doc.get("uuid", UUID.class),
                        doc.getString("username")));
            }
            return Optional.empty();
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to get user data from the database", e);
            return Optional.empty();
        }
    }

    /**
     * Get the latest data snapshot for a user.
     *
     * @param user The user to get data for
     * @return an optional containing the {@link DataSnapshot}, if it exists, or an empty optional if it does not
     */
    @Blocking
    @Override
    public Optional<DataSnapshot.Packed> getLatestSnapshot(@NotNull User user) {
        try {
            Document filter = new Document("player_uuid", user.getUuid());
            Document sort = new Document("timestamp", -1); // -1 = Descending
            FindIterable<Document> iterable = mongoCollectionHelper.getCollection(userDataTable).find(filter).sort(sort);
            Document doc = iterable.first();
            if (doc != null) {
                final UUID versionUuid = doc.get("version_uuid", UUID.class);
                final OffsetDateTime timestamp = OffsetDateTime.ofInstant(Instant.ofEpochMilli((long) doc.get("timestamp")), TimeZone.getDefault().toZoneId());
                final Binary bin = doc.get("data", Binary.class);
                final byte[] dataByteArray = bin.getData();
                return Optional.of(DataSnapshot.deserialize(plugin, dataByteArray, versionUuid, timestamp));
            }
            return Optional.empty();
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to get latest snapshot from the database", e);
            return Optional.empty();
        }
    }

    /**
     * Get all {@link DataSnapshot} entries for a user from the database.
     *
     * @param user The user to get data for
     * @return The list of a user's {@link DataSnapshot} entries
     */
    @Blocking
    @Override
    @NotNull
    public List<DataSnapshot.Packed> getAllSnapshots(@NotNull User user) {
        try {
            final List<DataSnapshot.Packed> retrievedData = Lists.newArrayList();
            Document filter = new Document("player_uuid", user.getUuid());
            Document sort = new Document("timestamp", -1); // -1 = Descending
            FindIterable<Document> iterable = mongoCollectionHelper.getCollection(userDataTable).find(filter).sort(sort);
            for (Document doc : iterable) {
                final UUID versionUuid = doc.get("version_uuid", UUID.class);
                final OffsetDateTime timestamp = OffsetDateTime.ofInstant(Instant.ofEpochMilli((long) doc.get("timestamp")), TimeZone.getDefault().toZoneId());
                final Binary bin = doc.get("data", Binary.class);
                final byte[] dataByteArray = bin.getData();
                retrievedData.add(DataSnapshot.deserialize(plugin, dataByteArray, versionUuid, timestamp));
            }
            return retrievedData;
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to get all snapshots from the database", e);
            return Lists.newArrayList();
        }
    }

    /**
     * Gets a specific {@link DataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link DataSnapshot} entry to get
     * @return An optional containing the {@link DataSnapshot}, if it exists
     */
    @Blocking
    @Override
    public Optional<DataSnapshot.Packed> getSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
        try {
            Document filter = new Document("player_uuid", user.getUuid()).append("version_uuid", versionUuid);
            Document sort = new Document("timestamp", -1); // -1 = Descending
            FindIterable<Document> iterable = mongoCollectionHelper.getCollection(userDataTable).find(filter).sort(sort);
            Document doc = iterable.first();
            if (doc != null) {
                final OffsetDateTime timestamp = OffsetDateTime.ofInstant(Instant.ofEpochMilli((long) doc.get("timestamp")), TimeZone.getDefault().toZoneId());
                final Binary bin = doc.get("data", Binary.class);
                final byte[] dataByteArray = bin.getData();
                return Optional.of(DataSnapshot.deserialize(plugin, dataByteArray, versionUuid, timestamp));
            }
            return Optional.empty();
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to get snapshot from the database", e);
            return Optional.empty();
        }
    }

    /**
     * <b>(Internal)</b> Prune user data for a given user to the maximum value as configured.
     *
     * @param user The user to prune data for
     * @implNote Data snapshots marked as {@code pinned} are exempt from rotation
     */
    @Blocking
    @Override
    protected void rotateSnapshots(@NotNull User user) {
        try {
            final List<DataSnapshot.Packed> unpinnedUserData = getAllSnapshots(user).stream()
                    .filter(dataSnapshot -> !dataSnapshot.isPinned()).toList();
            final int maxSnapshots = plugin.getSettings().getSynchronization().getMaxUserDataSnapshots();
            if (unpinnedUserData.size() > maxSnapshots) {

                Document filter = new Document("player_uuid", user.getUuid()).append("pinned", false);
                Document sort = new Document("timestamp", 1); // 1 = Ascending
                FindIterable<Document> iterable = mongoCollectionHelper.getCollection(userDataTable)
                        .find(filter)
                        .sort(sort)
                        .limit(unpinnedUserData.size() - maxSnapshots);

                for (Document doc : iterable) {
                    mongoCollectionHelper.deleteDocument(userDataTable, doc);
                }
            }
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to rotate snapshots", e);
        }
    }

    /**
     * Deletes a specific {@link DataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link DataSnapshot} entry to delete
     */
    @Blocking
    @Override
    public boolean deleteSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
        try {
            Document filter = new Document("player_uuid", user.getUuid()).append("version_uuid", versionUuid);
            Document doc = mongoCollectionHelper.getCollection(userDataTable).find(filter).first();
            if (doc == null) {
                return false;
            }
            mongoCollectionHelper.deleteDocument(userDataTable, doc);
            return true;
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to delete specific user data from the database", e);
        }
        return false;
    }

    /**
     * Deletes the most recent data snapshot by the given {@link User user}
     * The snapshot must have been created after {@link OffsetDateTime time} and NOT be pinned
     * Facilities the backup frequency feature, reducing redundant snapshots from being saved longer than needed
     *
     * @param user   The user to delete a snapshot for
     * @param within The time to delete a snapshot after
     */
    @Blocking
    @Override
    protected void rotateLatestSnapshot(@NotNull User user, @NotNull OffsetDateTime within) {
        try {
            Document filter = new Document("player_uuid", user.getUuid()).append("pinned", false);
            Document sort = new Document("timestamp", 1); // 1 = Ascending
            FindIterable<Document> iterable = mongoCollectionHelper.getCollection(userDataTable)
                    .find(filter)
                    .sort(sort);

            for (Document doc : iterable) {
                final OffsetDateTime timestamp = OffsetDateTime.ofInstant(
                        Instant.ofEpochMilli((long) doc.get("timestamp")), TimeZone.getDefault().toZoneId()
                );
                if (timestamp.isAfter(within)) {
                    mongoCollectionHelper.deleteDocument(userDataTable, doc);
                    return;
                }
            }
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to rotate latest snapshot from the database", e);
        }
    }

    /**
     * <b>Internal</b> - Create user data in the database
     *
     * @param user The user to add data for
     * @param data The {@link DataSnapshot} to set.
     */
    @Blocking
    @Override
    protected void createSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        try {
            Document doc = new Document("player_uuid", user.getUuid())
                    .append("version_uuid", data.getId())
                    .append("timestamp", data.getTimestamp().toInstant().toEpochMilli())
                    .append("save_cause", data.getSaveCause().name())
                    .append("pinned", data.isPinned())
                    .append("data", new Binary(data.asBytes(plugin)));
            mongoCollectionHelper.insertDocument(userDataTable, doc);
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to set user data in the database", e);
        }
    }

    /**
     * Update a saved {@link DataSnapshot} by given version UUID
     *
     * @param user The user whose data snapshot
     * @param data The {@link DataSnapshot} to update
     */
    @Blocking
    @Override
    public void updateSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        try {
            Document doc = new Document("player_uuid", user.getUuid()).append("version_uuid", data.getId());
            Bson updates = Updates.combine(
                    Updates.set("save_cause", data.getSaveCause().name()),
                    Updates.set("pinned", data.isPinned()),
                    Updates.set("data", new Binary(data.asBytes(plugin)))
            );
            mongoCollectionHelper.updateDocument(userDataTable, doc, updates);
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to update snapshot in the database", e);
        }
    }

    /**
     * Wipes <b>all</b> {@link User} entries from the database.
     * <b>This should only be used when preparing tables for a data migration.</b>
     */
    @Blocking
    @Override
    public void wipeDatabase() {
        try {
            mongoCollectionHelper.deleteCollection(usersTable);
        } catch (MongoException e) {
            plugin.log(Level.SEVERE, "Failed to wipe the database", e);
        }
    }

    /**
     * Close the database connection
     */
    @Override
    public void terminate() {
        if (mongoConnectionHandler != null) {
            mongoConnectionHandler.closeConnection();
        }
    }
}

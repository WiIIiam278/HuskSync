package net.william278.husksync.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.DeleteResult;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataAdapter;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.UserDataSnapshot;
import net.william278.husksync.event.DataSaveEvent;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.player.User;
import net.william278.husksync.util.Logger;
import net.william278.husksync.util.ResourceReader;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.in;

public class MongoDatabase extends Database {

    /**
     * MySQL server hostname
     */
    private final String mongoUri;
    private final String mongoUsername;
    private final String mongoPassword;
    private final String mongoDatabaseName;

    private MongoClient mongoClient;
    private com.mongodb.client.MongoDatabase database;
    private MongoCollection<Document> collectionUsers;
    private MongoCollection<Document> collectionUsersData;

    private final int maxPoolSize;
    private final int minPoolSize;
    private final int connectionTimeOut;
    private final int maximumLifetime;


    /**
     * The Hikari data source - a pool of database connections that can be fetched on-demand
     */

    public MongoDatabase(@NotNull Settings settings, @NotNull ResourceReader resourceReader, @NotNull Logger logger,
                         @NotNull DataAdapter dataAdapter, @NotNull EventCannon eventCannon) {
        super(settings.getStringValue(Settings.ConfigOption.DATABASE_USERS_TABLE_NAME),
                settings.getStringValue(Settings.ConfigOption.DATABASE_USER_DATA_TABLE_NAME),
                Math.max(1, Math.min(20, settings.getIntegerValue(Settings.ConfigOption.SYNCHRONIZATION_MAX_USER_DATA_SNAPSHOTS))),
                resourceReader, dataAdapter, eventCannon, logger);
        this.mongoUri = settings.getStringValue(Settings.ConfigOption.DATABASE_MONGO_URI);
        this.mongoUsername = settings.getStringValue(Settings.ConfigOption.DATABASE_MONGO_USERNAME);
        this.mongoPassword = settings.getStringValue(Settings.ConfigOption.DATABASE_MONGO_PASSWORD);
        this.mongoDatabaseName = settings.getStringValue(Settings.ConfigOption.DATABASE_MONGO_DATABASE);

        this.maxPoolSize = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_MAX_SIZE);
        this.minPoolSize = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_MIN_IDLE);
        this.connectionTimeOut = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_TIMEOUT);
        this.maximumLifetime = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_MAX_LIFETIME);
    }


    @Override
    public boolean initialize() {
        try {
            MongoClientSettings.Builder settings = MongoClientSettings.builder();
            settings.applyToConnectionPoolSettings(builder -> {
                builder.maxSize(maxPoolSize);
                builder.minSize(minPoolSize);
                builder.maxConnectionLifeTime(maximumLifetime, TimeUnit.MILLISECONDS);
            });

            settings.applyToSocketSettings(builder ->
                    builder.connectTimeout(connectionTimeOut, TimeUnit.MILLISECONDS)
            );


            if (mongoUri.isEmpty()) {
                settings.credential(
                        MongoCredential.createCredential(
                                mongoUsername,
                                mongoDatabaseName,
                                mongoPassword.toCharArray()
                        )
                );
            } else {
                settings.applyConnectionString(new ConnectionString(mongoUri));
            }


            this.mongoClient = MongoClients.create(settings.build());
            database = mongoClient.getDatabase(mongoDatabaseName);

            this.collectionUsers = database.getCollection(playerTableName);

            if (collectionNotExists(playerTableName)) {
                this.database.createCollection(playerTableName);
                collectionUsers.createIndex(new Document("uuid", 1),
                        new IndexOptions().unique(true));
            }

            this.collectionUsersData = database.getCollection(dataTableName);

            if (collectionNotExists(dataTableName)) {
                this.database.createCollection(dataTableName);
                // The number 1 mean tha the index will be sorted by ASCENDING order
                collectionUsersData.createIndex(new Document("version_uuid", 1),
                        new IndexOptions().unique(true));
                collectionUsersData.createIndex(new Document("player_uuid", 1),
                        new IndexOptions().unique(true));
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public CompletableFuture<Void> ensureUser(@NotNull User user) {
        return CompletableFuture.runAsync(() -> getUser(user.uuid).thenAccept(optionalUser ->
                optionalUser.ifPresentOrElse(existingUser -> {
                            if (!existingUser.username.equals(user.username)) {
                                collectionUsers.updateOne(
                                        eq("uuid", existingUser.uuid.toString()),
                                        new Document("$set", new Document("username", user.username))
                                );
                            }
                        },
                        () ->
                                collectionUsers.insertOne(
                                        new Document("uuid", user.uuid.toString())
                                                .append("username", user.username)
                                )
                )));
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (MongoCursor<Document> cursor = collectionUsers.find(eq("uuid", uuid.toString())).cursor()) {
                if (cursor.hasNext()) {
                    Document document = cursor.next();
                    final User user = mapUserFromDocument(document);
                    return Optional.of(user);
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user from uuid from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<User>> getUserByName(@NotNull String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (MongoCursor<Document> cursor = collectionUsers.find(eq("username", username)).iterator()) {
                if (cursor.hasNext()) {
                    Document document = cursor.next();
                    final User user = mapUserFromDocument(document);
                    return Optional.of(user);
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user by name from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<UserDataSnapshot>> getCurrentUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> {
            Document document = collectionUsersData.find()
                    .filter(eq("player_uuid", user.uuid.toString()))
                    .sort(new Document("timestamp", -1))
                    .limit(1).first();
            if (document != null) {
                final UserDataSnapshot userDataSnapshot = mapUserDataFromDocument(document);
                return Optional.of(userDataSnapshot);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<List<UserDataSnapshot>> getUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> {
            final List<UserDataSnapshot> retrievedData = new ArrayList<>();

            try (MongoCursor<Document> cursor = collectionUsersData.find(eq("player_uuid", user.uuid.toString()))
                    .sort(new Document("timestamp", -1)).cursor()) {
                while (cursor.hasNext()) {
                    final Document document = cursor.next();
                    final UserDataSnapshot data = mapUserDataFromDocument(document);
                    retrievedData.add(data);
                }

                return retrievedData;
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user's current user data from the database", e);
            }
            return retrievedData;
        });
    }

    @Override
    public CompletableFuture<Optional<UserDataSnapshot>> getUserData(@NotNull User user, @NotNull UUID versionUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document document = collectionUsersData
                        .find()
                        .filter(and(
                                eq("player_uuid", user.uuid.toString()),
                                eq("version_uuid", versionUuid.toString())
                        ))
                        .sort(new Document("timestamp", -1))
                        .limit(1).first();
                // We use -1 to sort in DESCENDING order

                if (document != null) {
                    final UserDataSnapshot userDataSnapshot = mapUserDataFromDocument(document);
                    return Optional.of(userDataSnapshot);
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to fetch specific user data by UUID from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    protected CompletableFuture<Void> rotateUserData(@NotNull User user) {
        return CompletableFuture.runAsync(() -> {
            try {
                final List<UserDataSnapshot> unpinnedUserData = getUserData(user).join().stream()
                        .filter(dataSnapshot -> !dataSnapshot.pinned()).toList();
                if (unpinnedUserData.size() > maxUserDataRecords) {
                    MongoIterable<ObjectId> mongoIterable = collectionUsersData.find(
                                    eq("player_uuid", user.uuid.toString())
                            ).sort(new Document("timestamp", 1))
                            .limit(unpinnedUserData.size() - maxUserDataRecords)
                            .map(document -> document.getObjectId("_id"));

                    collectionUsersData.deleteMany(in("_id", mongoIterable));
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to prune user data from the database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteUserData(@NotNull User user, @NotNull UUID versionUuid) {
        return CompletableFuture.supplyAsync(() -> {
            DeleteResult deleteResult = collectionUsersData.deleteOne(
                    and(
                            eq("player_uuid", user.uuid.toString()),
                            eq("version_uuid", versionUuid.toString())
                    )
            );
            return deleteResult.getDeletedCount() > 0;
        });
    }

    @Override
    public CompletableFuture<Void> setUserData(@NotNull User user, @NotNull UserData userData,
                                               @NotNull DataSaveCause saveCause) {
        return CompletableFuture.runAsync(() -> {
            final DataSaveEvent dataSaveEvent = (DataSaveEvent) getEventCannon().fireDataSaveEvent(user,
                    userData, saveCause).join();
            if (!dataSaveEvent.isCancelled()) {
                final UserData finalData = dataSaveEvent.getUserData();
                collectionUsersData.insertOne(
                        new Document("player_uuid", user.uuid.toString())
                                .append("version_uuid", UUID.randomUUID().toString())
                                .append("timestamp", Timestamp.from(Instant.now()).toString())
                                .append("save_cause", saveCause.name())
                                .append("data", new Binary(getDataAdapter().toBytes(finalData)))
                );
                // The Binary class here is the equivalent of blob in SQL.
            }
        }).thenRun(() -> rotateUserData(user).join());
    }

    @Override
    public CompletableFuture<Void> pinUserData(@NotNull User user, @NotNull UUID versionUuid) {
        return CompletableFuture.runAsync(() ->
                        collectionUsersData.updateOne(
                                eq("player_uuid", user.uuid.toString()),
                                new Document("$set", new Document("pinned", true))
                        )
                // The $set here mean it will update the document if it exists with the new value
        );
    }

    @Override
    public CompletableFuture<Void> unpinUserData(@NotNull User user, @NotNull UUID versionUuid) {
        return CompletableFuture.runAsync(() ->
                        collectionUsersData.updateOne(
                                eq("player_uuid", user.uuid.toString()),
                                new Document("$set", new Document("pinned", false))
                        )
                // The $set here mean it will update the document if it exists with the new value
        );
    }

    @Override
    public CompletableFuture<Void> wipeDatabase() {
        return CompletableFuture.runAsync(() -> collectionUsersData.drop());
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }

    }

    private User mapUserFromDocument(Document document) {
        return new User(UUID.fromString(document.getString("uuid")),
                document.getString("username"));
    }

    private UserDataSnapshot mapUserDataFromDocument(Document document) {
        final byte[] dataByteArray = document.get("data", Binary.class).getData();
        return new UserDataSnapshot(
                UUID.fromString(document.getString("version_uuid")),
                Date.from(Timestamp.valueOf(document.getString("timestamp")).toInstant()),
                DataSaveCause.getCauseByName(document.getString("save_cause")),
                document.getBoolean("pinned", false),
                getDataAdapter().fromBytes(dataByteArray));
    }

    private boolean collectionNotExists(final String collectionName) {
        return !database.listCollectionNames().into(new ArrayList<>()).contains(collectionName);
    }
}
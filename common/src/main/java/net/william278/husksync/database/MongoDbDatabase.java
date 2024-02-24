package net.william278.husksync.database;

import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.database.mongo.MongoConnectionHandler;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MongoDbDatabase extends Database {
    private MongoConnectionHandler mongoConnectionHandler;
    public MongoDbDatabase(@NotNull HuskSync plugin) {
        super(plugin);
    }

    /**
     * Initialize the database and ensure tables are present; create tables if they do not exist.
     *
     * @throws IllegalStateException if the database could not be initialized
     */
    @Override
    public void initialize() throws IllegalStateException {
        Settings.DatabaseSettings.MongoDatabaseCredentials credentials = plugin.getSettings().getDatabase().getMongoCredentials();
        mongoConnectionHandler = new MongoConnectionHandler(
                credentials.getHost(),
                credentials.getPort(),
                credentials.getUsername(),
                credentials.getPassword(),
                credentials.getDatabase(),
                credentials.getAuthDB()
        );
    }

    /**
     * Ensure a {@link User} has an entry in the database and that their username is up-to-date
     *
     * @param user The {@link User} to ensure
     */
    @Override
    public void ensureUser(@NotNull User user) {

    }

    /**
     * Get a player by their Minecraft account {@link UUID}
     *
     * @param uuid Minecraft account {@link UUID} of the {@link User} to get
     * @return An optional with the {@link User} present if they exist
     */
    @Override
    public Optional<User> getUser(@NotNull UUID uuid) {
        return Optional.empty();
    }

    /**
     * Get a user by their username (<i>case-insensitive</i>)
     *
     * @param username Username of the {@link User} to get (<i>case-insensitive</i>)
     * @return An optional with the {@link User} present if they exist
     */
    @Override
    public Optional<User> getUserByName(@NotNull String username) {
        return Optional.empty();
    }

    /**
     * Get the latest data snapshot for a user.
     *
     * @param user The user to get data for
     * @return an optional containing the {@link DataSnapshot}, if it exists, or an empty optional if it does not
     */
    @Override
    public Optional<DataSnapshot.Packed> getLatestSnapshot(@NotNull User user) {
        return Optional.empty();
    }

    /**
     * Get all {@link DataSnapshot} entries for a user from the database.
     *
     * @param user The user to get data for
     * @return The list of a user's {@link DataSnapshot} entries
     */
    @Override
    public @NotNull List<DataSnapshot.Packed> getAllSnapshots(@NotNull User user) {
        return null;
    }

    /**
     * Gets a specific {@link DataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link DataSnapshot} entry to get
     * @return An optional containing the {@link DataSnapshot}, if it exists
     */
    @Override
    public Optional<DataSnapshot.Packed> getSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
        return Optional.empty();
    }

    /**
     * <b>(Internal)</b> Prune user data for a given user to the maximum value as configured.
     *
     * @param user The user to prune data for
     * @implNote Data snapshots marked as {@code pinned} are exempt from rotation
     */
    @Override
    protected void rotateSnapshots(@NotNull User user) {

    }

    /**
     * Deletes a specific {@link DataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link DataSnapshot} entry to delete
     */
    @Override
    public boolean deleteSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
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
    @Override
    protected void rotateLatestSnapshot(@NotNull User user, @NotNull OffsetDateTime within) {

    }

    /**
     * <b>Internal</b> - Create user data in the database
     *
     * @param user The user to add data for
     * @param data The {@link DataSnapshot} to set.
     */
    @Override
    protected void createSnapshot(@NotNull User user, DataSnapshot.@NotNull Packed data) {

    }

    /**
     * Update a saved {@link DataSnapshot} by given version UUID
     *
     * @param user     The user whose data snapshot
     * @param snapshot The {@link DataSnapshot} to update
     */
    @Override
    public void updateSnapshot(@NotNull User user, DataSnapshot.@NotNull Packed snapshot) {

    }

    /**
     * Wipes <b>all</b> {@link User} entries from the database.
     * <b>This should only be used when preparing tables for a data migration.</b>
     */
    @Override
    public void wipeDatabase() {

    }

    /**
     * Close the database connection
     */
    @Override
    public void terminate() {

    }
}

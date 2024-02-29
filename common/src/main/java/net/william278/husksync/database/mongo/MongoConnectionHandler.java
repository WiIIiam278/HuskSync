package net.william278.husksync.database.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@Getter
public class MongoConnectionHandler {
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    /**
     * Initiate a connection to a Mongo Server
     * @param host The IP/Host Name of the Mongo Server
     * @param port The Port of the Mongo Server
     * @param username The Username of the user with the appropriate permissions
     * @param password The Password of the user with the appropriate permissions
     * @param databaseName The database to use.
     * @param authDb The database to authenticate with.
     */
    public MongoConnectionHandler(@NotNull String host, @NotNull Integer port, @NotNull String username, @NotNull String password, @NotNull  String databaseName, @NotNull String authDb) {
        final ServerAddress serverAddress = new ServerAddress(host, port);
        final MongoCredential credential = MongoCredential.createCredential(username, authDb, password.toCharArray());

        final MongoClientSettings settings = MongoClientSettings.builder()
                .credential(credential)
                .applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(serverAddress)))
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(databaseName);
    }

    /**
     * Close the connection with the database
     */
    public void closeConnection() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
    }
}

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

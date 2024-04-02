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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.UuidRepresentation;
import org.jetbrains.annotations.NotNull;

@Getter
public class MongoConnectionHandler {
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    /**
     * Initiate a connection to a Mongo Server
     * @param uri The connection string
     */
    public MongoConnectionHandler(@NotNull ConnectionString uri, @NotNull  String databaseName) {
        try {
            final MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(uri)
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase(databaseName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to establish a connection to the MongoDB database. " +
                    "Please check the supplied database credentials in the config file", e);
        }
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

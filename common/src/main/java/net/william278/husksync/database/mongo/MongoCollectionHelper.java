package net.william278.husksync.database.mongo;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

public class MongoCollectionHelper {
    private final MongoConnectionHandler database;

    /**
     * Initialize the collection helper
     * @param database Instance of {@link MongoConnectionHandler}
     */
    public MongoCollectionHelper(@NotNull MongoConnectionHandler database) {
        this.database = database;
    }

    /**
     * Create a collection
     * @param collectionName the collection name
     */
    public void createCollection(@NotNull String collectionName) {
        database.getDatabase().createCollection(collectionName);
    }

    /**
     * Delete a collection
     * @param collectionName the collection name
     */
    public void deleteCollection(@NotNull String collectionName) {
        database.getDatabase().getCollection(collectionName).drop();
    }

    /**
     * Get a collection
     * @param collectionName the collection name
     * @return MongoCollection<Document>
     */
    public MongoCollection<Document> getCollection(@NotNull String collectionName) {
        return database.getDatabase().getCollection(collectionName);
    }

    /**
     * Add a document to a collection
     * @param collectionName collection to add to
     * @param document Document to add
     */
    public void insertDocument(@NotNull String collectionName, @NotNull Document document) {
        MongoCollection<Document> collection = database.getDatabase().getCollection(collectionName);
        collection.insertOne(document);
    }

    /**
     * Update a document
     * @param collectionName collection the document is in
     * @param document filter of document
     * @param updates Bson of updates
     */
    public void updateDocument(@NotNull String collectionName, @NotNull Document document, @NotNull Bson updates) {
        MongoCollection<Document> collection = database.getDatabase().getCollection(collectionName);
        collection.updateOne(document, updates);
    }

    /**
     * Delete a document
     * @param collectionName collection the document is in
     * @param document filter to remove
     */
    public void deleteDocument(@NotNull String collectionName, @NotNull Document document) {
        MongoCollection<Document> collection = database.getDatabase().getCollection(collectionName);
        collection.deleteOne(document);
    }
}

package net.william278.husksync.database.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.william278.husksync.HuskSync;
import net.william278.husksync.util.Task;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MongoCacheHandler {
    private final Map<String, MongoCollection<Document>> collectionCache = new HashMap<>();
    private final MongoConnectionHandler db;

    public MongoCacheHandler(MongoConnectionHandler db, HuskSync plugin) {
        this.db = db;
        final AtomicReference<Task.Repeating> task = new AtomicReference<>();
        task.set(plugin.getRepeatingTask(() -> {

        },300L * 20L));
        task.get().run();
    }

    public void updateCache(String collectionName) {
        MongoDatabase database = db.getDatabase();
        collectionCache.put(collectionName, database.getCollection(collectionName));
    }

    public void removeFromCache(String collectionName) {
        MongoCollection<Document> cachedCollection = collectionCache.get(collectionName);
        if (cachedCollection != null) {
            MongoDatabase database = db.getDatabase();
            collectionCache.remove(collectionName, database.getCollection(collectionName));
        }
    }

    public MongoCollection<Document> getCachedCollection(String collectionName) {
        return collectionCache.get(collectionName);
    }
}

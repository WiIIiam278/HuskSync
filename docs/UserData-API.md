HuskSync provides an API for fetching and retrieving `UserData`; a snapshot of a user's synchronization.

This page assumes you've read the [[API]] introduction and have imported the HuskSync API into your repository.

## Table of contents
1. Creating a class to interface with the API
2. Checking if HuskSync is present and creating the hook
3. Getting an instance of the API
4. Getting a user by UUID
5. Getting a user's data
6. Getting a user's data
7. Getting a user's inventory contents
8. Updating a user's data

## 1. Creating a class to interface with the API
- Unless your plugin completely relies on HuskSync, you shouldn't put HuskSync API calls into your main class, otherwise if HuskSync is not installed you'll encounter `ClassNotFoundException`s

```java
public class HuskSyncAPIHook {

    public HuskSyncAPIHook() {
        // Ready to do stuff with the API
    }

}
```
## 2. Checking if HuskSync is present and creating the hook
- Check to make sure the HuskSync plugin is present before instantiating the API hook class

```java
public class MyPlugin extends JavaPlugin {

    public HuskSyncAPIHook huskSyncAPIHook;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("HuskSync") != null) {
            this.huskSyncAPIHook = new HuskSyncAPIHook();
        }
    }
}
```

## 3. Getting an instance of the API
- You can now get the API instance by calling `HuskSyncAPI#getInstance()`

```java
import net.william278.husksync.api.HuskSyncAPI;

public class HuskSyncAPIHook {

    private final HuskSyncAPI huskSyncAPI;

    public HuskSyncAPIHook() {
        this.huskSyncAPI = HuskSyncAPI.getInstance();
    }

}
```

## 4. CompletableFuture and Optional basics
- HuskSync's API methods return `CompletableFuture`s and `Optional`s.
- A `CompletableFuture` is an asynchronous callback mechanism. The method will be processed asynchronously and the data returned when it has been retrieved. While you can use `CompletableFuture#join()` to block the thread until the future has finished processing, it's smarter to use `CompletableFuture#thenAccept(data -> {})` to do what you want to do with the `data` you requested after it has asynchronously been retrieved, to prevent lag.
- An `Optional` is a null-safe representation of data, or no data. You can check if the Optional is empty via `Optional#isEmpty()` (which will be returned by the API if no data could be found for the call you made). If the optional does contain data, you can get it via `Optional#get()`.

## 5. Getting a user by UUID
- HuskSync has a `User` object, representing a user saved in the database. You can retrieve a user using `HuskSyncAPI#getUser(uuid)`
- If you have an online `org.bukkit.Player` object, you can use `BukkitPlayer#adapt(player)` to get an `OnlineUser` (extends `User`), representing a logged-in user. 

```java
public class HuskSyncAPIHook {

    private final HuskSyncAPI huskSyncAPI;

    public HuskSyncAPIHook() {
        this.huskSyncAPI = HuskSyncAPI.getInstance();
    }
    

    public void logUserName(UUID uuid) {
        // getUser() returns a CompletableFuture supplying an Optional<User>
        huskSyncAPI.getUser(uuid).thenAccept(optionalUser -> {
            // Check if we found a user by that UUID either online or on the database
            if (optionalUser.isEmpty()) {
                // If we didn't, we'll log that they don't exist
                System.out.println("User does not exist!");
                return;
            }
            // The User object has two fields; uuid and username.
            System.out.println("User name is: " + optionalUser.get().username);
        });
    }

}
```

## 6. Getting a user's data
- With a `User` object, we can now call `HuskSyncAPI#getUserData()` to fetch their latest data
- The `UserData` object contains eight data "modules", each holding certain parts of information. 
- UserData does not have to contain any single data "module"; the modules contained in a given UserData object when user data is saved by the plugin are determined by the plugin config settings. 
- You can fetch each module, which returns wrapped in an optional (empty if not present in the UserData object), via:
    - `UserData#getStatus();` - The user's status (health, hunger, saturation, exp, game mode, etc)
    - `UserData#getInventory();` - The user's inventory contents. Contains a Base 64 serialized `ItemStack` array.
    - `UserData#getEnderChest();` - The user's ender chest contents. Contains a Base 64 serialized `ItemStack` array.
    - `UserData#getPotionEffects();` - The user's active potion effects. Contains a Base 64 serialized `PotionEffect` array.
    - `UserData#getAdvancements();` - List of a user's advancements and mapped awarded criteria
    - `UserData#getStatistics();` - The user's statistics data containing four categories (untyped, items, blocks and entities)
    - `UserData#getLocation();` - The user's location data, for servers that have location syncing turned on.
    - `UserData#getPersistentDataContainer();` - The user's peristent data container, containing a map of keys to strings

```java
public class HuskSyncAPIHook {

    // ... //

    public void logUserData(UUID uuid) {
        huskSyncAPI.getUser(uuid).thenAccept(optionalUser -> {
            // Optional#isPresent() is the opposite of #isEmpty()
            if (optionalUser.isPresent()) {
                logHealth(optionalUser.get());
            }
        });
    }

    private void logHealth(User user) {
        // A user might not have data, if it's deleted by an admin or they're brand new
        huskSyncAPI.getUserData(user).thenAccept(optionalUserData -> {
            if (optionalUserData.isPresent()) {
                // Get the StatusData from the UserData object
                Optional<StatusData> statusData = optionalUserData.get().getStatus();
                // Print the health from the fields, if the user has a status object
                statusData.ifPresent(status -> {
                    System.out.println(user.username + "'s health: " + status.health + "/" + status.maxHealth);
                });
            }
        });
    }

}
```

## 7. Getting a user's inventory contents
- The API provides methods for deserialzing `ItemData` used to hold Base 64 serialized inventory and ender chest `ItemStack` array contents into actual `ItemStack` array data.
- For deserialziing inventories, use `HuskSyncAPI#deserializeInventory(serializedItems)`
- For deserialziing ender chests, use `HuskSyncAPI#deserializeItemStackArray(serializedItems)`
- Alternatively, the `HuskSyncAPI#getPlayerInventory(user)` and `HuskSyncAPI#getPlayerEnderChest(user)` methods will do this for you nice and easily. Note though if the last UserData module does not contain UserData, the ItemData returned here will be representative of an empty ItemStack array.
- Serialization and deserialization methods are also available for Potion Effects.

```java
public class HuskSyncAPIHook {

    // ... //

    private void printInventoryItems(User user) {
        huskSyncAPI.getUserData(user).thenAccept(optionalUserData -> {
            if (optionalUserData.isPresent()) {
                // Get the ItemData and make sure it's present
                Optional<ItemData> inventoryDataOptional = optionalUserData.get().getInventory();
                if (inventoryDataOptional.isEmpty) {
                    return;
                }
                ItemData inventoryData = inventoryDataOptional.get();

                // Get the ItemStack[] array as a BukkitInventoryMap.
                // This returns a future, but we're using #join() to block the thread until it's done
                BukkitInventoryMap inventory = huskSyncAPI.deserializeInventory(inventoryData.serializedItems).join();
                
                // A BukkitInventoryMap is simply a wrapper for an ItemStack array.
                // It provides a few handy methods for getting the player's armor, their offhand item, etc.
                // To get the ItemStack array from it, just call BukkitInventoryMap#getContents();
                for (ItemStack item : inventory.getContents()) {
                    // Print out the item material types of every item in the player's inventory
                    System.out.println(item.getType().name());
                }
            }
        });
    }

}
```

<details>
<summary>HuskSyncAPI#getPlayerInventory()</summary>

```java
private void printInventoryItems(User user) {
    huskSyncAPI.getPlayerInventory(user).thenAccept(inventory -> {
        if (inventory.isPresent()) {
            for (ItemStack item : inventory.get().getContents()) {
                System.out.println(item.getType().name());
            }
        }
    });
}
```
</details>

<details>
<summary>HuskSyncAPI#getPlayerEnderChest()</summary>

```java
private void printEnderChestItems(User user) {
    huskSyncAPI.getPlayerEnderChest(user).thenAccept(enderChest -> {
        if (enderChest.isPresent()) {
            for (ItemStack item : enderChest.get()) {
                System.out.println(item.getType().name());
            }
        }
    });
}
```
</details>


## 8. Updating a user's data
- You can use `HuskSyncAPI#setUserData(user, userData)` to set a user's modified data to the database.
- If you need to modify user data every time it's updated, you may want to look at listening to one of HuskSync's provided events instead.
- You can use `HuskSyncAPI#serializeItemStackArray(itemStack[])` to serialize an array of ItemStacks into Base 64.
- Alternatively, you can use `HuskSyncAPI#setInventoryData(user, bukkitInventoryMap)` to set a user's inventory, or `HuskSyncAPI#setEnderChestData(user, itemStack[])` to set a user's ender chest.
- Updating UserData will overwrite the player's current "live" data. HuskSync does not track the player's "live" data constantly by design, only "snapshots" of their data when it is saved. In other words, getting and updating a user's data may actually end up rolling them back.

```java
public class HuskSyncAPIHook {

    // Set a user's health to 20
    private void updateUserHealth(User user) {
        huskSyncAPI.getUserData(user).thenAccept(optionalUserData -> {
            if (optionalUserData.isPresent()) {
                UserData data = optionalUserData.get();
                Optional<StatusData> statusDataOptional = data.getStatus();
                StatusData statusData = statusDataOptional.get();            
                statusData.health = 20;
    
                // This returns a CompletableFuture<Void> that will invoke #thenRun() when it has completed
                huskSyncAPI.setUserData(user, data).thenRun(() -> {
                    System.out.println("Healed " + user.username + "!");
                });
            }
        });
    }

}
```
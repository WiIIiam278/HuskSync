HuskSync v2.0 provides an API for fetching and retrieving `UserData`; a snapshot of a user's synchronization.

> **Warning:** API v2 is no longer supported or compatible with HuskSync v3.0. See [[Data Snapshot API]] for the equivalent v3 API. 🚨

This page assumes you've read the general [[API]] introduction and imported HuskSync (v2.x) into your project, and added it as a dependency.

🚨 HuskSync API v2 only targets HuskSync v2.0-2.2.8. It is **not compatible with HuskSync v3.0+**. The equivalent API for HuskSync v3 is the [[Data Snapshot API]].

## Table of Contents
1. [Getting a user by UUID](#1-getting-a-user-by-uuid)
2. [Getting a user's data](#2-getting-a-users-data)
3. [Getting a user's inventory contents](#3-getting-a-users-inventory-contents)
4. [Updating a user's data](#4-updating-a-users-data)

## 1. Getting a user by UUID
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
            System.out.println("User name is: %s", optionalUser.get().username);
        });
    }

}
```

## 2. Getting a user's data
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
                    System.out.println("%s's health: %d/%d", user.username, status.health, status.maxHealth);
                });
            }
        });
    }

}
```

## 3. Getting a user's inventory contents
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


## 4. Updating a user's data
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
                    System.out.println("Healed %s!", user.username);
                });
            }
        });
    }

}
```
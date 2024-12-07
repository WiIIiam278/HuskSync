HuskSync v3.0 provides an API for getting, creating, editing and deleting  `DataSnapshot`s; a snapshot of a user at a given moment in time. This page will walk you through how to manipulate delete snapshots using the HuskSync API.

This page assumes you have read the general [[API]] introduction and that you have both imported HuskSync (v3.x) into your project and added it as a dependency.

## Table of Contents
1. [Getting a User](#1-getting-a-user)
2. [Getting DataSnapshots for a User](#2-getting-datasnapshots-for-a-user)
    1. [Getting a User's current data](#21-getting-a-users-current-data)
    2. [Getting a User's latest saved DataSnapshot](#22-getting-a-users-latest-saved-datasnapshot)
    3. [Getting a list of a User's saved DataSnapshots](#23-getting-a-list-of-a-users-saved-datasnapshots)
3. [Packing and Unpacking DataSnapshots](#3-packing-and-unpacking-datasnapshots)
4. [Getting and setting data in a DataSnapshot](#4-getting-and-setting-data-in-a-datasnapshot)
    1. [Data Types](#41-data-types)
    2. [Editing Health, Hunger, Experience, and GameMode data](#42-editing-health-hunger-experience-and-gamemode-data)
    3. [Editing Inventory and Ender Chest data](#43-editing-inventory-and-ender-chest-data)
    4. [Editing Location data](#44-editing-location-data)
    5. [Editing Advancement data](#45-editing-advancement-data)
5. [Creating new DataSnapshots](#5-creating-new-datasnapshots)
    1. [Creating a new snapshot from a player's current data](#51-creating-a-new-snapshot-from-a-players-current-data)
    2. [Creating a new snapshot from scratch](#52-creating-a-new-snapshot-from-scratch)
6. [Deleting DataSnapshots](#6-deleting-datasnapshots)

## 1. Getting a User
* HuskSync has a `User` object, representing a user saved in the database. You can retrieve a user using `HuskSyncAPI#getUser(uuid)`

<details>
<summary>Code Example &mdash; Getting a user by UUID</summary>

```java
// getUser() returns a CompletableFuture supplying an Optional<User>
huskSyncAPI.getUser(uuid).thenAccept(optionalUser -> {
    // Check if we found a user by that UUID either online or on the database
    if (optionalUser.isEmpty()) {
        // If we didn't, we'll log that they don't exist
        System.out.println("User does not exist!");
        return;
    }
    
    // The User object provides methods for getting a user's UUID and username
    System.out.println("Found %s", optionalUser.get().getUsername());
});
```
</details>

* If you have an online `org.bukkit.Player` object, you can use `BukkitPlayer#adapt(player)` to get an `OnlineUser` (extends `User`), representing a logged-in user.
* You can also use `#getOnlineUser(UUID)` to get an OnlineUser by their UUID - note this only works for players online on the server the logic is called from, however.

<details>
<summary>Code Example &mdash; Getting an online user</summary>

```java
// Get an online user
OnlineUser user = huskSyncAPI.getUser(player);
System.out.println("Hello, %s!", user.getUsername());
```
</details>

## 2. Getting DataSnapshots for a User
### 2.1 Getting a User's current data
* HuskSync provides a method for getting the user's current data. `HuskSyncAPI#getCurrentData(User)` returns a `CompletableFuture` supplying an `Optional<DataSnapshot.Unpacked>`.
* This method will get the user's current data if they are online on the network, or their last saved data if they are offline (or, an empty optional if they user has never logged on).

<details>
<summary>Code Example &mdash; Getting a user's current DataSnapshot</summary>

```java
// Get a user's current data
huskSyncAPI.getCurrentData(user).thenAccept(optionalSnapshot -> {
    if (optionalSnapshot.isEmpty()) {
        System.out.println("Couldn't get data for %s", user.getUsername());
        return;
    }
    
    // Get the snapshot, which you can then do stuff with
    DataSnapshot.Unpacked snapshot = optionalSnapshot.get();
});
```
</details>

### 2.2 Getting a User's latest saved DataSnapshot
* Note that the snapshot returned by the previous method _is not necessarily saved in the database_. 
* If you want to get the user's last saved snapshot, use `HuskSyncAPI#getLatestSnapshot(User)` instead:

<details>
<summary>Code Example &mdash; Getting a user's latest saved DataSnapshot</summary>

```java
// Get a user's latest saved snapshot
huskSyncAPI.getLatestSnapshot(user).thenAccept(optionalSnapshot -> {
    if (optionalSnapshot.isEmpty()) {
        System.out.println("%s has no saved snapshots!", user.getUsername());
        return;
    }
    
    // Get the snapshot, which you can then do stuff with
    DataSnapshot.Unpacked snapshot = optionalSnapshot.get();
});
```
</details>

### 2.3 Getting a list of a User's saved DataSnapshots
* If you want to get a list of all of a user's saved snapshots, you can use `HuskSyncAPI#getSnapshots(User)`. This method returns a `CompletableFuture` supplying an `Optional<List<DataSnapshot.Unpacked>>`.

<details>
<summary>Code Example &mdash; Getting all of a user's saved DataSnapshots</summary>

```java
// Get a user's saved snapshots
huskSyncAPI.getSnapshots(user).thenAccept(optionalSnapshots -> {
    if (optionalSnapshots.isEmpty()) {
        System.out.println("%s has no saved snapshots!", user.getUsername());
        return;
    }
    
    // Get the list of snapshots, which you can then do stuff with
    List<DataSnapshot.Unpacked> snapshots = optionalSnapshots.get();
});
```
</details>

## 3. Packing and Unpacking DataSnapshots
* HuskSync provides two types of `DataSnapshot` objects: `DataSnapshot.Packed` and `DataSnapshot.Unpacked`.
    - `DataSnapshot.Packed` is a snapshot that has had its data serialized into a byte map. This snapshot is ready to be saved in the database or set to Redis.
    - `DataSnapshot.Unpacked` is a snapshot that has been unpacked from a `DataSnapshot.Packed` object. You can get, set, and manipulate data from these snapshots.
* Most of the time, you won't need to worry about this, as HuskSync typically deals with `Unpacked` snapshots. However, if you need to convert between the two (e.g., if you wish to copy the snapshot), you can use the `HuskSyncAPI#packSnapshot(DataSnapshot.Unpacked)` and `HuskSyncAPI#unpackSnapshot(DataSnapshot.Packed)` methods.
* The editor method `HuskSyncAPI#editPackedSnapshot(DataSnapshot.Packed, Consumer<DataSnapshot.Unpacked>)` additionally provides a utility for unpacking, editing, then repacking a packed `DataSnapshot` object.

<details>
<summary>Code Example &mdash; Packing and unpacking snapshots</summary>

```java
// Get a user's current data
huskSyncAPI.getCurrentData(user).thenAccept(optionalSnapshot -> {
    if (optionalSnapshot.isEmpty()) {
        System.out.println("User does not exist!");
        return;
    }
    
    // Get the snapshot
    DataSnapshot.Unpacked snapshot = optionalSnapshot.get();
    
    // Pack the snapshot
    DataSnapshot.Packed packedSnapshot = huskSyncAPI.packSnapshot(snapshot);
    // You can call #copy() on a packed snapshot to make a copy of it, which you can then edit
    
    // Unpack the snapshot again
    DataSnapshot.Unpacked unpackedSnapshot = huskSyncAPI.unpackSnapshot(packedSnapshot);
});
```
</details>

## 4. Getting and setting data in a DataSnapshot
* Data within `DataSnapshot`s is represented by `Data` objects of different types; `Data.Items.Inventory` represents Inventory data, `Data.Health` represents a user's current/max health, `Data.Hunger` represents a user's current/max hunger, and so on. 
* On Bukkit servers, `BukkitData` classes implement `Data` classes and provide utilities for converting between `Bukkit` and `HuskSync` data types.
* `DataSnapshot.Unpacked` provides methods for getting and setting `Data` in the snapshot, such as `DataSnapshot.Unpacked#getInventory()` (which returns an `Optional`) and `DataSnapshot.Unpacked#setHealth(Data.Health)`.

### 4.1 Data Types
| Identifier Key             | Description                          | Get Method            | Set Method             |
|----------------------------|--------------------------------------|-----------------------|------------------------|
| `husksync:inventory`       | User inventories & held item slot    | `#getInventory`       | `#setInventory`        |
| `husksync:ender_chest`     | User Ender Chests                    | `#getEnderChest`      | `#setEnderChest`       |
| `husksync:potion_effects`  | User active potion effects           | `#getPotionEffects`   | `#setPotionEffects`    |
| `husksync:advancements`    | User advancements                    | `#getAdvancements`    | `#setAdvancements`     |
| `husksync:location`        | User location                        | `#getLocation`        | `#setLocation`         |
| `husksync:statistics`      | User statistics                      | `#getStatistics`      | `#setStatistics`       |
| `husksync:health`          | User health                          | `#getHealth`          | `#setHealth`           |
| `husksync:hunger`          | User hunger, saturation & exhaustion | `#getHunger`          | `#setHunger`           |
| `husksync:attributes`      | User attributes                      | `#getAttributes`      | `#setAttributes`       |
| `husksync:experience`      | User level, experience, and score    | `#getExperience`      | `#setExperience`       |
| `husksync:game_mode`       | User game mode                       | `#getGameMode`        | `#setGameMode`         |
| `husksync:flight_status`   | User ability to fly/if flying now    | `#getFlightStatus`    | `#setFlightStatus`     |
| `husksync:persistent_data` | User persistent data container       | `#getPersistentData`  | `#setPersistentData`   |
| Custom types; `plugin:foo` | Any custom data                      | `#getData(Identifer)` | `#setData(Identifier)` |

* Plugin developers may supply their own `Data` classes for synchronisation & saving by implementing the `Data` interface and registering a `Serializer<>` for their class to an `Identifier`. See the [[Custom Data API]] page for more information.
* You can only get data from snapshots where a serializer has been registered for it on this server and, in the case of the built-in data types, where the sync feature has been enabled in the [[Config File]]. If you try to get data from a snapshot where the data type is not supported, you will get an empty `Optional`.

### 4.2 Editing Health, Hunger, Experience, and GameMode data
* `DataSnapshot.Unpacked#getHealth()` returns an `Optional<Data.Health>`, which you can then use to get the player's current health.
* `DataSnapshot.Unpacked#setHealth(Data.Health)` sets the player's current health. You can create a `Health` instance to pass on the Bukkit platform through `BukkitData.Health.from(double, double)`.
* Similar methods exist for Hunger, Experience, and GameMode data types
* Once you've updated the data in the snapshot, you can save it to the database using `HuskSyncAPI#setCurrentData(user, userData)`.

<details>
<summary>Code Example &mdash; Getting and setting a player's health</summary>

```java
// Get a user's current data
huskSyncAPI.getCurrentData(user).thenAccept(optionalSnapshot -> {
    if (optionalSnapshot.isEmpty()) {
        System.out.println("User does not exist!");
        return;
    }
    
    // Get the player's health and game mode from the snapshot
    DataSnapshot.Unpacked snapshot = optionalSnapshot.get();
    Optional<Data.Health> healthOptional = snapshot.getHealth();
    if (healthOptional.isEmpty()) {
        System.out.println("User has no health data!");
        return;
    }
    Optional<Data.GameMode> gameModeOptional = snapshot.getGameMode();
    if (gameModeOptional.isEmpty()) {
        System.out.println("User has no game mode data!");
        return;
    }
    Optional<Data.FlightStatus> flightStatusOptional = snapshot.getFlightStatus();
    if (flightStatusOptional.isEmpty()) {
        System.out.println("User has no flight status data!");
        return;
    }
    // getExperience() and getHunger() work similarly
        
    // Get the health data
    Data.Health health = healthOptional.get();
    double currentHealth = health.getCurrentHealth(); // Current health
    double healthScale = health.getHealthScale(); // Health scale (used to determine health/damage display hearts)
    snapshot.setHealth(BukkitData.Health.from(20, 20, true));
    // Need max health? Look at the Attributes data type.
    
    // Get the game mode data
    Data.GameMode gameMode = gameModeOptional.get();
    String gameModeName = gameMode.getGameModeName(); // Game mode name (e.g., "SURVIVAL")
    snapshot.setGameMode(BukkitData.GameMode.from("SURVIVAL"));
    
    // Get flight data
    Data.FlightStatus flightStatus = flightStatusOptional.get(); // Whether the player is flying
    boolean isFlying = flightStatus.isFlying(); // Whether the player is *currently* flying
    boolean canFly = flightStatus.isAllowFlight(); // Whether the player *can* fly
    snapshot.setFlightStatus(BukkitData.FlightStatus.from(false, false));
    
    // Save the snapshot - This will update the player if online and save the snapshot to the database
    huskSyncAPI.setCurrentData(user, snapshot);
});
```
</details>

* To make this code more concise, we can use the `HuskSyncAPI#editCurrentData()` method to get the user's current data and perform an operation in a `Consumer` class.
* The result of editing the `DataSnapshot` object in the `Consumer` is then automatically passed to `HuskSyncAPI#setCurrentData()` to save the snapshot to the database and update the user if they are online

<details>
<summary>Code Example &mdash; Editing a player's health</summary>

```java
// Edit a user's current data
huskSyncAPI.editCurrentData(user, snapshot -> {
    // Get the player's health
    Optional<Data.Health> healthOptional = snapshot.getHealth();
    if (healthOptional.isEmpty()) {
        System.out.println("User has no health data!");
        return;
    }
    
    // Get the health data
    Data.Health health = healthOptional.get();
    
    // Get the player's current health
    double currentHealth = health.getCurrentHealth();
    
    // Set the player's health / health scale
    snapshot.setHealth(BukkitData.Health.from(20, 20));
});
```
</details>

### 4.3 Editing Inventory and Ender Chest data
* We can get a player's inventory using `DataSnapshot.Unpacked#getInventory()`, which returns an `Optional<Data.Items.Inventory>`. You can also get the player's Ender Chest inventory using `DataSnapshot.Unpacked#getEnderChest()`.
* `Data.Items.Inventory` provides methods for the player's inventory, armor, offhand, and ender chest items as platform-agnostic `Stack` objects, which lets you view basic Item information, but does not expose their full NBT data.
* On Bukkit, simply cast a `Data.Items.(Inventory/EnderChest)` to a `BukkitData.Items.(Inventory/EnderChest)` to get access to the Bukkit `ItemStack[]` contents of the player's items, allowing you to edit the contents.

<details>
<summary>Code Example &mdash; Getting and setting a player's inventory or Ender Chest</summary>

```java
// Get a user's current data
huskSyncAPI.getCurrentData(user).thenAccept(optionalSnapshot -> {
    if (optionalSnapshot.isEmpty()) {
        System.out.println("User does not exist!");
        return;
    }
    
    // Get the snapshot
    DataSnapshot.Unpacked snapshot = optionalSnapshot.get();
    
    // Get the player's inventory
    Optional<Data.Items.Inventory> inventoryOptional = snapshot.getInventory();
    if (inventoryOptional.isEmpty()) {
        System.out.println("User has no inventory data!");
        return;
    }
    
    // Get the inventory data
    Data.Items.Inventory inventory = inventoryOptional.get();
    
    // Get the player's inventory contents
    ItemStack[] inventoryContents = ((BukkitData.Items.Inventory) inventory).getContents();
    
    // Set the player's inventory contents
    ((BukkitData.Items.Inventory) inventory).setContents(inventoryContents);
    
    // Save the snapshot - This will update the player if online and save the snapshot to the database
    huskSyncAPI.setCurrentData(user, snapshot);
});
```
</details>

* There also exist utility methods for both getting and setting just the player's current inventory or Ender Chest contents, if all you need to do is just update the player's inventory/Ender Chest.
* The Inventory methods are `#getCurrentInventory`, `#setCurrentInventory`, and `#editCurrentInventory`. For Ender chests, these are `#getCurrentEnderChest`, `#setCurrentEnderChest`, and `#editCurrentEnderChest`. There's also `Contents` methods that just deal with ItemStacks[], if you prefer.

<details>
<summary>Code Example &mdash; Editing a player inventory</summary>

```java
// Edit a user's current inventory
huskSyncAPI.editCurrentInventory(user, inventory -> {
    // Get the player's inventory contents
    ItemStack[] inventoryContents = ((BukkitData.Items.Inventory) inventory).getContents();
    
    // The array of ItemStacks is a copy of the player's inventory contents (Typically an array of length 42)
    inventoryContents[0] = new ItemStack(Material.DIAMOND_SWORD);
    inventoryContents[1] = null; // null = an empty slot
    
    // Set the player's inventory contents
    ((BukkitData.Items.Inventory) inventory).setContents(inventoryContents);
});
```
</details>

### 4.4 Editing Location data
* HuskSync's support for player Locations is intended for mirrored world instances (such as RPG servers), and so is disabled by default in the plugin config.
* We can get a player's location using `DataSnapshot.Unpacked#getLocation()`, which returns an `Optional<Data.Location>`.
* `Data.Location` provides methods for getting and setting the player's location, pitch, and yaw. We can also use the aforementioned `BukkitData` classes to set this using a `org.bukkit.Location`, and speed things along using the `HuskSyncAPI#editCurrentData` method.

<details>
<summary>Code Example &mdash; Editing a player's location</summary>

```java
// Edit a user's current data
huskSyncAPI.editCurrentData(user, snapshot -> {
    // Get the player's location
    Optional<Data.Location> locationOptional = snapshot.getLocation();
    if (locationOptional.isEmpty()) {
        System.out.println("User has no location data!");
        return;
    }
    
    // Get the location data
    Data.Location location = locationOptional.get();
    
    // Get the player's location
    org.bukkit.Location bukkitLocation = ((BukkitData.Location) location).getLocation();
    
    // Set the player's location
    ((BukkitData.Location) location).setLocation(bukkitLocation);
});
```
</details>

### 4.5 Editing Advancement data
* Advancements can be retrieved using `DataSnapshot.Unpacked#getAdvancements()`, which returns an `Optional<Data.Advancements>`.
* `Data.Advancements` provides a wrapper for a list of `Data.Advancements.Advancement` objects, representing a map of a player's completed criteria when progressing to complete an advancement.
* You can add and remove advancements from the list, and set the list of advancements using `Data.Advancements#setAdvancements(List<Data.Advancements.Advancement>)`.

<details>
<summary>Code Example &mdash; Editing a player's advancements</summary>

```java
// Edit a user's current data
huskSyncAPI.editCurrentData(user, snapshot -> {
    // Get the player's advancements
    Optional<Data.Advancements> advancementsOptional = snapshot.getAdvancements();
    if (advancementsOptional.isEmpty()) {
        System.out.println("User has no advancements data!");
        return;
    }
    
    // Get the advancements data
    Data.Advancements advancements = advancementsOptional.get();
    
    // Get the player's advancements
    List<Data.Advancements.Advancement> playerAdvancements = new ArrayList<>(advancements.getAdvancements());
    
    // Advancement progress is represented by completed critera entries, mapped to when said criteria was completed
    Map<String, Date> criteria = Map.of("criteria_item_1", new Date());
    
    // Add an advancement to the player's advancements
    playerAdvancements.add(Data.Advancements.Advancement.adapt("foo:bar/baz", criteria));
    
    // Remove all "recipe" advancements from the player's advancements
    playerAdvancements.removeIf(advancement -> advancement.getIdentifier().startsWith("minecraft:recipes/"));
    
    // Set the player's advancements
    advancements.setAdvancements(playerAdvancements);
});
```
</details>

## 5. Creating new DataSnapshots
* HuskSync provides methods for creating new snapshots; either by capturing a player's current data or by creating a new snapshot from scratch using a `DataSnapshot.Builder`.

### 5.1 Creating a new snapshot from a player's current data
* You can create a new snapshot from a player's current data using `HuskSyncAPI#createSnapshot(OnlineUser)`, which returns a `DataSnapshot.Packed` with a save cause of `SaveCause.API`.

<details>
<summary>Code Example &mdash; Capturing a player's current data into a Snapshot</summary>

```java
// Create a new snapshot from a player's current data
final DataSnapshot.Packed data = huskSyncAPI.createSnapshot(user);

// editPackedSnapshot() provides a utility for unpacking, editing, then repacking a DataSnapshot object
final DataSnapshot.Packed edited = huskSyncAPI.editPackedSnapshot(data, (unpacked) -> {
    unpacked.setHealth(BukkitData.Health.from(10, 20, 20)); // Example - sets the user's health to 10 (5 hearts)
});

// Save the snapshot - This will save the snapshot to the database
huskSyncAPI.addSnapshot(edited);
```
</details>

### 5.2 Creating a new snapshot from scratch
* You can create a new snapshot from scratch using a `DataSnapshot.Builder`. This is useful if you want to create a custom snapshot with specific data and apply it to a user.
* Get a new `DataSnapshot.Builder` using `HuskSyncAPI#snapshotBuilder()`.

<details>
<summary>Code Example &mdash; Creating a new snapshot from scratch</summary>

```java
// Create a new snapshot from scratch
final DataSnapshot.Builder builder = huskSyncAPI.snapshotBuilder();

// Create an empty inventory with a diamond sword in the first slot
final BukkitData.Items.Inventory inventory = BukkitData.Items.Inventory.empty();
inventory.setContents(new ItemStack[] { new ItemStack(Material.DIAMOND_SWORD) });
inventory.setHeldItemSlot(0); // Set the player's held item slot to the first slot

// Use the builder to create, then pack, a new snapshot
final DataSnapshot.Packed packed = builder
        .saveCause(SaveCause.API) // This is the default save cause, but you can change it if you want
        .setTimestamp(OffsetDateTime.now().minusDays(3)) // Set the timestamp to 3 days ago
        .setInventory(inventory) // Set the player's inventory
        .setHealth(BukkitData.Health.from(10, 20, 20)) // Set the player to having 5 hearts
        .buildAndPack(); // You can also call just #build() to get a DataSnapshot.Unpacked

// Save the snapshot - This will save the snapshot to the database for a User
huskSyncAPI.addSnapshot(user, packed);
```
</details>

## 6. Deleting DataSnapshots
* You can delete a snapshot using `HuskSyncAPI#deleteSnapshot(User, UUID)`, which will delete a snapshot from the database by its UUID.
* This method returns a CompletableFuture<Boolean> which will resolve to `true` if there was a snapshot with that UUID to delete, or `false` if there was no snapshot with that UUID to delete.

<details>
<summary>Code Example &mdash; Deleting a snapshot</summary>

```java
// Delete a snapshot
huskSyncAPI.deleteSnapshot(user, uuid).thenAccept(success -> {
    if (success) {
        System.out.println("Deleted snapshot with UUID %s", uuid);
    } else {
        System.out.println("No snapshot with UUID %s to delete", uuid);
    }
});
```
</details>
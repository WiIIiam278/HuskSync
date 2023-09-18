HuskSync v3.0 provides an API for getting, creating, editing and deleting  `DataSnapshot`s; a snapshot of a user at a given moment in time. This page will walk you through how to manipulate delete snapshots using the HuskSync API.

This page assumes you have read the general [[API]] introduction and that you have both imported HuskSync (v3.x) into your project and added it as a dependency.

## Table of Contents
1. [Getting a User](#1-getting-a-user)

## 1. Getting a User
* HuskSync has a `User` object, representing a user saved in the database. You can retrieve a user using `HuskSyncAPI#getUser(uuid)`
* If you have an online `org.bukkit.Player` object, you can use `BukkitPlayer#adapt(player)` to get an `OnlineUser` (extends `User`), representing a logged-in user.

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
* `DataSnapshot.Unpacked` provides methods for getting and setting data in the snapshot, such as `DataSnapshot.Unpacked#getInventory()`.

(WIP)
HuskSync provides three API events your plugin can listen to when certain parts of the data synchronization process are performed. These events deal with HuskSync class types, so you may want to familiarize yourself with the [API basics](API) first. Two of the events can be cancelled (thus aborting the synchronization process at certain stages), and some events expose methods letting you affect their outcome (such as modifying the data that is saved during the process).

Consult the Javadocs for more information. Please note that carrying out expensive blocking operations during these events is strongly discouraged as this may affect plugin performance.

## Bukkit Platform Events
> **Tip:** Don't forget to register your listener when listening for these event calls.
 
| Bukkit Event class        | Cancellable | Description                                                                                 |
|---------------------------|:-----------:|---------------------------------------------------------------------------------------------|
| `BukkitDataSaveEvent`     |      ✅      | Called when player data snapshot is created, saved and cached due to a DataSaveCause        |
| `BukkitPreSyncEvent`      |      ✅      | Called before a player has their data updated from the cache or database, just after login  |
| `BukkitSyncCompleteEvent` |      ❌      | Called once a player has completed their data synchronization on login successfully&dagger; |

## Fabric Platform Callbacks
> Access the callback via the static EVENT field in each interface class.

| Fabric Callback              | Cancellable | Description                                                                                 |
|------------------------------|:-----------:|---------------------------------------------------------------------------------------------|
| `FabricDataSaveCallback`     |      ✅      | Called when player data snapshot is created, saved and cached due to a DataSaveCause        |
| `FabricPreSyncCallback`      |      ✅      | Called before a player has their data updated from the cache or database, just after login  |
| `FabricSyncCompleteCallback` |      ❌      | Called once a player has completed their data synchronization on login successfully&dagger; |

&dagger;This can also fire when a user's data is updated while the player is logged in; i.e., when an admin rolls back the user, updates their inventory or Ender Chest through the respective commands, or when an API call is made forcing the user to have their data updated.

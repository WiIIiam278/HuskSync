If your server uses the `keepInventory` gamerule, where players keep the contents of their inventory after dying, HuskSync's built-in snapshot-on-death and dead-player synchronisation features can cause a conflict leading to synchronisation issues.

To solve this issue, you will need to adjust three settings in your `config.yml` file, as described below.

## Why does this happen?
HuskSync has some special handling when players die, to account for scenarios where users change servers after death (to prevent item loss).
* **Death state saving**&mdash;HuskSync has special logic to save player snapshots *except their inventory* when they change servers while dead. When `keepInventory` is enabled, though, the inventory still contains items, so the snapshot is not saved correctly. This logic is enabled by default.
* **Snapshot creation on death**&mdash;HuskSync can create a special snapshot for backup purposes when a player dies, formed by taking their drops and setting this to their inventory. When `keepInventory` is enabled, the player drops are empty, so this creates an inaccurate snapshot. This option is disabled by default.

## How can this be fixed?
You will need to set the `synchronization.save_on_death` (which controls making snapshots on death), `save_empty_drops_on_death` (which controls whether snapshots of players who have no items to drop should be created), and `synchronization.synchronise_dead_players_changing_server` (which controls whether to sync dead players when they change servers) options to `false` in `config.yml`.

<details>
  <summary>Example in config.yml</summary>
  
  ```yml
     synchronization:
       # ...
       save_on_death: false # <-- Set this to false
       save_empty_drops_on_death: false # <-- Set this to false
       # ...
       synchronise_dead_players_changing_server: false # <-- Set this to false
  ```
  
</details>


## Troubleshooting with custom keepInventory setups
If the above doesn't work for you, you may need to do more things to get this to work properly.

If your server uses an advanced custom setup where some items are kept and others are not through custom plugin logic, you'll need to use the HuskSync API to create a custom hook to update data on the DataSaveEvent when a player changes server *while dead*, transforming their inventory data as appropriate.

If your server uses a permission node to control whether the user keeps their inventory on death, you should be able to follow the above instructions although your mileage may vary dependent on your setup and how you handle players when they die. Note that this option may also conflict with other plugins that make assumptions about the persistence of items on death.
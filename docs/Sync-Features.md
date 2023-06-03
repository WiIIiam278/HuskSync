This page contains a list of the features HuskSync is and isn't able to syncrhonise on your server.

You can customise how much data HuskSync saves about a player by [turning each synchronisation feature on or off](#toggling-sync-features). When a synchronisation feature is turned off, HuskSync won't touch that part of a player's profile; in other words, the data they will inherit when changing servers will be read from their player data file on the local server.

## Feature table
✅&mdash;Supported&nbsp; ❌&mdash;Unsupported&nbsp; ⚠️&mdash;Experimental

| Name                      | Description                                                 | Availability |
|---------------------------|-------------------------------------------------------------|:------------:|
| Inventories               | Items in player inventories & selected hotbar slot          |      ✅      |
| Ender chests              | Items in ender chests&midast;                               |      ✅      |
| Health                    | Player health points                                        |      ✅      |
| Max health                | Player max health points and health scale                   |      ✅      |
| Hunger                    | Player hunger, saturation & exhaustion                      |      ✅      |
| Experience                | Player level, experience points & score                     |      ✅      |
| Potion effects            | Active status effects on players                            |      ✅      |
| Advancements              | Player advancements, recipes & progress                     |      ✅      |
| Game modes                | Player's current game mode                                  |      ✅      |
| Statistics                | Player's in-game stats (ESC -> Statistics)                  |      ✅      |
| Location                  | Player's current coordinate positon and world&dagger;       |      ✅      |
| Persistent Data Container | Custom plugin persistent data key map                       |      ⚠️      |
| Locked maps               | Maps/treasure maps locked in a cartography table            |      ⚠️      |
| Unlocked maps             | Regular, unlocked maps/treasure maps ([why?](#map-syncing)) |      ❌      |
| Economy balances          | Vault economy balance. ([why?](#economy-syncing))           |      ❌      |

&midast;Purpur's custom ender chest resizing feature is also supported.

&dagger;This is intended for servers that have mirrorred worlds across instances (such as RPG servers). With this option enabled, players will be placed at the same coordinates when changing servers.

### PersistentDataContainer tags
The player [PersistentDataContainer](https://blog.jeff-media.com/persistent-data-container-the-better-alternative-to-nbt-tags/) is a part of the Spigot API that enables plugins to set custom data tags to players, entities & items and have them persist. HuskSync will synchronise this data cross-server. Plugins that use legacy or propietary forms of saving data, such as by modifying NBT directly, may not correctly synchronise.

### Custom enchantments
Plugins that add custom enchantments by registering them to ItemStacks through setting them via the [EnchantmentStorageMeta](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/inventory/meta/EnchantmentStorageMeta.html) will work, but note that the plugin _must_ be lower on the load order than HuskSync; in other words, HuskSync should be on the plugin's `loadbefore:`. This is because Spigot's item serialization API requires that the plugin that registered the enchantment be online to serialize it due to how it reads from the enchantment registry and so if the plugin does not load before (and thus does not shut down after) HuskSync, it won't be able to serialize the custom enchantments in the event of a server shutdown with players online.

### Map syncing
Map items are a special case, as their data is not stored in the item itself, but rather in the game world files. In addition to this, their data is dynamic and changes based on the updating of the world, something which can't be tracked across multiple instances. As a result, it's not possible to sync unlocked map items. 

However, experimental support for synchronising locked map items&mdash;that is, maps that have been locked in a cartography table&mdash;is currently available in development builds. This works by serializing its' map canvas pixel grid to the map item's persistent data container.

### Economy syncing
Although it's a common request, HuskSync doesn't synchronise economy data for a number of reasons!

I strongly reccommend making use of economy plugins that have cross-server economy balance synchronisation built-in, of which there are a multitude of options available. Please see our [[FAQs]] section for more details on this decision.

## Toggling Sync Features
All synchronisation features, except location and locked map synchronising, are enabled by default. To toggle a feature, navigate to the `features:` section in the `synchronisation:` part of your `config.yml` file, and change the option to `true`/`false` respectively.

<details>
  <summary>Example in config.yml</summary>
  
  ```yaml
  synchronization:
    # ...
    features:
      inventories: true
      ender_chests: true
      health: true
      max_health: true
      hunger: true
      experience: true
      potion_effects: true
      advancements: true
      game_mode: true
      statistics: true
      persistent_data_container: false
      locked_maps: true
      location: false
    #...
  ```

</details>
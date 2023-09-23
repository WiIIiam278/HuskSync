This page contains a list of the features HuskSync is and isn't able to syncrhonise on your server.

You can customise how much data HuskSync saves about a player by [turning each synchronization feature on or off](#toggling-sync-features). When a synchronization feature is turned off, HuskSync won't touch that part of a player's profile; in other words, the data they will inherit when changing servers will be read from their player data file on the local server.

## Feature table
✅&mdash;Supported&nbsp; ❌&mdash;Unsupported&nbsp; ⚠️&mdash;Experimental

| Name                      | Description                                                 | Availability |
|---------------------------|-------------------------------------------------------------|:------------:|
| Inventories               | Items in player inventories & selected hotbar slot          |      ✅       |
| Ender chests              | Items in ender chests&midast;                               |      ✅       |
| Health                    | Player health points                                        |      ✅       |
| Max health                | Player max health points and health scale                   |      ✅       |
| Hunger                    | Player hunger, saturation & exhaustion                      |      ✅       |
| Experience                | Player level, experience points & score                     |      ✅       |
| Potion effects            | Active status effects on players                            |      ✅       |
| Advancements              | Player advancements, recipes & progress                     |      ✅       |
| Game modes                | Player's current game mode                                  |      ✅       |
| Statistics                | Player's in-game stats (ESC -> Statistics)                  |      ✅       |
| Location                  | Player's current coordinate positon and world&dagger;       |      ✅       |
| Persistent Data Container | Custom plugin persistent data key map                       |      ✅️      |
| Locked maps               | Maps/treasure maps locked in a cartography table            |      ⚠️      |
| Unlocked maps             | Regular, unlocked maps/treasure maps ([why?](#map-syncing)) |      ❌       |
| Economy balances          | Vault economy balance. ([why?](#economy-syncing))           |      ❌       |

What about modded items? Or custom item plugins such as MMOItems or SlimeFun? These items are **not compatible**&mdash;check the [[FAQs]] for more information.

&midast;Purpur's custom ender chest resizing feature is also supported.

&dagger;This is intended for servers that have mirrored worlds across instances (such as RPG servers). With this option enabled, players will be placed at the same coordinates when changing servers.

### Map syncing
Map items are a special case, as their data is not stored in the item itself, but rather in the game world files. In addition to this, their data is dynamic and changes based on the updating of the world, something that can't be tracked across multiple instances. As a result, it's not possible to sync unlocked map items. Locked maps, however, are supported. This works by saving the pixel canvas grid to the map NBT itself, and generating virtual maps on the other servers.

### Economy syncing
Although it's a common request, HuskSync doesn't synchronize economy data for a number of reasons!

I strongly recommend making use of economy plugins that have cross-server economy balance synchronization built-in, of which there are a multitude of options available. Please see our [[FAQs]] section for more details on this decision.

## Toggling Sync Features
All synchronization features, except location and locked map synchronising, are enabled by default. To toggle a feature, navigate to the `features:` section in the `synchronization:` part of your `config.yml` file, and change the option to `true`/`false` respectively.

<details>
<summary>Example in config.yml</summary>
  
```yaml
synchronization:
  # ...
  features:
    health: true
    statistics: true
    location: false
    potion_effects: true
    ender_chest: true
    experience: true
    advancements: true
    game_mode: true
    inventory: true
    persistent_data: true
    hunger: true
  #...
```

</details>
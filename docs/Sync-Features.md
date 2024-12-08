This page contains a list of the features HuskSync is and isn't able to synchronise on your server.

You can customise how much data HuskSync saves about a player by [turning each synchronization feature on or off](#toggling-sync-features). When a synchronization feature is turned off, HuskSync won't touch that part of a player's profile; in other words, the data they will inherit when changing servers will be read from their player data file on the local server.

## Feature table
✅&mdash;Supported&nbsp; ❌&mdash;Unsupported&nbsp; ⚠️&mdash;Experimental

| Name                      | Description                                                                                 | Availability |
|---------------------------|---------------------------------------------------------------------------------------------|:------------:|
| Inventories               | Items in player inventories & selected hotbar slot                                          |      ✅       |
| Ender chests              | Items in ender chests                                                                       |      ✅       |
| Health                    | Player health points and scale                                                              |      ✅       |
| Hunger                    | Player hunger, saturation & exhaustion                                                      |      ✅       |
| Attributes                | Player max health, movement speed, reach, etc. ([wiki](https://minecraft.wiki/w/Attribute)) |      ✅       |
| Experience                | Player level, experience points & score                                                     |      ✅       |
| Potion effects            | Active status effects on players                                                            |      ✅       |
| Advancements              | Player advancements, recipes & progress                                                     |      ✅       |
| Game modes                | Player's current game mode                                                                  |      ✅       |
| Flight status             | If the player is currently flying / can fly                                                 |      ✅       |
| Statistics                | Player's in-game stats (ESC -> Statistics)                                                  |      ✅       |
| Location                  | Player's current coordinate position and world (see below)                                  |      ✅       |
| Persistent Data Container | Custom plugin persistent data key map                                                       |      ✅️      |
| Locked maps               | Maps/treasure maps locked in a cartography table                                            |      ✅       |
| Unlocked maps             | Regular, unlocked maps/treasure maps ([why?](#map-syncing))                                 |      ❌       |
| Economy balances          | Vault economy balance. ([why?](#economy-syncing))                                           |      ❌       |

* What about modded items (Arclight, etc.)? &ndash; Though we can't provide support for these setups to work, they have been reported to save & sync correctly with HuskSync v3.x+.
* What about SlimeFun, MMOItems, etc.? &ndash; Yes, items created via these plugins should save & sync correctly, but be sure to test thoroughly first. 
* What about Purpur's custom ender chest resizing feature? &ndash; Yes, this is supported (but make sure it's enabled on _all_ servers!).
* What do you mean by location syncing? &ndash; This is intended for servers that have mirrored worlds across instances (such as RPG servers). With this enabled, players will be placed at the same coordinates when changing servers.

### Map syncing
Map items are a special case, as their data is not stored in the item itself, but rather in the game world files. In addition to this, their data is dynamic and changes based on the updating of the world, something that can't be tracked across multiple instances. As a result, it's not possible to sync unlocked map items. Locked maps, however, are supported. This works by saving the pixel canvas grid to the map NBT itself, and generating virtual maps on the other servers.

### Economy syncing
Although it's a common request, HuskSync doesn't synchronize economy data for a number of reasons!

I strongly recommend making use of economy plugins that have cross-server economy balance synchronization built-in, of which there are a multitude of options available. Please see our [[FAQs]] section for more details on this decision.

## Toggling Sync Features
All synchronization features, except location and locked map synchronizing, are enabled by default. To toggle a feature, navigate to the `features:` section in the `synchronization:` part of your `config.yml` file, and change the option to `true`/`false` respectively.

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
HuskSync supports the following versions of Minecraft. Since v3.7, you must download the correct version of HuskSync for your server:

|    Minecraft    | Latest HuskSync | Java Version | Platforms     | Support Status               |
|:---------------:|:---------------:|:------------:|:--------------|:-----------------------------|
|     1.21.4      |    _latest_     |      21      | Paper, Fabric | ✅ **Active Release**         |
|     1.21.3      |      3.7.1      |      21      | Paper, Fabric | 🗃️ Archived (December 2024) |
|     1.21.1      |    _latest_     |      21      | Paper, Fabric | ✅ **November 2025** (LTS)    |
|     1.20.6      |      3.6.8      |      17      | Paper         | 🗃️ Archived (October 2024)  |
|     1.20.4      |      3.6.8      |      17      | Paper         | 🗃️ Archived (July 2024)     |
|     1.20.1      |    _latest_     |      17      | Paper, Fabric | ✅ **November 2025** (LTS)    |
| 1.17.1 - 1.19.4 |      3.6.8      |      17      | Paper         | 🗃️ Archived                 |
|     1.16.5      |      3.2.1      |      16      | Paper         | 🗃️ Archived                 |

HuskSync is primarily developed against the latest release. Old Minecraft versions are allocated a support channel based on popularity, mod support, etc:

* Long Term Support (LTS) &ndash; Supported for up to 12-18 months
* Non-Long Term Support (Non-LTS) &ndash; Supported for 3-6 months

## Incompatible versions
This plugin does not support the following software-Minecraft version combinations. The plugin will fail to load if you attempt to run it with these versions. Apologies for the inconvenience.

| Minecraft         | Server Software                           | Notes                                  |
|-------------------|-------------------------------------------|----------------------------------------|
| 1.19.4            | Only: `Purpur, Pufferfish`&dagger;        | Older Paper builds also not supported. |
| 1.19.3            | Only: `Paper, Purpur, Pufferfish`&dagger; | Upgrade to 1.19.4 or use Spigot        |
| 1.16.5            | _All_                                     | Please use v3.3.1 or lower             |
| below 1.16.5      | _All_                                     | Upgrade to Minecraft 1.16.5            |

&dagger;Further downstream forks of this server software are also affected.

## Incompatible plugins / mods
Please note the following plugins / mods can cause issues with HuskSync:

* Restart plugins / mods are not supported. These will cause [player data to not save correctly when your server restarts](troubleshooting#issues-with-player-data-going-out-of-sync-during-a-server-restart) due to the way these plugins utilise bash scripts. It's important to understand that restart plugins don't actually restart yur server, they just trigger some (often unstable) process-killing scripting logic to occur!
* Combat logging plugins / mods are not supported. Some have built-in support for HuskSync and should work as expected, but for others you may wish to modify the [[Event Priorities]]
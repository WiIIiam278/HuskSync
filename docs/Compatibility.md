HuskSync supports the following versions of Minecraft. Since v3.7, you must download the correct version of HuskSync for your server:

|    Minecraft    | Latest HuskSync | Java Version | Platforms     | Support Ends                  |
|:---------------:|:---------------:|:------------:|:--------------|:------------------------------|
|     1.21.3      |    _latest_     |      21      | Paper, Fabric | ✅ **Active Release**          |
|     1.21.1      |    _latest_     |      21      | Paper, Fabric | ✅ **December 2024** (Non-LTS) |
|     1.20.6      |      3.6.8      |      17      | Paper         | ❌ _October 2024_              |
|     1.20.4      |      3.6.8      |      17      | Paper         | ❌ _July 2024_                 |
|     1.20.1      |    _latest_     |      17      | Paper, Fabric | ✅ **November 2025** (LTS)     |
| 1.17.1 - 1.19.4 |      3.6.8      |      17      | Paper         | ❌ _Support ended_             |
|     1.16.5      |      3.2.1      |      16      | Paper         | ❌ _Support ended_             |

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
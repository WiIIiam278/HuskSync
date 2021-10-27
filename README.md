[![HuskSync Banner](images/banner-graphic.png)](https://github.com/WiIIiam278/HuskSync)
# HuskSync
[![Maven CI](https://github.com/WiIIiam278/HuskSync/actions/workflows/gradle.yml/badge.svg)](https://github.com/WiIIiam278/HuskSync/actions/workflows/gradle.yml)
[![Discord](https://img.shields.io/discord/818135932103557162?color=7289da&logo=discord)](https://discord.gg/tVYhJfyDWG)

**HuskSync** is a modern, cross-server player data synchronisation system that allows player data (inventories, health, hunger & status effects) to be synchronised across servers through the use of **Redis**. 

## Disclaimer
This source code is provided as reference to licensed individuals that have purchased the HuskSync plugin once from any of the official sources it is provided. The availability of this code does not grant you the rights to re-distribute, compile or share this source code outside this intended purpose. 

Are you a developer? [Read below for information about code bounty licensing](#Contributing).

## Setup
### Requirements
* A BungeeCord-based proxy server
* A Spigot-based game server
* A Redis server

### Installation
1. Install HuskSync in the `/plugins/` folder of both your Spigot and Proxy servers.
2. Start your servers, then stop them again to allow the configuration files to generate.  
3. Navigate to the generated `config.yml` files on your Spigot server and Proxy (located in `/plugins/HuskSync/`) and fill in the credentials of your redis server. 
   1. On the Proxy server, you can additionally configure a MySQL database to save player data in, as by default the plugin will create a SQLite database. 
   3. By default, everything except player locations are synchronised. If you would like to change what gets synchronised, you can do this by editing the `config.yml` files of each Spigot server.
4. Once you have finished setting everything up, make sure to restart all of your servers and proxy server. Then, log in and data should be synchronised!

### Migration from MySQLPlayerDataBridge
HuskSync supports the migration of player data from [MySQLPlayerDataBridge](https://www.spigotmc.org/resources/mysql-player-data-bridge.8117/). Please note that HuskSync is not compatible with MySQLPlayerInventoryBridge, as that has a different system for data handling.

To migrate from MySQLPLayerDataBridge, you need a Proxy server with HuskSync installed and one Spigot server with both HuskSync and MySQLPlayerDataBridge installed. To migrate:
1. Make sure HuskSync is set up correctly on the Proxy and Spigot server, making sure that the two are able to communicate with Redis (it will display a handshake confirmation message in both consoles when communications have been established)
2. Make sure your database is configured correctly on your Proxy server. For example, if you would like to change from SQLite to MySQL, you should do this now because the data from MySQLPlayerDataBridge will be moved into it.
3. Make sure no players are online, then in the Proxy server's console run `husksync migrate`
4. Follow the steps in the Migration wizard to ensure the connection credentials and details of the database containing your MySQLPlayerDataBridge are correct, changing settings with `husksync migrate setting <setting> <new value>` as necessary.
5. Run `husksync migrate start` in the Proxy server's console to start the migration. This could take some time, depending on the amount of data that needs migrating and the speed of your database/server. When the migration is complete, it will display a "Migration complete" message. 

### Troubleshooting
#### Commands do not function
Please check that the plugin is installed and enabled on both the proxy and bukkit server you are trying to execute the command from and that both plugins connected to Redis. (A connection handshake confirmation message is logged to console when communications are successfully established.)

#### SQL errors in proxy console / data not synchronising
This issue frequently occurs in users running Cracked (illegal) servers. I do not support piracy and so will be limited in my ability to help you.
If you are running an offline server for a legitimate reason, however, make sure that in the `paper.yml` of your Bukkit servers `bungee-online-mode` is set to the correct value - and that both your Proxy (BungeeCord, Waterfall, etc.) server and Bukkit (Spigot, paper, etc.) servers are set up correctly to work with offline mode.

## How it works
![Flow chart showing different processes of how the plugin works](images/flow-chart.png)
HuskSync saves a player's data when they log out to a cache on your proxy server, and redistributes that data to players when they join another HuskSync-enabled server. Player data in the cache is then saved to a database (be it SQLite or MySQL) and this is loaded from when a player joins your network.

To facilitate the transfer of data between servers, HuskSync serializes player data and then makes use of Redis to communicate between the Proxy and Spigot servers.

### What is synchronised
Everything except player locations are synchronised by default. You can enable or disable what data is loaded on a server by modifying these values in the `/plugins/HuskSync/config.yml` file on each Spigot server.
* Player inventory
   * Player armour and off-hand
   * Player currently selected hotbar slot
* Player ender chest
* Player experience points & levels
* Player health
   * Player max health
   * Player health scale
* Player hunger
   * Player saturation
   * Player exhaustion
* Player game mode
* Player advancements
* Player statistics (ESC → Statistics menu)
* Player location
   * Player flight status 

### Commands
Commands are handled by the proxy server, rather than each spigot server. Some will only work on Spigot servers with HuskSync installed. Please remember that you will need a Proxy permission plugin (e.g. LuckPermsBungee) to set permissions for proxy commands.

Command | Description | Permission
------- | ----------- | ----------
`/husksync about`  | View plugin information | _None_
`/husksync update` | Check if an update is available | `husksync.command.admin`
`/husksync status` | View system status information | `husksync.command.admin`
`/husksync reload` | Reload config & message files | `husksync.command.admin`
`/husksync invsee` | View an offline player's inventory | `husksync.command.inventory`
`/husksync echest` | View an offline player's ender chest | `husksync.command.ender_chest`
`/husksync migrate`| Migrate data from MPDB | _Console-only_

### Frequently Asked Questions (FAQs)
#### Is Redis required?
Yes. Redis is both free, easy to install and multiplatform, though. Pterodactyl users can also run it in an egg with relatively low overheads.

#### What is Redis?
Redis is server software that acts as an in-memory data store. Minecraft server software typically makes use of its function to send messages efficiently.

#### Is Economy / Vault synchronization supported?
No.

Synchronising economy data like MySQLPlayerDataBridge does causes a number of issues and incompatibilities that mean that MySQLPlayerDataBridge has had to add integrations with a number of plugins just to make them work. This leads to poor compatibility and more bugs as plugins change their APIs and systems. In the case of HuskSync, this would require both plugin authors and myself to manually support each other, which would inevitably increase update times, lead to a bottomless pit of "add support for this plugin" requests and these integrations would then inevitably break when authors decide to update their plugins, requiring me to update manually.

I strongly recommend making use of economy plugins that provide built-in support for cross-server synchronisation instead, which do not have the same issues. I have personally used [XConomy](https://www.spigotmc.org/resources/xconomy.75669/) in the past and reccommend it.

#### Will this work on servers running multiple proxies?
Short answer: Not right now, but improved support for this is planned in the future.

Long answer: This is a difficult question to unpack because of the wide variety of setups that involve multiple proxies, however currently the architecture of how messages are sent between servers assumes that one proxy will serve multiple Bukkit servers, so having multiple proxies will lead to data going out of sync, among other issues.

#### Does it work with Velocity?
I'd like to add support for Velocity in the future, but right now it is not supported.

#### Is this faster than MySqlPlayerDataBridge (MPDB)?
It's difficult to say, and will depend on your server. 

MPDB stores data in a MySQL database (hence the name) and operates by querying a database for said data when a player joins a Bukkit server. 
HuskSync stores player data in a central cache on the Proxy server and servers request data from said cache; data is only queried from the database when a player joins the network, not when switching servers within it.

HuskSync should operate faster in theory, then, as it does not need to query large amounts of data from a database file as often. However, any performance enhancements you might see will heavily depend on the speed of your existing database and your server hardware.

#### Are modded items supported?
Most likely not - and I cannot support it - but feel free to test it, as depending on the implementation of your modding API it may work just fine.

## Developers
### API
HuskSync currently has a few API events (located in the api module) which developers can use to detect when player data synchronisation has completed, or to update Player Data:
* **SyncCompleteEvent** - Fires when a player's data has finished synchronising. Use #getData to get the PlayerData being set.
* **SyncEvent** - Fires just before a player's data is synchronised. Can be cancelled. Use #getData to get the PlayerData being set, and #setData to set it.

### Contributing
A code bounty program is in place for HuskSync, where developers making significant code contributions to HuskSync may be entitled to a discretionary license to use HuskSync in commercial contexts without having to purchase the resource, so please feel free to submit pull requests with improvements, fixes and features!

### Translation
While the code bounty program is not available for translation contributors, they are still strongly appreciated in making the plugin more accessible. If you'd like to contribute translated message strings for your language, you can submit a Pull Request that creates a .yml file in `bungeecord/src/main/resources/languages` with the correct translations.

### Building
To build HuskSync you will first need to download MySqlPlayerDataBridge and `mvn install:install-file` the jar file to your local maven repository.
```
mvn install:install-file -Dfile=MysqlPlayerDataBridge-v3.36.3.jar -DgroupId=net.craftersland.data -DartifactId=bridge -Dversion=3.36.3 -Dpackaging=jar
```

Then, to build the plugin, run the following in the root of the repository:
```
./gradlew clean build
```

## bStats
This plugin uses bStats to provide me with metrics about its usage:
* [View Bukkit metrics](https://bstats.org/plugin/bukkit/HuskSync%20-%20Bukkit/13140)
* [View BungeeCord metrics](https://bstats.org/plugin/bungeecord/HuskSync%20-%20BungeeCord/13141)

You can turn metric collection off by navigating to `plugins/bStats/config.yml` and editing the config to disable plugin metrics.

## Support
* Report bugs: [Click here](https://github.com/WiIIiam278/HuskSync/issues)
* Discord support: Join the [HuskHelp Discord](https://discord.gg/tVYhJfyDWG)!
    * Proof of purchase is required for support.

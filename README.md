[![HuskSync Banner](images/banner-graphic.png)](https://github.com/WiIIiam278/HuskSync)
# HuskSync
[![Discord](https://img.shields.io/discord/818135932103557162?color=7289da&logo=discord)](https://discord.gg/tVYhJfyDWG)

**HuskSync** is a modern, cross-server player data synchronisation system that allows player data (inventories, health, hunger & status effects) to be synchronised across servers through the use of **Redis**. 

## Disclaimer
This source code is provided as reference to licensed individuals that have purchased the HuskSync plugin once from any of the official sources it is provided. The availability of this code does not grant you the rights to re-distribute, compile or share this source code outside this intended purpose. 

Are you a developer? [Read below for information about code bounty licensing](#Contributing).

## Setup
### Requirements
* A BungeeCord or Velocity-based proxy server
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

#### Data not being synced on player join and SQL errors in proxy console
This issue frequently occurs in users running Cracked (illegal) servers. I do not support piracy and so will be limited in my ability to help you.
If you are running an offline server for a legitimate reason, however, make sure that in the `paper.yml` of your Bukkit servers `bungee-online-mode` is set to the correct value - and that both your Proxy (BungeeCord, Waterfall, etc.) server and Bukkit (Spigot, paper, etc.) servers are set up correctly to work with offline mode.

#### Data sometimes not syncing between servers
There are two primary reasons this may happen:
* On your proxy server, you are running _FlameCord_ or a similar fork of Waterfall. Due to the nature of these forks changing security parameters, they can block or interfere with Redis packets being sent to and from your server. FlameCord, XCord and other forks are not compatible with HuskSync. For security-conscious users, I recommend Velocity.
* Your backend servers/proxy and Redis server have noticeably different amounts of latency between each other. This is particularly relevant for users running across multiple machines, where some backend servers / the proxy are installed with Redis and other backend servers are on a different machine. The solution to this is to have your BungeeCord and Redis alone on one machine, and your backend servers across the others - or have a separate machine with equal latency to the others that has Redis on. In the future, I may have a look at automatically correcting and accounting for differences in latency.

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

| Command                               | Description                          | Permission                     |
|---------------------------------------|--------------------------------------|--------------------------------|
| `/husksync about`                     | View plugin information              | _None_                         |
| `/husksync update`                    | Check if an update is available      | `husksync.command.admin`       |
| `/husksync status`                    | View system status information       | `husksync.command.admin`       |
| `/husksync reload`                    | Reload config & message files        | `husksync.command.admin`       |
| `/husksync invsee <player> [cluster]` | View an offline player's inventory   | `husksync.command.inventory`   |
| `/husksync echest <player> [cluster]` | View an offline player's ender chest | `husksync.command.ender_chest` |
| `/husksync migrate [args]           ` | Migrate data from MPDB               | _Console-only_                 |

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
Yes! Servers running the Velocity proxy software are supported as of HuskSync 1.2+.

#### Is this faster than MySqlPlayerDataBridge (MPDB)?
It's difficult to say, and will depend on your server. 

MPDB stores data in a MySQL database (hence the name) and operates by querying a database for said data when a player joins a Bukkit server. 
HuskSync stores player data in a central cache on the Proxy server and servers request data from said cache; data is only queried from the database when a player joins the network, not when switching servers within it.

HuskSync should operate faster in theory, then, as it does not need to query large amounts of data from a database file as often. However, any performance enhancements you might see will heavily depend on the speed of your existing database and your server hardware.

#### Are modded items supported?
Most likely not - and I cannot support it - but feel free to test it, as depending on the implementation of your modding API it may work just fine.

## Developers
### API
HuskSync has an API for Bukkit providing events that fire when synchronisation takes place as well as a method to access and deserialize player data on demand. There is no API for the proxy side currently.

HuskSync's API is available on [JitPack](https://jitpack.io/#WiIIiam278/HuskSync/Tag). You can view the [HuskSync JavaDocs here](https://javadoc.jitpack.io/com/github/WiIIiam278/HuskSync/latest/javadoc/index.html). You should only use stuff in the `husksync.bukkit.api` and `husksync.bukkit.data` packages (as well as the PlayerData class located in the `husksync` root package.

#### Including the API in your project
With Maven, add the repository to your pom.xml:
```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
Then, add the dependency. Replace `version` with the latest version of HuskSync: [![](https://jitpack.io/v/WiIIiam278/HuskSync.svg)](https://jitpack.io/#WiIIiam278/HuskSync)
```xml
	<dependency>
            <groupId>com.github.WiIIiam278</groupId>
            <artifactId>HuskSync</artifactId>
            <version>version</version>
            <scope>provided</scope>
	</dependency>
```

Or, with Gradle, add the dependency like so to your build.gradle:
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Then add the dependency as follows. Replace `version` with the latest version of HuskSync: [![](https://jitpack.io/v/WiIIiam278/HuskSync.svg)](https://jitpack.io/#WiIIiam278/HuskSync)
```
        dependencies {
	        compileOnly 'com.github.WiIIiam278:HuskSync:version'
	}
```

#### API Events
* **SyncCompleteEvent** - Fires when a player's data has finished synchronising. Use #getData to get the PlayerData being set.
* **SyncEvent** - Fires just before a player's data is synchronised. Can be cancelled. Use #getData to get the PlayerData being set, and #setData to set it.

#### Fetching player data on demand
To fetch PlayerData from a UUID as you need it, create an instance of the HuskSyncAPI class and use the `#getPlayerData` method. Note that data returned in this method is only the data from the central cache. That is to say, if the player is online, the data returned in this way will not necessarily be the same as the player's actual current data.
```java
HuskSyncAPI huskSyncApi = HuskSyncAPI.getInstance();
try {
    CompletableFuture<PlayerData> playerDataCompletableFuture = huskSyncApi.getPlayerData(playerUUID);
    // thenAccept blocks the thread until HuskSync has grabbed the data, so you may wish to run this asynchronously (e.g. Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {});.
    playerDataCompletableFuture.thenAccept(playerData -> {
        // You now have a PlayerData object which you can get serialized data from and deserialize with the DataSerializer static methods 
    });
} catch (IOException e) {
    Bukkit.getLogger().severe("An error occurred fetching player data!");
}
```

#### Getting ItemStacks and usable data from PlayerData
Use the static methods provided in the [DataSerializer class](https://javadoc.jitpack.io/com/github/WiIIiam278/HuskSync/latest/javadoc/me/william278/husksync/bukkit/data/DataSerializer.html). For instance, to get a player's inventory as an `ItemStack[]` from a `PlayerData` object.
```java
ItemStack[] inventoryItems = DataSerializer.serializeInventory(playerData.getSerializedInventory());
ItemStack[] enderChestItems = DataSerializer.serializeInventory(playerData.getSerializedEnderChest());
```

#### Updating PlayerData
You can then update PlayerData back to the central cache using the `HuskSyncAPI#updatePlayerData(playerData)` method. For example:
```java
// Update a value in the player data object
playerData.setHealth(20);
try {
    // Update the player data to the cache
    huskSyncApi.updatePlayerData(playerData);
} catch (IOException e) {
    Bukkit.getLogger().severe("An error occurred updating player data!");
}
```


### Contributing
A code bounty program is in place for HuskSync, where developers making significant code contributions to HuskSync may be entitled to a discretionary license to use HuskSync in commercial contexts without having to purchase the resource, so please feel free to submit pull requests with improvements, fixes and features!

### Translation
While the code bounty program is not available for translation contributors, they are still strongly appreciated in making the plugin more accessible. If you'd like to contribute translated message strings for your language, you can submit a Pull Request that creates a .yml file in `bungeecord/src/main/resources/languages` with the correct translations.

### Building
You can build HuskSync yourself, though please read the license and buy yourself a copy as HuskSync is indeed a premium resource. 

To build HuskSync, you'll need to get the [MPDBConverter](https://github.com/WiIIiam278/MPDBDataConverter) library, either by authenticating through GitHub packages or by downloading and running `mvn install-file` to publish it to your local maven repository.
```
mvn install:install-file -Dfile=MysqlPlayerDataBridge-v4.0.1.jar -DgroupId=net.craftersland.data -DartifactId=bridge -Dversion=4.0.1 -Dpackaging=jar
```

Then, to build the plugin, run the following in the root of the repository:
```
./gradlew clean build
```

## bStats
This plugin uses bStats to provide me with metrics about its usage:
* [View Bukkit metrics](https://bstats.org/plugin/bukkit/HuskSync%20-%20Bukkit/13140)
* [View BungeeCord metrics](https://bstats.org/plugin/bungeecord/HuskSync%20-%20BungeeCord/13141)
* [View Velocity metrics](https://bstats.org/plugin/velocity/HuskSync%20-%20Velocity/13489)

You can turn metric collection off by navigating to `plugins/bStats/config.yml` and editing the config to disable plugin metrics.

## Support
* Report bugs: [Click here](https://github.com/WiIIiam278/HuskSync/issues)
* Discord support: Join the [HuskHelp Discord](https://discord.gg/tVYhJfyDWG)!
    * Proof of purchase is required for support.

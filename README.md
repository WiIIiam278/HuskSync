# CrossServerSync
**CrossServerSync** is a robust solution for synchronising player data (inventories, health, hunger & status effects) between servers. It was designed as a lightweight alternative to MySQLPlayerDataBridge, 

### Installation
Install CrossServerSync in the `/plugins/` folder of your Spigot (and derivatives) servers and Proxy (BungeeCord and derivatives) server.
Start your servers, then stop them again to allow the configuration files to generate.

Navigate to the generated config.yml files on your Spigot server and Proxy (located in `/plugins/CrossServerSync/`) and fill in the credentials of your redis server. On the Proxy server, you can additionally configure a MySQL database to save player data in, as by default the plugin will create a SQLite database for this.

If you have multiple proxy servers (i.e. via RedisBungee), you need to install the plugin on all of them and make use of the MySQL option and ensure the proxies are using the same database.

### How it works
![Flow chart showing different processes of how the plugin works](images/flow-chart.png)
CrossServerSync synchronises player data between servers using Redis to transfer cached data, loaded from a central database as necessary.

### Building
To build CrossServerSync, run the following in the root of the repository:
```
./gradlew clean build
```
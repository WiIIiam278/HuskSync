> **Warning:** Fabric support is currently in beta and is not production ready yet.

This will walk you through installing HuskSync on your network of Spigot or Fabric servers. Please check your server's [[Compatibility]] and download the correct version of HuskSync for your server.


## Requirements
> **Warning:** Mixing and matching Fabric/Spigot servers is not supported, and all servers must be running the same Minecraft version.

> **Note:** Please also note some specific legacy Paper/Purpur versions are [not compatible](Compatibility) with HuskSync.

* A MySQL Database (v8.0+)
  * **OR** a MariaDB, PostrgreSQL or MongoDB database, which are also supported
* A Redis Database (v5.0+) &mdash; see [[FAQs]] for more details.
* Any number of Spigot servers, connected by a BungeeCord or Velocity-based proxy (see [[Compatibility]])
  * **OR** a network of Fabric servers, connected by a Velocity-based proxy

## Setup Instructions
### 1. Install the jar
- Place the plugin jar file in the `/plugins/` or `/mods/` directory of each Spigot/Fabric server respectively.
- You do not need to install HuskSync as a proxy plugin.
- _Spigot users_: You can additionally install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) or [PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/) for better locked user handling.
- _Fabric users_: Ensure the latest Fabric API mod jar is installed! 

### 2. Restart servers
- Start, then stop every server to let HuskSync generate the [[config file]].
- HuskSync will throw an error in the console and disable itself as it is unable to connect to the database. You haven't set the credentials yet, so this is expected.

### 3. Enter Mysql & Redis database credentials
- Navigate to the new config file on each server (`~/plugins/HuskSync/config.yml` on Spigot, `~/config/husksync/config.yml` on Fabric)
- Under `credentials` in the `database` section, enter the credentials of your (MySQL/MariaDB/MongoDB/PostgreSQL) Database. You shouldn't touch the `connection_pool` properties.
- Under `credentials` in the `redis` section, enter the credentials of your Redis Database. If your Redis server doesn't have a password, leave the password blank as it is.
- Unless you want to have multiple clusters of servers within your network, each with separate user data, you should not change the value of `cluster_id`.

<details>
<summary>MongoDB users &mdash; additional instructions</summary>

- Navigate to the HuskSync config file on each server (`~/plugins/HuskSync/config.yml`)
- Set `type` in the `database` section to `MONGO`
- Under `credentials` in the `database` section, enter the credentials of your MongoDB Database. You shouldn't touch the `connection_pool` properties.
- Under `parameters` in the `mongo_settings` section, ensure the specified `&authSource=` matches the database you are using (default is `HuskSync`).

#### Additional setup for MongoDB Atlas

- Set `using_atlas` in the `mongo_settings` section to `true`. 
- Remove `&authSource=HuskSync` from `parameters` in the `mongo_settings`. 

(The `port` setting in `credentials` is disregarded when using Atlas.)
</details>

<details>
<summary>Pterodactyl self-hosts &mdash; Redis setup instructions</summary>

If you are hosting your Redis server on the same node as your servers, you need to use `172.18.0.1` as your host (or equivalent if you changed your network settings), and bind it in the Redis config `nano /etc/redis/redis.conf`.

You will also need to uncomment the `requirepass` directive and set a password to allow outside connections, or disable `protected-mode`. Once a password is set and Redis is restarted `systemctl restart redis`, you will also need to update the password in your pterodactyl `.env` (`nano /var/www/pterodactyl/.env`) and refresh the cache `cd /var/www/pterodactyl && php artisan config:clear`.

You may also need to allow connections from your firewall depending on your distribution.
</details>

### 4. Set server names in server.yml files
- Navigate to the server name file on each server (`~/plugins/HuskSync/server.yml` on Spigot, `~/config/husksync/server.yml` on Fabric)
- Set the `name:` of the server in this file to the ID of this server as defined in the config of your proxy (e.g., if this is the "hub" server you access with `/server hub`, put `'hub'` here)

### 5. Start every server again
- Provided your MySQL and Redis credentials were correct, synchronization should begin as soon as you start your servers again.
- If you need to import data from HuskSync v1.x or MySQLPlayerDataBridge, please see the guides below:
  - [[Legacy Migration]]
  - [[MPDB Migration]]
This will walk you through installing HuskSync on your network of Spigot or Fabric servers.

## Requirements
> **Warning:** Mixing and matching Fabric/Spigot servers is not supported, and all servers must be running the same Minecraft version.

> **Note:** Please also note some specific legacy Paper/Purpur versions are [not compatible](Unsupported-Versions) with HuskSync.

* A MySQL Database (v8.0+)
  * **OR** a MariaDB, PostrgreSQL or MongoDB database, which are also supported
* A Redis Database (v5.0+) &mdash; see [[FAQs]] for more details.
* Any number of Spigot servers, connected by a BungeeCord or Velocity-based proxy (Minecraft v1.17.1+, running Java 17+)
  * **OR** a network of Fabric servers, connected by a Fabric proxy (Minecraft v1.20.1, running Java 17+)

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
<summary>Important &mdash; MongoDB Users</summary>

- Navigate to the HuskSync config file on each server (`~/plugins/HuskSync/config.yml`)
- Set `type` in the `database` section to `MONGO`
- Under `credentials` in the `database` section, enter the credentials of your MongoDB Database. You shouldn't touch the `connection_pool` properties.
<details>

<summary>Additional configuration for MongoDB Atlas users</summary>

- Navigate to the HuskSync config file on each server (`~/plugins/HuskSync/config.yml`)
- Set `using_atlas` in the `mongo_settings` section to `true`. 
- Remove `&authSource=HuskSync` from `parameters` in the `mongo_settings`. 

(The `port` setting in `credentials` is disregarded when using Atlas.)
</details>

</details>

### 4. Set server names in server.yml files
- Navigate to the server name file on each server (`~/plugins/HuskSync/server.yml` on Spigot, `~/config/husksync/server.yml` on Fabric)
- Set the `name:` of the server in this file to the ID of this server as defined in the config of your proxy (e.g., if this is the "hub" server you access with `/server hub`, put `'hub'` here)

### 5. Start every server again
- Provided your MySQL and Redis credentials were correct, synchronization should begin as soon as you start your servers again.
- If you need to import data from HuskSync v1.x or MySQLPlayerDataBridge, please see the guides below:
  - [[Legacy Migration]]
  - [[MPDB Migration]]
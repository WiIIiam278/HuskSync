This will walk you through installing HuskSync on your network of Spigot servers.

## Requirements
* A MySQL Database (v8.0+)
* A Redis Database (v5.0+) &mdash; see [[FAQs]] for more details.
* Any number of Spigot servers, connected by a BungeeCord or Velocity-based proxy (Minecraft v1.16.5+, running Java 16+)

## Setup Instructions
### 1. Install the jar
- Place the plugin jar file in the `/plugins/` directory of each Spigot server.
- You do not need to install HuskSync as a proxy plugin.
### 2. Restart servers
- Start, then stop every server to let HuskSync generate the [[config file]].
- HuskSync will throw an error in console and disable itself as it is unable to connect to the database. You haven't set the credentials yet, so this is expected.
- Advanced users: If you'd prefer, you can just create one config.yml file and create symbolic links in each `/plugins/HuskSync/` folder to it to make updating it easier.
### 3. Enter MySQL & Redis database credentails
- Navigate to the HuskSync config file on each server (`~/plugins/HuskSync/config.yml`)
- Under `credentials` in the `database` section, enter the credentials of your MySQL Database. You shouldn't touch the `connection_pool` properties.
- Under `credentials` in the `redis` section, enter the credentails of your Redis Database. If your Redis server doesn't have a password, leave the password blank as it is.
- Unless you want to have multiple clusters of servers within your network, each with separate user data, do not change the value of `cluster_id`.
### 4. Start every server again
- Provided your MySQL and Redis credentials were correct, synchronisation should begin as soon as you start your servers again.
- If you need to import data from HuskSync v1.x or MySQLPlayerDataBridge, please see the guides below:
  - [[Legacy Migration]]
  - [[MPDB Migration]]
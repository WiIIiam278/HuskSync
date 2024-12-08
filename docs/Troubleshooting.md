This page contains a number of common issues when using HuskSync and how you can troubleshoot and resolve them.

## Topics
### Duplicate UUIDs in database
This is most frequently caused by running a cracked "offline mode" network of servers. We [don't provide support](https://william278.net/terms) for problems caused by cracked servers and the most advice we can offer you is:
- Ensure `bungee_online_mode` is set to the correct value in the `paper.yml` config file on each of your Bukkit servers
- Ensure your authenticator plugin is passing valid, unique IDs to each backend Spigot/Fabric server.

### Cannot set data with newer Minecraft version than the server
This is caused when you attempt to downgrade user data from a newer version of Minecraft to an older one, or when your Spigot/Fabric servers are running mismatched Minecraft versions.

HuskSync will identify this and safely prevent the synchronization from occurring. Your Spigot/Fabric servers must be running the same version of both Minecraft and HuskSync.

### User data failing to synchronize
This can occur due to misaligned timings between your Spigot/Fabric servers and your Redis server. HuskSync has a built in way of tuning this. Try continously increasing the `network_latency_milliseconds` option in your config to a higher value.

### Synchronization issues with Keep Inventory enabled
On servers that use [[Keep Inventory]] (where players keep their items when they die), you can run into synchronization issues. See [[Keep Inventory]] for details on why this happens and how to resolve it.

### Exceptions when compressing data via Snappy (lightweight Linux distros)
Some lightweight Linux distros such as Alpine Linux (used on Pterodactyl) might not have the dependencies needed for the [Snappy](https://github.com/xerial/snappy-java) compressor. It's possible to disable data compression by changing `compress_data` to false in your config. Note that after changing this setting you will need to reset your database. Alternatively, find the right libraries for your distro!

### Redis connection problems on Pterodactyl / Pelican
If you are hosting your [[Redis]] server on the same node as your servers, you need to use 172.18.0.1 (or equivelant if you changed your network settings) as your host. You may also need to [allow connections from your firewall](https://pterodactyl.io/community/games/minecraft.html#firewalls) depending on your distribution. See our tips for running [Redis on a Pterodactyl or Pelican panel](Redis#pterodactyl--pelican-panel-hosts)

### Database connection problems on Pterodactyl / Pelican
If you have more than one [[Database]] server connected to your panel, you may need to set `useSSL=true` in the parameters.

### Issues with player data going out of sync during a server restart
This can happen due to the way in which your server restarts. If your server uses either:

* `/restart` (this is a weird Spigot/Fabric command that uses legacy bash scripting)
* ANY restart plugin, e.g. UltimateAutoRestart (these basically execute an API-called restart using the same legacy bash logic as per above)

These are **not compatible** with HuskSync in most cases due to the way in which this causes restart servers causing shutdown logic to process in strange and unpredictable orders, usually before HuskSync has had a chance to scan and perform its shutdown logic. To safely restart your server, please use:

* A Pterodactyl task to perform a Restart. This executes the Power Action program stopcode (and then execute the startup command when the container has terminated)
* A cronjob to send a stop command / Power Action program stopcode, listen for the service to fully terminate, and then execute your startup command
* For manual restarts, executing `/stop` and starting your server up with the startup command is totally fine.

It's not a great idea to use a plugin to handle restarts. Plugins are only able to operate when your server is turned on and must rely on scripts which don't safely shutdown servers when restarting.
This page contains a number of common issues and how you can troubleshoot and resolve them.

## Topics
### Duplicate UUIDs in database
This is most frequently caused by running a cracked "offline mode" network of servers. We [don't provide support](https://william278.net/terms) for problems caused by cracked servers and the most advice we can offer you is:
- Ensure `bungee_online_mode` is set to the correct value in the `paper.yml` config file on each of your Bukkit servers
- Ensure your authenticator plugin is passing valid, unique IDs to each backend Spigot server.

### Cannot set data with newer Minecraft version than the server
This is caused when you attempt to downgrade user data from a newer version of Minecraft to an older one, or when your Spigot servers are running mismatched Minecraft versions.

HuskSync will identify this and safely prevent the synchronization from occuring. Your Spigot servers must be running the same version of both Minecraft and HuskSync.

### User data failing to synchronize
This can occur due to misaligned timings between your Spigot servers and your Redis server. HuskSync has a built in way of tuning this. Try continously increasing the `network_latency_milliseconds` option in your config to a higher value.

### Synchronization issues with Keep Inventory enabled
On servers that use Keep Inventory move (where players keep their items when they die), you can run into synchronization issues. See [[Keep Inventory]] for details on why this happens and how to resolve it.

### Exceptions when compressing data via Snappy (lightweight Linux distros)
Some lightweight Linux distros such as Alpine Linux (used on Pterodactyl) might not have the dependencies needed for the [Snappy](https://github.com/xerial/snappy-java) compressor. It's possible to disable data compression by changing `compress_data` to false in your config. Note that after changing this setting you will need to reset your database. Alternatively, find the right libraries for your distro!

### Redis connection problems on Pterodactyl
If you are hosting your Redis server on the same node as your servers, you need to use 172.18.0.1 (or equivelant if you changed your network settings) as your host. You may also need to [allow connections from your firewall](https://pterodactyl.io/community/games/minecraft.html#firewalls) depending on your distribution.

### MySQL connection problems on Pterodactyl
If you have more than one MySQL server connected to your panel, you may need to set `useSSL=true` in the parameters.

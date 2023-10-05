This page addresses a number of frequently asked questions about the plugin.

## Frequently Asked Questions

<details>
<summary>&nbsp;<b>What data can be synchronized?</b></summary>

HuskSync supports synchronising a wide range of different data elements, each of which can be toggled to your liking. Please check out the [[Sync Features]] page for a full list.

</details>

<details>
<summary>&nbsp;<b>Are modded items supported?</b></summary>

Modded items are not supported.

</details>

<details>
<summary>&nbsp;<b>Are MMOItems / SlimeFun / ItemsAdder items supported?</b></summary>

These plugins, which provide custom items, should be supported as of HuskSync v3.x; but do note we cannot guarantee compatibility with all methods of injecting custom data to create custom items. Be sure to test thoroughly before deploying on production!

</details>

<details>
<summary>&nbsp;<b>Is Redis required? What is Redis?</b></summary>

HuskSync requires Redis to operate (for reasons demonstrated below). Redis is an in-memory database server used for caching data at scale and sending messages across a network. You have a Redis server in a similar fashion to the way you have a MySQL database server. If you're using a Minecraft hosting company, you'll want to contact their support and ask if they offer Redis. If you're looking for a host, I have a list of some popular hosts and whether they support Redis [available to read here.](https://william278.net/redis-hosts)

</details>

<details>
<summary>&nbsp;<b>How does the plugin synchronize data?</b></summary>

HuskSync makes use of both MySQL and Redis for optimal data synchronization. You have the option of using one of two [[Sync Modes]], which synchronize data between servers (`DELAY` or `LOCKSTEP`)

When a user changes servers, in addition to data being saved to MySQL, it is also cached via the Redis server with a temporary expiry key. When changing servers, the receiving server detects the key and sets the user data from Redis. When a player rejoins the network, the system fetches the last-saved data snapshot from the MySQL Database.

This approach is able to dramatically improve both synchronization performance and reliability. A few other techniques are used to optimize this process, such as compressing the serialized user data json using Snappy.

</details>

<details>
<summary>&nbsp;<b>Why doesn't HuskSync sync player economy balances / support Vault?</b></summary>

This is a very common request, but there's a good reason why HuskSync does not support this.

The Vault API is designed to be a central "Vault" for storing user data. It's the role of economy plugins that *implement* vault to handle the data storage -- and, by extension, synchronization cross-server. Plugins that *hook into* Vault then expect to be able to use the Vault API to get the player's latest economy balance and data.

Plugins such as MySQLPlayerDataBridge that support synchronizing Vault *hook into* Vault and as a result can violate this expectation&mdash;plugins that expect Vault to return the latest user data no longer can. As a result, plugins like MySQLPlayerDataBridge have to provide lots of manual hooks and tweaks for individual plugins to ensure compatibility. 

This causes all sorts of compatibility issues with unsupported plugins and increases plugin size and update workload.

As a result, I recommend using an economy plugin (that directly *implements* the Vault API), that works cross-server. XConomy is a popular choice for this, which I have personally had a good experience with in the past.

</details>

<details>
<summary>&nbsp;<b>Is this better than MySQLPlayerDataBridge?</b></summary>

I can't provide a fair answer to this question! What I can say is that your mileage may vary. The performance improvements offered by HuskSync's synchronization method will depend on your network environment and the economies of scale that come with your player count.

With that said, servers running plugins or mods that make use of custom items (such as MMOItems, SlimeFun) are not supported by HuskSync and so MySQLPlayerDataBridge may be a better choice for you.

A migrator from MPDB is built-in to HuskSync.

</details>
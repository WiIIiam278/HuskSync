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

Vault is a plugin that provides a common API for developers to do two things:

1. Developers can _implement_ Vault to create economy plugins
2. Developers can _target_ Vault to modify and check economy balances without having to write code to hook into individual economy plugins

In essence, Vault is beneficial as it allows developers to write less code. A developer only needs to write code that targets the Vault API when you need to do stuff with player economy balances.

_Vault itself, however, is not an Economy plugin_. The developers of Economy plugins that _implement_ are responsible for writing the implementation code and database systems for creating player economy accounts and updating balances. By extension, this also means it is the responsibility of Economy plugin developers to implement Vault's API in a way that allows that data to be synchronized cross-server; Vault itself does not contain API for doing so.

Most Economy plugins do not support doing this, however, as cross-server support isn't (and historically hasn't) been a priority. _MySQLPlayerDataBridge_ allows you to workaround this and synchronize Vault balances &mdash; but as detailed above, since Vault itself is not an economy plugin, the way this works is MySQLPlayerDataBridge has to provide and continually maintain a bespoke laundry list of manual, individual hooks and tweaks for both Economy plugins that _implement_ Vault and other plugins that _target_ Vault.

Implementing a similar system in HuskSync would considerably increase the size of the codebase, lengthen update times, and decrease overall system stability. The much better solution is to use an Economy plugin that _implements_ Vault in a way that works cross-server.

Indeed, there exist economy plugins &mdash; such as [XConomy](https://github.com/YiC200333/XConomy) and [RedisEconomy](https://github.com/Emibergo02/RedisEconomy) which do just this, and this is my recommended solution. Need to move from an incompatible Economy plugin? Vault provides methods for transferring balances between Economy plugins (`/vault-convert`).
</details>

<details>
<summary>&nbsp;<b>Is this better than MySQLPlayerDataBridge?</b></summary>

I can't provide a fair answer to this question! What I can say is that your mileage will of course vary. 

The performance improvements offered by HuskSync's synchronization method will depend on your network environment and the economies of scale that come with your player count. In terms of featureset, HuskSync does feature greater rollback and snapshot backup/management features if this is something you are looking for.
</details>

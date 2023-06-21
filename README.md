<!--suppress ALL -->
<p align="center">
    <img src="images/banner.png" alt="HuskSync" />
    <a href="https://github.com/WiIIiam278/HuskSync/actions/workflows/ci.yml">
        <img src="https://img.shields.io/github/actions/workflow/status/WiIIiam278/HuskSync/ci.yml?branch=master&logo=github"/>
    </a> 
    <a href="https://jitpack.io/#net.william278/HuskSync">
        <img src="https://img.shields.io/jitpack/version/net.william278/HuskSync?color=%2300fb9a&label=api&logo=gradle" />
    </a> 
    <a href="https://discord.gg/tVYhJfyDWG">
        <img src="https://img.shields.io/discord/818135932103557162.svg?label=&logo=discord&logoColor=fff&color=7389D8&labelColor=6A7EC2" />
    </a> 
    <br/>
    <b>
        <a href="https://www.spigotmc.org/resources/husksync.97144/">Spigot</a>
    </b> —
    <b>
        <a href="https://william278.net/docs/husksync/setup">Setup</a>
    </b> — 
    <b>
        <a href="https://william278.net/docs/husksync/">Docs</a>
    </b> — 
    <b>
        <a href="https://github.com/WiIIiam278/HuskSync/issues">Issues</a>
    </b>
</p>
<br/>

**HuskSync** is a modern, cross-server player data synchronisation system that enables the comprehensive synchronisation of your user's data across multiple proxied servers. It does this by making use of Redis and MySQL to optimally cache data while players change servers.

## Features
**⭐ Seamless synchronisation** &mdash; Utilises optimised Redis caching when players change server to sync player data super quickly for a seamless experience.

**⭐ Complete player synchronisation** &mdash; Sync inventories, Ender Chests, health, hunger, effects, advancements, statistics, locked maps & [more](https://william278.net/docs/husksync/sync-features)—no data left behind!

**⭐ Backup, restore & rotate** &mdash; Something gone wrong? Restore players back to a previous data state. Rotate and manage data snapshots in-game!

**⭐ Import existing data** &mdash; Import your MySQLPlayerDataBridge data—or from your existing world data! No server reset needed!

**⭐ Works great with Plan** &mdash; Stay in touch with your community through HuskSync analytics on your Plan web panel.

**⭐ Extensible API & open-source** &mdash; Need more? Extend the plugin with the Developer API. Or, submit a pull request through our code bounty system!

**Ready?** [It's syncing time!](https://william278.net/docs/husksync/setup)

## Setup
Requires a MySQL (v8.0+) database, a Redis (v5.0+) server and any number of Spigot-based 1.16.5+ Minecraft servers, running Java 16+.

1. Place the plugin jar file in the /plugins/ directory of each Spigot server. You do not need to install HuskSync as a proxy plugin.
2. Start, then stop every server to let HuskSync generate the config file.
3. Navigate to the HuskSync config file on each server (~/plugins/HuskSync/config.yml) and fill in both the MySQL and Redis database credentials.
4. Start every server again and synchronization will begin.

## Building
To build HuskSync, simply run the following in the root of the repository:

```bash
./gradlew clean build
```

## License
HuskSync is a premium resource. This source code is provided as reference only for those who have purchased the resource from an official source.

- [License](https://github.com/WiIIiam278/HuskSync/blob/master/LICENSE)

## Contributing
A code bounty program is in place for HuskSync, where developers making significant code contributions to HuskSync may be entitled to a license at my discretion to use HuskSync in commercial contexts without having to purchase the resource. Please read the information for contributors in the LICENSE file before submitting a pull request.

## Translations
Translations of the plugin locales are welcome to help make the plugin more accessible. Please submit a pull request with your translations as a `.yml` file.

- [Locales Directory](https://github.com/WiIIiam278/HuskSync/tree/master/common/src/main/resources/languages)
- [English Locales](https://github.com/WiIIiam278/HuskSync/tree/master/common/src/main/resources/languages/en-gb.yml)

## Links
- [Docs](https://william278.net/docs/husksync/) &mdash; Read the plugin documentation!
- [Spigot](https://www.spigotmc.org/resources/husksync.97144/) &mdash; View the Spigot resource page (Also: [Polymart](https://polymart.org/resource/husksync.1634), [Craftaro](https://craftaro.com/marketplace/product/husksync.758))
- [Issues](https://github.com/WiIIiam278/HuskSync/issues) &mdash; File a bug report or feature request
- [Discord](https://discord.gg/tVYhJfyDWG) &mdash; Get help, ask questions (Proof of purchase required)
- [bStats](https://bstats.org/plugin/bukkit/HuskSync%20-%20Bukkit/13140) &mdash; View plugin metrics

---
&copy; [William278](https://william278.net/), 2023. All rights reserved.

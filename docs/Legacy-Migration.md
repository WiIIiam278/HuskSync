This guide will walk you through how to upgrade from HuskSync v1.4.x to HuskSync 2.x.

## Requirements
- MySQL Database with HuskSync v1.4.x data
  - Migration from SQLite is not supported, as HuskSync v2.x requires a MySQL database and does not support SQLite. Appologies for the inconvenience.
  - If you're running v1.3.x or older, follow the update instructions to 1.4.x first before updating to 2.x.

## Migration Instructions
### 1. Uninstall HuskSync v1.x from all servers
- Switch off all servers and your proxy
- Delete the jarfile from your `~/plugins/` folders on your Spigot servers
- Also delete the jarfile from your `~/plugins/` folders on your Proxy. HuskSync v2.x no longer requires a proxy plugin.
- Delete (or make a copy and delete) all HuskSync config data folders (`~/plugins/HuskSync/`). HuskSync 2.x has a new config and messages file.

### 2. Install HuskSync v2.x on all Spigot servers
- HuskSync v2.x must only be installed on your Spigot servers, not your proxy.
- Follow the setup instructions [here](Setup).

### 3. Configure the migrator
- With your servers back on and correctly configured to run HuskSync v2.x, ensure nobody is online.
- Use the console on one of your Spigot servers to enter: `husksync migrate legacy`
- Carefully read the migration configuration instructions. In most cases, you won't have to change the settings, but if you do need to adjust them, use `husksync migrate legacy set <setting> <value>`.
- Migration will be carried out *from* the database you specify with the settings in console *to* the database configured in `config.yml`. If you're migrating from multiple clusters, ensure you run the migrator on the correct servers corresponding to the migrator.

### 4. Start the migrator
- Run `husksync migrate legacy start` to begin the migration process. This may take some time, depending on the amount of data you're migrating.

### 5. Ensure the migration was successful
- HuskSync will notify in console when migration is complete. Verify that the migration went OK by logging in and using the `/userdata list <username>` command to see if the data was imported with the `legacy migration` cause. 
- You can delete the old tables in the database if you want. Be careful to make sure you delete the right ones. By default the *new* table names are `husksync_users` and `husksync_user_data` and the *old* ones were `husksync_players` and `husksync_data`, but you may have changed these.
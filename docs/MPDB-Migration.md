This guide will walk you through how to migrate from MySQLPlayerDataBridge (MPDB) to HuskSync v2.x.

## Requirements
- Spigot servers with MySQLPlayerDataBridge *still installed*

## Migration Instructions
### 1. Install HuskSync v2.x on all Spigot servers
- Download, then install HuskSync on all your servers. Don't uninstall MySQLPlayerDataBridge yet.
- Follow the setup instructions [here](Setup).
- Start your servers again when done.

### 2. Configure the migrator
- With your servers back on and correctly configured to run HuskSync v2.x, ensure nobody is online.
- Use the console on one of your Spigot servers to enter: `husksync migrate mpdb`. If the MPDB migrator is not available, ensure MySQLPlayerDataBridge is still installed.
- Adjust the migration setting as needed using the following command: `husksync migrate mpdb set <setting> <value>`.
- Note that migration will be carried out *from* the database you specify with the settings in console *to* the database configured in `config.yml`.

### 3. Start the migrator
- Run `husksync migrate mpdb start` to begin the migration process. This may take some time, depending on the amount of data you're migrating.

### 4. Uninstall MySQLPlayerDataBridge
- HuskSync will display a message in console when data migration is complete.
- Stop all your Spigot servers and remove the MySQLPlayerDataBridge jar from each of them.
- Start your Spigot servers again.

### 5. Ensure the migration was successful
- Verify that the migration was successful by logging in and using the `/userdata list <username>` command to see if the data was imported with the `mpdb_migration` cause. 
- You can delete the old tables in the database if you want. Be careful to make sure you delete the correct ones.
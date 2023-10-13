This page contains the configuration structure for HuskSync.

## Configuration structure
📁 `plugins/HuskSync/`
- 📄 `config.yml`: General plugin configuration
- 📄 `server.yml`: Server ID configuration
- 📄 `messages-xx-xx.yml`: Plugin locales, formatted in MineDown (see [[Translations]])

## Example files
<details>
<summary>config.yml</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃        HuskSync Config       ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┣╸ Information: https://william278.net/project/husksync
# ┣╸ Config Help: https://william278.net/docs/husksync/config-file/
# ┗╸ Documentation: https://william278.net/docs/husksync
# Locale of the default language file to use. Docs: https://william278.net/docs/husksync/translations
language: en-gb
# Whether to automatically check for plugin updates on startup
check_for_updates: true
# Specify a common ID for grouping servers running HuskSync. Don't modify this unless you know what you're doing!
cluster_id: ''
# Enable development debug logging
debug_logging: false
# Whether to provide modern, rich TAB suggestions for commands (if available)
brigadier_tab_completion: false
# Whether to enable the Player Analytics hook. Docs: https://william278.net/docs/husksync/plan-hook
enable_plan_hook: true
database:
  # Type of database to use (MYSQL, MARIADB)
  type: MYSQL
  credentials:
    # Specify credentials here for your MYSQL or MARIADB database
    host: localhost
    port: 3306
    database: HuskSync
    username: root
    password: pa55w0rd
    parameters: ?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8
  connection_pool:
    # MYSQL / MARIADB database Hikari connection pool properties. Don't modify this unless you know what you're doing!
    maximum_pool_size: 10
    minimum_idle: 10
    maximum_lifetime: 1800000
    keepalive_time: 0
    connection_timeout: 5000
  # Names of tables to use on your database. Don't modify this unless you know what you're doing!
  table_names:
    users: husksync_users
    user_data: husksync_user_data
redis:
  credentials:
    # Specify the credentials of your Redis database here. Set "password" to '' if you don't have one
    host: localhost
    port: 6379
    password: ''
  use_ssl: false
synchronization:
  # The mode of data synchronization to use (DELAY or LOCKSTEP). DELAY should be fine for most networks. Docs: https://william278.net/docs/husksync/sync-modes
  mode: DELAY
  # The number of data snapshot backups that should be kept at once per user
  max_user_data_snapshots: 16
  # Number of hours between new snapshots being saved as backups (Use "0" to backup all snapshots)
  snapshot_backup_frequency: 4
  # List of save cause IDs for which a snapshot will be automatically pinned (so it won't be rotated). Docs: https://william278.net/docs/husksync/data-rotation#save-causes
  auto_pinned_save_causes:
    - INVENTORY_COMMAND
    - ENDERCHEST_COMMAND
    - BACKUP_RESTORE
    - LEGACY_MIGRATION
    - MPDB_MIGRATION
  # Whether to create a snapshot for users on a world when the server saves that world
  save_on_world_save: true
  save_on_death:
    # Whether to create a snapshot for users when they die (containing their death drops)
    enabled: true
    # What items to save in death snapshots? (DROPS or ITEMS_TO_KEEP). Note that ITEMS_TO_KEEP (suggested for keepInventory servers) requires a Paper 1.19.4+ server
    items_to_save: DROPS
    # Should a death snapshot still be created even if the items to save on the player's death are empty?
    save_empty_items: false
    # Whether dead players who log out and log in to a different server should have their items saved.
    sync_dead_players_changing_server: true
  # Whether to use the snappy data compression algorithm. Keep on unless you know what you're doing
  compress_data: true
  # Where to display sync notifications (ACTION_BAR, CHAT, TOAST or NONE)
  notification_display_slot: ACTION_BAR
  # (Experimental) Persist Cartography Table locked maps to let them be viewed on any server
  persist_locked_maps: true
  # Whether to synchronize player max health (requires health syncing to be enabled)
  synchronize_max_health: true
  # If using the DELAY sync method, how long should this server listen for Redis key data updates before pulling data from the database instead (i.e., if the user did not change servers).
  network_latency_milliseconds: 500
  # Which data types to synchronize (Docs: https://william278.net/docs/husksync/sync-features)
  features:
    hunger: true
    persistent_data: false
    inventory: true
    game_mode: true
    advancements: true
    experience: true
    ender_chest: true
    potion_effects: true
    location: false
    statistics: true
    health: true
  # Commands which should be blocked before a player has finished syncing (Use * to block all commands)
  blacklisted_commands_while_locked:
    - '*'
  # Event priorities for listeners (HIGHEST, NORMAL, LOWEST). Change if you encounter plugin conflicts
  event_priorities:
    quit_listener: LOWEST
    join_listener: LOWEST
    death_listener: NORMAL
```

</details>

<details>
<summary>server.yml</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃   HuskSync Server ID config  ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┣╸ This file should contain the ID of this server as defined in your proxy config.
# ┗╸ If you join it using /server alpha, then set it to 'alpha' (case-sensitive)
name: beta
```

</details>
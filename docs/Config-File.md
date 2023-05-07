This page contains the configuration file reference for HuskSync. The config file is located in `/plugins/HuskSync/config.yml`

## Example config
<details>
<summary>config.yml</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃        HuskSync Config       ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┣╸ Information: https://william278.net/project/husksync
# ┗╸ Documentation: https://william278.net/docs/husksync
language: en-gb
check_for_updates: true
cluster_id: ''
debug_logging: false
database:
  credentials:
    # Database connection settings
    host: localhost
    port: 3306
    database: HuskSync
    username: root
    password: pa55w0rd
    parameters: ?autoReconnect=true&useSSL=false
  connection_pool:
    # MySQL connection pool properties
    maximum_pool_size: 10
    minimum_idle: 10
    maximum_lifetime: 1800000
    keepalive_time: 0
    connection_timeout: 5000
  table_names:
    users: husksync_users
    user_data: husksync_user_data
redis:
  credentials:
    # Redis connection settings
    host: localhost
    port: 6379
    password: ''
  use_ssl: false
synchronization:
  # Synchronization settings
  max_user_data_snapshots: 5
  save_on_world_save: true
  save_on_death: false
  save_empty_drops_on_death: true
  compress_data: true
  notification_display_slot: ACTION_BAR
  synchronise_dead_players_changing_server: true
  network_latency_milliseconds: 500
  features:
    health: true
    statistics: true
    persistent_data_container: false
    hunger: true
    ender_chests: true
    advancements: true
    location: false
    game_mode: true
    potion_effects: true
    locked_maps: false
    inventories: true
    max_health: true
    experience: true
  blacklisted_commands_while_locked: []
  event_priorities:
    join_listener: LOWEST
    quit_listener: LOWEST
    death_listener: NORMAL
```

</details>

## Messages files
You can customize the plugin locales, too, by editing your `messages-xx-xx.yml` file. This file is formatted using [MineDown syntax](https://github.com/Phoenix616/MineDown). For more information, see [[Translations]].
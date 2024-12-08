HuskSync persists player data and snapshots in a database of your choice. This is separate from a [[Redis]] server, which HuskSync uses for caching and inter-server messaging, which is also required to use HuskSync.

## Database types
> **Warning:** There is no automatic way of migrating between _database_ types. Changing the database type will cause data to be lost.

| Type                      | Database Software         |
|:--------------------------|:--------------------------|
| `MYSQL`                   | MySQL 8.0 or newer        |
| `MARIADB`                 | MariaDB 5.0 or newer      |
| `POSTGRES`                | PostgreSQL                |
| [`MONGO`](#mongodb-setup) | MongoDB                   |

## Configuring
To change the database type, navigate to your [`config.yml`](Config-File) file and modify the properties under `database`.

<details>
<summary>Database options (config.yml)</summary>

```yaml
# Database settings
database:
  # Type of database to use (MYSQL, MARIADB, POSTGRES, MONGO)
  type: MYSQL
  # Specify credentials here for your MYSQL, MARIADB, POSTGRES OR MONGO database
  credentials:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: ''
    # Only change this if you're using MARIADB or POSTGRES
    parameters: ?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8
  # MYSQL, MARIADB, POSTGRES database Hikari connection pool properties. Don't modify this unless you know what you're doing!
  connection_pool:
    maximum_pool_size: 10
    minimum_idle: 10
    maximum_lifetime: 1800000
    keepalive_time: 0
    connection_timeout: 5000
  # Advanced MongoDB settings. Don't modify unless you know what you're doing!
  mongo_settings:
    using_atlas: false
    parameters: ?retryWrites=true&w=majority&authSource=HuskSync
  # Names of tables to use on your database. Don't modify this unless you know what you're doing!
  table_names:
    users: husksync_users
    user_data: husksync_user_data
```
</details>

### Credentials
You will need to specify the credentials (hostname, port, username, password and the database). These credentials are used to connect to your database server.

If your database server account doesn't have a password (not recommended), leave the password field blank (`password: ''`') and the plugin will attempt to connect without a password.

### Connection Pool properties
If you're using MySQL, MariaDB, or PostgreSQL as your database type, you can modify the HikariCP connection pool properties if you know what you're doing.

Please note that modifying these values can cause issues if you don't know what you're doing. The default values should be fine for most users. 

## MongoDB Setup
If you're using a MongoDB database, in addition to setting the database type to `MONGO`, you'll need to perform slightly different configuration steps.

- Under `credentials` in the `database` section, enter the credentials of your MongoDB Database. You shouldn't touch the `connection_pool` properties.
- Under `parameters` in the `mongo_settings` section, ensure the specified `&authSource=` matches the database you are using (default is `HuskSync`).

### MongoDB Atlas setup
If you're using a MongoDB Atlas database, you'll also need to set the Atlas settings and adjust the connection parameters string.

- Set `using_atlas` in the `mongo_settings` section to `true`.
- Remove `&authSource=HuskSync` from `parameters` in the `mongo_settings`.

Note that the `port` setting in `credentials` is ignored when using Atlas.
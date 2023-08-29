This page contains a table of HuskSync commands and their required permission nodes.

| Command                                       | Description                                   |     | Permission                            |
|-----------------------------------------------|-----------------------------------------------|-----|---------------------------------------|
| `/husksync`                                   | Use `/husksync` subcommands                   |     | `husksync.command.husksync`           |
| `/husksync info`                              | View plugin information                       |     | `husksync.command.husksync info`      |
| `/husksync reload`                            | Reload config & message files                 |     | `husksync.command.husksync.reload`    |
| `/husksync update`                            | Check if an update is available               |     | `husksync.command.husksync.update`    |
| `/husksync migrate <migrator> [args]`         | Migrate user data                             |     | _Console-only_                        |
| `/userdata`                                   | Use `/userdata` subcommands                   |     | `husksync.command.userdata`           |
| `/userdata view <username> [version_uuid]`    | View a snapshot of user data                  |     | `husksync.command.userdata.view`      |
| `/userdata restore <username> <version_uuid>` | Restore a snapshot of user data               |     | `husksync.command.userdata.restore`   |
| `/userdata delete <username> <version_uuid>`  | Delete a snapshot of user data                |     | `husksync.command.userdata.delete`    |
| `/userdata pin <username> <version_uuid>`     | Pin a snapshot of user data                   |     | `husksync.command.userdata.pin`       |
| `/userdata dump <username> <version_uuid>`    | [Dump](dumping-userdata) a user data snapshot |     | `husksync.command.userdata.dump`      |
| `/inventory <username> [version_uuid]`        | View a user's inventory contents              |     | `husksync.command.inventory`&dagger;  |
| `/enderchest <username> [version_uuid]`       | View a user's ender chest contents            |     | `husksync.command.enderchest`&dagger; |

&dagger; The respective `husksync.command.inventory.edit` and `husksync.command.enderchest.edit` permission node is required to be able to edit a user's inventory/ender chest using the interface.
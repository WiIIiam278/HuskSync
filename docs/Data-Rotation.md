HuskSync provides options for backing up and automatically rotating user data. That way, if something goes wrong, you can restore a user from a previous snapshot of user data.

## Snapshots
HuskSync creates what is known as "Snapshots" of a user's data whenever it saves data.

Each user data snapshot has:
- a unique ID
- a timestamp (when it was created)
- a save cause (why it was created)
- a flag to indicate if the snapshot has been pinned (preventing it from being rotated)
- a map of saved data

By default, HuskSync will automatically replace the user's current snapshot in the database if it has been less than 4 hours since the last snapshot was created. This can be changed in the `config.yml` file by changing the `snapshot_backup_frequency` setting under `synchronization`. Setting this to "0" will save a new snapshot each time data is saved.

HuskSync will keep the 16 most recent data snapshots for each user (including their current data). After that, when a new user snapshot is set, the oldest snapshot will automatically be deleted. You can change the number of snapshots to keep by changing the `max_user_data_snapshots` setting (minimum 1).

Pinned user data snapshots are exempt from being replaced/rotated and can only be deleted manually in-game.

## Viewing user data
To view a list of a user's snapshots, use `/userdata list [username]`. Their most recent snapshots will be listed from the database, from newest to oldest. You can click the buttons to navigate through their pages.

![Data snapshot list](https://raw.githubusercontent.com/WiIIiam278/HuskSync/master/images/data-snapshot-list.png)

Snapshots marked with a star after the number have been pinned. Hover over it to view more information.

You can then click on the items in the list in chat to view an overview of each snapshot. Alternatively, to view a user's most recent data snapshot, use `/userdata view [username]`.

[Data snapshot viewer](https://raw.githubusercontent.com/WiIIiam278/HuskSync/master/images/data-snapshot-viewer.png)

## Managing user data
You can use the "Manage" buttons to manage user data. These buttons will only appear if you have the userdata command manage permission. (See [[Commands]]) 
- Click on "Delete" to remove the data
- Click on "Restore" to restore the user data. If the user is online, their items and stats will be updated, otherwise their data will be set to this snapshot upon their next login.
- Click on "Pin" to pin the user data. An indicator will appear in the data viewer and list marking the snapshot as being pinned. 

## Save causes
Data save causes, marked with a 🚩 flag, indicate what caused the data to be saved.

- **disconnect**: Indicates data saved when a player disconnected from the server (either to change servers, or to log off)
- **world save**: Indicates data saved when the world saved. This can be turned off in `config.yml` by setting `save_on_world_save` to false under `synchronization`.
- **server shutdown**: Indicates data saved when the server shut down
- **inventory command**: Indicates data was saved by editing inventory contents via the `/inventory` command
- **enderchest command**: Indicates data was saved by editing Ender Chest contents via the `/enderchest` command
- **backup restore**: Indicates data was saved by restoring it from a previous version
- **api**: Indicates data was saved by a call to the HuskSync [[API]]
- **mpdb migration**: Indicates data was saved from being imported from MySQLPlayerDataBridge (See [[MPDB Migration]])
- **legacy migration**: Indicates data was saved from being imported from a legacy version (v1.x - See [[Legacy Migration]])
- **converted from v2**: Indicates data was automatically converted from HuskSync v2.0's data format 
- **unknown**: Indicates data was saved by an unknown saveCause.
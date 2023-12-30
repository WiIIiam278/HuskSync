This page contains a table of HuskSync commands and their required permission nodes. You can also use wildcard patterns for each command, such as `husksync.command.<command_name>.*` to grant access to all sub-commands.

<table>
    <thead>
        <tr>
            <th colspan="2">Command</th>
            <th>Description</th>
            <th>Permission</th>
        </tr>
    </thead>
    <tbody>
        <!-- /husksync command -->
        <tr>
            <td rowspan="6"><code>/husksync</code></td>
            <td><code>/husksync</code></td>
            <td>View & manage plugin system information</td>
            <td><code>husksync.command.husksync</code></td>
        </tr>
        <tr>
            <td><code>/husksync about</code></td>
            <td>View information about the plugin</td>
            <td><code>husksync.command.husksync.about</code></td>
        </tr>
        <tr>
            <td><code>/husksync status</code></td>
            <td>View plugin system status information</td>
            <td><code>husksync.command.husksync.status</code></td>
        </tr>
        <tr>
            <td><code>/husksync reload</code></td>
            <td>Reload the plugin configuration</td>
            <td><code>husksync.command.husksync.reload</code></td>
        </tr>
        <tr>
            <td><code>/husksync migrate</code></td>
            <td>Migrate data from other plugins/legacy versions</td>
            <td><i>(Console-only)</i></td>
        </tr>
        <tr>
            <td><code>/husksync update</code></td>
            <td>Check for plugin updates</td>
            <td><code>husksync.command.husksync.update</code></td>
        </tr>
        <!-- /userdata command -->
        <tr>
            <td rowspan="7"><code>/userdata</code></td>
            <td><code>/userdata</code></td>
            <td>View & manage user data snapshots</td>
            <td><code>husksync.command.userdata</code></td>
        </tr>
        <tr>
            <td><code>/userdata list</code></td>
            <td>View a list of a player's data snapshots</td>
            <td><code>husksync.command.userdata.list</code></td>
        </tr>
        <tr>
            <td><code>/userdata view</code></td>
            <td>View a player's user data snapshot</td>
            <td><code>husksync.command.userdata.view</code></td>
        </tr>
        <tr>
            <td><code>/userdata restore</code></td>
            <td>Restore a data snapshot for a user</td>
            <td><code>husksync.command.userdata.restore</code></td>
        </tr>
        <tr>
            <td><code>/userdata delete</code></td>
            <td>Delete user data snapshots</td>
            <td><code>husksync.command.userdata.delete</code></td>
        </tr>
        <tr>
            <td><code>/userdata pin</code></td>
            <td>Pin and unpin a user data snapshots</td>
            <td><code>husksync.command.userdata.pin</code></td>
        </tr>
        <tr>
            <td><code>/userdata dump</code></td>
            <td>Dump a user data snapshot</td>
            <td><code>husksync.command.userdata.dump</code></td>
        </tr>
        <!-- /inventory command -->
        <tr>
            <td rowspan="2" colspan="2"><code>/inventory</code></td>
            <td>View the inventory of a user/a data snapshot</td>
            <td><code>husksync.command.inventory</code></td>
        </tr>
        <tr>
            <td>Edit the contents of a user's current inventory</td>
            <td><code>husksync.command.inventory.edit</code></td>
        </tr>
        <!-- /enderchest command -->
        <tr>
            <td rowspan="2" colspan="2"><code>/enderchest</code></td>
            <td>View the Ender Chest of a user/a data snapshot</td>
            <td><code>husksync.command.enderchest</code></td>
        </tr>
        <tr>
            <td>Edit the contents of a user's current Ender Chest</td>
            <td><code>husksync.command.enderchest.edit</code></td>
        </tr>
    </tbody>
</table>
It's possible to dump user data snapshots to `json` objects as of HuskSync v2.1, either to a file or to a web paste service (`mc.lo.gs`). This can be performed through the `/userdata dump` command.

This can be useful in debugging synchronization problems or for manually inspecting data.

## How-to guide
1. Ensure you have the `husksync.command.userdata.dump` permission node. This is not set by default, even for operators.
2. Use the `/userdata list <user>` command to view a list of user data entries for a user.
3. Click on one of the user data entries for your chosen user. The data snapshot preview menu should appear, along with two new buttons at the bottom.

![Data dumping buttons](https://raw.githubusercontent.com/WiIIiam278/HuskSync/master/images/data-dumping.png)

### Dumping to a file
After clicking the "File Dump..." button (equivalent to `/userdata dump <user> <snapshot-id> file`), a dump of this user data entry will be output in `~/plugins/HuskSync/dumps/`.

The name of the generated .json file will match the following format: `<username>_<timestamp>_<save-saveCause>_<short-uuid>.json`

<details>
<summary>Example output file: William278_2022-10-12_21-46-37_disconnect_f7719f5c.json</summary>

```json
{
  "id": "209a56fd-efd0-4354-8f7c-e09f6d0673d8",
  "pinned": false,
  "timestamp": "2023-09-15T17:27:08.6768038+01:00",
  "save_cause": "DISCONNECT",
  "server": "alpha",
  "minecraft_version": "1.20.2",
  "platform_type": "bukkit",
  "format_version": 4,
  "data": {
    "husksync:statistics": "{\"generic\":{\"minecraft:fly_one_cm\":26261,\"minecraft:jump\":23,\"minecraft:leave_game\":3,\"minecraft:play_one_minute\":1904,\"minecraft:sneak_time\":7,\"minecraft:sprint_one_cm\":1849,\"minecraft:time_since_death\":1904,\"minecraft:time_since_rest\":1904,\"minecraft:total_world_time\":1904,\"minecraft:walk_one_cm\":414},\"blocks\":{},\"items\":{},\"entities\":{}}",
    "husksync:experience": "{\"total_experience\":0,\"exp_level\":0,\"exp_progress\":0.0}",
    "husksync:game_mode": "{\"game_mode\":\"CREATIVE\",\"allow_flight\":true,\"is_flying\":true}",
    "husksync:advancements": "[{\"key\":\"minecraft:recipes/decorations/crafting_table\",\"completed_criteria\":{\"unlock_right_away\":1694795225426}},{\"key\":\"minecraft:adventure/adventuring_time\",\"completed_criteria\":{\"minecraft:old_growth_birch_forest\":1694795225478}}]",
    "husksync:inventory": "{held_item_slot:0,items:{size:41}}",
    "husksync:ender_chest": "{size:27}",
    "husksync:potion_effects": "[]",
    "husksync:hunger": "{\"food_level\":20,\"saturation\":5.0,\"exhaustion\":0.449}",
    "husksync:health": "{\"health\":20.0,\"max_health\":20.0,\"health_scale\":20.0}"
  }
}
```
</details>

### Dumping to the web
The \[Web Dump...\] button (equivalent to `/userdata dump <user> <snapshot-id> web`) will dump user data snapshot json data to the https://mclo.gs service and provide you with a link to the uploaded file. Note that the web dumping service may not work if your user data snapshot exceeds 10MB in file size.

It's possible to dump user data snapshots to `json` objects as of HuskSync v2.1, either to a file or to a web paste service (`mc.lo.gs`). This can be performed through the `/userdata dump` command.

This can be useful in debugging synchronization problems or for manually inspecting data.

## How-to guide
1. Grant yourself the special `husksync.command.userdata.dump` permission node. This is not set by default, even for operators.
2. Use the `/userdata list <user>` command to view a list of user data entries for a user.
3. Click on one of the user data entries for your chosen user. The data snapshot preview menu should appear, along with two new buttons at the bottom.

[![Data dumping buttons](https://raw.githubusercontent.com/WiIIiam278/HuskSync/master/images/data-dumping.png)](#)

### Dumping to a file
After clicking the "File Dump..." button (equivalent to `/userdata dump <user> <snapshot-id> file`), a dump of this user data entry will be output in `~/plugins/HuskSync/dumps/`.

The name of the generated .json file will match the following format: `<username>_<timestamp>_<save-cause>_<short-uuid>.json`

<details>
<summary>Example output file: William278_2022-10-12_21-46-37_disconnect_f7719f5c.json</summary>

```json
{
  "status": {
    "health": 20.0,
    "max_health": 20.0,
    "health_scale": 0.0,
    "hunger": 20,
    "saturation": 0.0,
    "saturation_exhaustion": 0.24399996,
    "selected_item_slot": 1,
    "total_experience": 0,
    "experience_level": 0,
    "experience_progress": 0.0,
    "game_mode": "CREATIVE",
    "is_flying": true
  },
  "inventory": {
    "serialized_items": "rO0ABXcEAAAAKXBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBzcgAXamF2YS51dGlsLkxp\r\nbmtlZEhhc2hNYXA0wE5cEGzA+wIAAVoAC2FjY2Vzc09yZGVyeHIAEWphdmEudXRpbC5IYXNoTWFw\r\nBQfawcMWYNEDAAJGAApsb2FkRmFjdG9ySQAJdGhyZXNob2xkeHA/QAAAAAAADHcIAAAAEAAAAAJ0\r\nAAF2c3IAEWphdmEubGFuZy5JbnRlZ2VyEuKgpPeBhzgCAAFJAAV2YWx1ZXhyABBqYXZhLmxhbmcu\r\nTnVtYmVyhqyVHQuU4IsCAAB4cAAADDB0AAR0eXBldAASTEVBVEhFUl9DSEVTVFBMQVRFeABwc3EA\r\nfgAAP0AAAAAAAAx3CAAAABAAAAADcQB+AANzcQB+AAQAAAwwcQB+AAd0AAdCRURST0NLdAAGYW1v\r\ndW50c3EAfgAEAAAAQHgAcHBwcHBwcA\u003d\u003d\r\n"
  },
  "ender_chest": {
    "serialized_items": "rO0ABXcEAAAAG3NyABdqYXZhLnV0aWwuTGlua2VkSGFzaE1hcDTATlwQbMD7AgABWgALYWNjZXNz\r\nT3JkZXJ4cgARamF2YS51dGlsLkhhc2hNYXAFB9rBwxZg0QMAAkYACmxvYWRGYWN0b3JJAAl0aHJl\r\nc2hvbGR4cD9AAAAAAAAMdwgAAAAQAAAAA3QAAXZzcgARamF2YS5sYW5nLkludGVnZXIS4qCk94GH\r\nOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAMMHQABHR5cGV0\r\nAA1TUFJVQ0VfUExBTktTdAAGYW1vdW50c3EAfgAEAAAAQHgAc3EAfgAAP0AAAAAAAAx3CAAAABAA\r\nAAADcQB+AANzcQB+AAQAAAwwcQB+AAd0AAdCRURST0NLcQB+AAlxAH4ACngAc3EAfgAAP0AAAAAA\r\nAAx3CAAAABAAAAADcQB+AANzcQB+AAQAAAwwcQB+AAdxAH4ADXEAfgAJcQB+AAp4AHNxAH4AAD9A\r\nAAAAAAAMdwgAAAAQAAAAA3EAfgADc3EAfgAEAAAMMHEAfgAHcQB+AA1xAH4ACXEAfgAKeABzcQB+\r\nAAA/QAAAAAAADHcIAAAAEAAAAANxAH4AA3NxAH4ABAAADDBxAH4AB3EAfgANcQB+AAlxAH4ACngA\r\nc3EAfgAAP0AAAAAAAAx3CAAAABAAAAADcQB+AANzcQB+AAQAAAwwcQB+AAdxAH4ADXEAfgAJcQB+\r\nAAp4AHNxAH4AAD9AAAAAAAAMdwgAAAAQAAAAA3EAfgADc3EAfgAEAAAMMHEAfgAHcQB+AA1xAH4A\r\nCXEAfgAKeABzcQB+AAA/QAAAAAAADHcIAAAAEAAAAANxAH4AA3NxAH4ABAAADDBxAH4AB3EAfgAN\r\ncQB+AAlxAH4ACngAc3EAfgAAP0AAAAAAAAx3CAAAABAAAAADcQB+AANzcQB+AAQAAAwwcQB+AAdx\r\nAH4ADXEAfgAJcQB+AAp4AHNxAH4AAD9AAAAAAAAMdwgAAAAQAAAAA3EAfgADc3EAfgAEAAAMMHEA\r\nfgAHcQB+AA1xAH4ACXEAfgAKeABzcQB+AAA/QAAAAAAADHcIAAAAEAAAAANxAH4AA3NxAH4ABAAA\r\nDDBxAH4AB3EAfgANcQB+AAlxAH4ACngAc3EAfgAAP0AAAAAAAAx3CAAAABAAAAADcQB+AANzcQB+\r\nAAQAAAwwcQB+AAdxAH4ADXEAfgAJcQB+AAp4AHNxAH4AAD9AAAAAAAAMdwgAAAAQAAAAA3EAfgAD\r\nc3EAfgAEAAAMMHEAfgAHcQB+AA1xAH4ACXEAfgAKeABzcQB+AAA/QAAAAAAADHcIAAAAEAAAAANx\r\nAH4AA3NxAH4ABAAADDBxAH4AB3EAfgANcQB+AAlxAH4ACngAc3EAfgAAP0AAAAAAAAx3CAAAABAA\r\nAAADcQB+AANzcQB+AAQAAAwwcQB+AAdxAH4ADXEAfgAJcQB+AAp4AHNxAH4AAD9AAAAAAAAMdwgA\r\nAAAQAAAAA3EAfgADc3EAfgAEAAAMMHEAfgAHcQB+AA1xAH4ACXEAfgAKeABzcQB+AAA/QAAAAAAA\r\nDHcIAAAAEAAAAANxAH4AA3NxAH4ABAAADDBxAH4AB3EAfgANcQB+AAlxAH4ACngAc3EAfgAAP0AA\r\nAAAAAAx3CAAAABAAAAADcQB+AANzcQB+AAQAAAwwcQB+AAdxAH4ADXEAfgAJcQB+AAp4AHNxAH4A\r\nAD9AAAAAAAAMdwgAAAAQAAAAA3EAfgADc3EAfgAEAAAMMHEAfgAHcQB+AA1xAH4ACXEAfgAKeABz\r\ncQB+AAA/QAAAAAAADHcIAAAAEAAAAANxAH4AA3NxAH4ABAAADDBxAH4AB3EAfgANcQB+AAlxAH4A\r\nCngAc3EAfgAAP0AAAAAAAAx3CAAAABAAAAADcQB+AANzcQB+AAQAAAwwcQB+AAdxAH4ADXEAfgAJ\r\ncQB+AAp4AHNxAH4AAD9AAAAAAAAMdwgAAAAQAAAAA3EAfgADc3EAfgAEAAAMMHEAfgAHcQB+AA1x\r\nAH4ACXEAfgAKeABzcQB+AAA/QAAAAAAADHcIAAAAEAAAAANxAH4AA3NxAH4ABAAADDBxAH4AB3EA\r\nfgANcQB+AAlxAH4ACngAc3EAfgAAP0AAAAAAAAx3CAAAABAAAAADcQB+AANzcQB+AAQAAAwwcQB+\r\nAAdxAH4ACHEAfgAJcQB+AAp4AHNxAH4AAD9AAAAAAAAMdwgAAAAQAAAAA3EAfgADc3EAfgAEAAAM\r\nMHEAfgAHcQB+AAhxAH4ACXEAfgAKeABzcQB+AAA/QAAAAAAADHcIAAAAEAAAAANxAH4AA3NxAH4A\r\nBAAADDBxAH4AB3EAfgAIcQB+AAlxAH4ACngAc3EAfgAAP0AAAAAAAAx3CAAAABAAAAADcQB+AANz\r\ncQB+AAQAAAwwcQB+AAdxAH4ACHEAfgAJcQB+AAp4AA\u003d\u003d\r\n"
  },
  "potion_effects": {
    "serialized_potion_effects": ""
  },
  "advancements": [
    {
      "key": "minecraft:recipes/transportation/mangrove_boat",
      "completed_criteria": {
        "in_water": "Oct 11, 2022, 10:07:07 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/spruce_button",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/misc/iron_nugget_from_smelting",
      "completed_criteria": {
        "has_chainmail_leggings": "Oct 12, 2022, 5:43:37 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/spruce_pressure_plate",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/warped_pressure_plate",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    },
    {
      "key": "minecraft:recipes/decorations/spruce_sign",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/spruce_trapdoor",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/building_blocks/warped_slab",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/spruce_door",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/spruce_fence_gate",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/decorations/crafting_table",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/decorations/chest",
      "completed_criteria": {
        "has_lots_of_items": "Oct 12, 2022, 5:43:42 PM"
      }
    },
    {
      "key": "minecraft:story/shiny_gear",
      "completed_criteria": {
        "diamond_boots": "Oct 12, 2022, 5:43:36 PM"
      }
    },
    {
      "key": "minecraft:recipes/misc/stick",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/warped_fence_gate",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    },
    {
      "key": "minecraft:recipes/transportation/acacia_boat",
      "completed_criteria": {
        "in_water": "Oct 11, 2022, 10:07:07 PM"
      }
    },
    {
      "key": "minecraft:recipes/misc/iron_nugget_from_blasting",
      "completed_criteria": {
        "has_chainmail_leggings": "Oct 12, 2022, 5:43:37 PM"
      }
    },
    {
      "key": "minecraft:recipes/decorations/warped_fence",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    },
    {
      "key": "minecraft:adventure/adventuring_time",
      "completed_criteria": {
        "minecraft:beach": "Oct 12, 2022, 5:10:27 PM",
        "minecraft:old_growth_pine_taiga": "Oct 12, 2022, 9:32:20 PM",
        "minecraft:dark_forest": "Oct 11, 2022, 9:24:06 PM",
        "minecraft:forest": "Oct 11, 2022, 10:06:58 PM",
        "minecraft:taiga": "Oct 12, 2022, 8:58:59 PM",
        "minecraft:river": "Oct 11, 2022, 10:07:07 PM",
        "minecraft:stony_shore": "Oct 11, 2022, 9:23:59 PM",
        "minecraft:snowy_plains": "Oct 11, 2022, 10:08:53 PM",
        "minecraft:snowy_taiga": "Oct 12, 2022, 3:38:05 PM",
        "minecraft:frozen_river": "Oct 11, 2022, 10:09:54 PM",
        "minecraft:windswept_gravelly_hills": "Oct 12, 2022, 3:14:39 PM",
        "minecraft:old_growth_spruce_taiga": "Oct 12, 2022, 9:42:12 PM",
        "minecraft:snowy_beach": "Oct 11, 2022, 10:08:40 PM",
        "minecraft:plains": "Oct 11, 2022, 10:07:07 PM"
      }
    },
    {
      "key": "minecraft:recipes/decorations/barrel",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/transportation/spruce_boat",
      "completed_criteria": {
        "in_water": "Oct 11, 2022, 10:07:07 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/redstone_from_smelting_redstone_ore",
      "completed_criteria": {
        "has_redstone_ore": "Oct 11, 2022, 10:21:34 PM"
      }
    },
    {
      "key": "minecraft:recipes/transportation/birch_boat",
      "completed_criteria": {
        "in_water": "Oct 11, 2022, 10:07:07 PM"
      }
    },
    {
      "key": "minecraft:recipes/decorations/spruce_fence",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/building_blocks/spruce_stairs",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/transportation/oak_boat",
      "completed_criteria": {
        "in_water": "Oct 11, 2022, 10:07:07 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/redstone_from_blasting_redstone_ore",
      "completed_criteria": {
        "has_redstone_ore": "Oct 11, 2022, 10:21:34 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/warped_button",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    },
    {
      "key": "minecraft:recipes/building_blocks/warped_stairs",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/warped_trapdoor",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    },
    {
      "key": "minecraft:recipes/building_blocks/spruce_slab",
      "completed_criteria": {
        "has_planks": "Oct 11, 2022, 9:25:12 PM"
      }
    },
    {
      "key": "minecraft:recipes/decorations/warped_sign",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    },
    {
      "key": "minecraft:recipes/transportation/jungle_boat",
      "completed_criteria": {
        "in_water": "Oct 11, 2022, 10:07:07 PM"
      }
    },
    {
      "key": "minecraft:recipes/transportation/dark_oak_boat",
      "completed_criteria": {
        "in_water": "Oct 11, 2022, 10:07:07 PM"
      }
    },
    {
      "key": "minecraft:recipes/redstone/warped_door",
      "completed_criteria": {
        "has_planks": "Oct 12, 2022, 5:43:22 PM"
      }
    }
  ],
  "statistics": {
    "untyped_statistics": {
      "LEAVE_GAME": 16,
      "TOTAL_WORLD_TIME": 282633,
      "CROUCH_ONE_CM": 43,
      "WALK_UNDER_WATER_ONE_CM": 113,
      "DEATHS": 4,
      "WALK_ONE_CM": 7313,
      "JUMP": 866,
      "SPRINT_ONE_CM": 63807,
      "DROP_COUNT": 9,
      "WALK_ON_WATER_ONE_CM": 331,
      "TIME_SINCE_DEATH": 282357,
      "SNEAK_TIME": 95,
      "FLY_ONE_CM": 584296,
      "ENDERCHEST_OPENED": 2,
      "PLAY_ONE_MINUTE": 282633,
      "TIME_SINCE_REST": 282377
    },
    "block_statistics": {},
    "item_statistics": {
      "PICKUP": {
        "SUGAR_CANE": 2
      },
      "DROP": {
        "SPRUCE_PLANKS": 70,
        "TURTLE_HELMET": 1,
        "DIAMOND_BOOTS": 1,
        "BEDROCK": 1,
        "CHAINMAIL_LEGGINGS": 1,
        "MANGROVE_PROPAGULE": 1,
        "LEATHER_CHESTPLATE": 1
      },
      "USE_ITEM": {
        "TURTLE_HELMET": 1,
        "DIAMOND_BOOTS": 1,
        "GRASS_BLOCK": 5,
        "ENDER_CHEST": 1,
        "CHAINMAIL_LEGGINGS": 1,
        "LEATHER_CHESTPLATE": 1
      }
    },
    "entity_statistics": {}
  },
  "persistent_data_container": {
    "persistent_data_map": {}
  },
  "minecraft_version": "1.19.2",
  "format_version": 3
}
```
</details>

### Dumping to the web
The Web Dump... button (equivalent to `/userdata dump <user> <snapshot-id> web`) will dump user data snapshot json data to the https://mc.lo.gs service and provide you with a link to the uploaded file. Note that the web dumping service may not work if your user data snapshot exceeds 10MB in file size.
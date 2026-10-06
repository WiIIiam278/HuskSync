If you make use of plugins that perform logic with player items or statuses on the quit, join or death events, such as combat logging plugins, you may encounter issues with HuskSync caused by the event execution order.

In the case of combat logging plugins, this can mean that HuskSync is listening to the event called when a player dies, joins or leaves before the combat logger can kill the player and handle their items. In other words, the player will be brought back to life and synchronized as though they didn't die, even though they did. This can lead to item duplication.

HuskSync provides a way of customizing the event priorities—that is, the priorities at which HuskSync listens to event calls—to let you fix this issue.

## Changing event priorities
As of HuskSync v2.1.3+, you can modify event priorities by editing the `synchronization` section of the `config.yml` file, as seen below.

```yaml
synchronization:
  #(...)
  event_priorities:
    join_listener: LOWEST
    death_listener: NORMAL
    quit_listener: LOWEST
```

To change the event execution priority for the join, death or quit listener, simply modify the value to one of the ones listed below, in order of when they are processed:
1. `LOWEST` (executed first, just after the event is fired)
2. `NORMAL` (executed after all LOWEST listeners have finished processing)
3. `HIGHEST` (executed after all NORMAL and LOWEST listeners have finished processing)

Note that by default, HuskSync executes the join and quit events on the earliest listener priority (`LOWEST`). For the `join_listener` this is for synchronization performance reasons. For the `quit_listener` the priority sets when HuskSync locks the player.

Once locked the player can't take damage, drop or pick up items, or interact with anything, and their death drops get cleared.

Note that player data is saved at the end of the quit event (`MONITOR`), so that the save includes any changes other plugins make at the priorities above.

## Combat-loggers
For those using combat logging plugins—the ones that kill players when they disconnect while in PvP—you should try changing the `quit_listener` to having a `NORMAL` or `HIGHEST` priority. HuskSync should only lock a player after the combat logger has killed them, so that their items still drop. This may not work for any combat loggers that act at `MONITOR` priority.
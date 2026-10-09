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
4. `MONITOR` (executed last, after all other priorities have finished processing)

`MONITOR` is not recommended. Other plugins listening at `MONITOR` typically observe events, and wouldn't see any changes HuskSync makes at the same priority. Only use `MONITOR` if a plugin needs HuskSync to run after its own `MONITOR` listener, such as the combat loggers below. Event priorities only apply on Bukkit/Paper.

Note that by default, HuskSync executes the join and quit events on the (`LOWEST`) listener priority. For the `join_listener` this is for synchronization performance reasons. For the `quit_listener` the priority sets when HuskSync locks the player.

Once locked the player can't take damage, drop or pick up items, or interact with anything, and their death drops get cleared.

Note that player data is saved at the end of the quit event (`MONITOR`), so that the save includes any changes other plugins make at the priorities above. HuskSync registers this listener a tick after it enables, so it also runs after other plugins' `MONITOR` listeners. Plugins loaded or reloaded at runtime will register theirs after it, and HuskSync will log a warning, so restart the server instead.

## Players quitting while dead
If a player is dead when they quit, either because they closed the game on the death screen or because a plugin killed them as they left, HuskSync saves their data as dead. They'll respawn on the next server they join.

HuskSync then restores their health on the server they quit, so it doesn't save its own player data for them as dead. Otherwise, they would die again the next time they joined that server. The same is true for players who are dead when the server stops.

This only happens when health is synced via `synchronization.features.health` in the `config.yml` file. Without it, HuskSync can't save the player as dead, and restoring their health would let them skip respawning.

## Combat-loggers
For those using combat logging plugins—the ones that kill players when they disconnect while in PvP—you should try changing the `quit_listener` to having a `NORMAL` or `HIGHEST` priority. HuskSync should only lock a player after the combat logger has killed them, so that their items still drop.

Some combat loggers, such as CombatLogX, kill players at `MONITOR` priority, after `HIGHEST`. For these, set the `quit_listener` to `MONITOR`, so HuskSync locks the player at the same time it saves their data, after the combat logger has killed them. Leave the other listeners as they are.
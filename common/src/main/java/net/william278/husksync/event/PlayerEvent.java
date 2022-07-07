package net.william278.husksync.event;

import net.william278.husksync.player.OnlineUser;

public interface PlayerEvent extends Event {

    OnlineUser getUser();

}

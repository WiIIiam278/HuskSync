package net.william278.husksync.redis;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public interface MessageHandler {

    // Inbound message telling HuskSync on this server to update a user's data (if the users, by UUID, is online)
    default void handleUpdateUserData(@NotNull RedisMessage message) {
        final Optional<OnlineUser> target = getPlugin().getOnlineUser(message.getTargetUuid());
        if (target.isEmpty()) {
            return;
        }

        // Handle the request, update the data locally
        final OnlineUser user = target.get();
        getPlugin().lockPlayer(user.getUuid());
        try {
            final DataSnapshot.Packed data = DataSnapshot.deserialize(getPlugin(), message.getPayload());
            user.applySnapshot(data, DataSnapshot.UpdateCause.UPDATED);
            return;
        } catch (Throwable e) {
            getPlugin().log(Level.SEVERE, "An exception occurred updating user data from Redis", e);
        }
        user.completeSync(false, DataSnapshot.UpdateCause.UPDATED, getPlugin());
    }

    // Inbound message telling HuskSync on this server to reply with a user's data (if the user, by UUID, is online)
    default void handleRequestUserData(@NotNull RedisMessage message) {
        final Optional<OnlineUser> target = getPlugin().getOnlineUser(message.getTargetUuid());
        if (target.isEmpty()) {
            return;
        }

        // Handle the request, return the data
        final OnlineUser user = target.get();
        final RedisMessage reply = RedisMessage.create(
                UUID.fromString(new String(message.getPayload(), StandardCharsets.UTF_8)),
                user.createSnapshot(DataSnapshot.SaveCause.INVENTORY_COMMAND).asBytes(getPlugin())
        );
        reply.dispatch(getPlugin(), RedisMessage.Type.RETURN_USER_DATA);
    }

    // Inbound message containing returned user data from a REQUEST_USER_DATA message. If the server had made a request
    // then it will complete the future in the pendingRequests map.
    default void handleReturnUserData(@NotNull RedisMessage message) {
        final UUID requestId = message.getTargetUuid();
        final CompletableFuture<Optional<DataSnapshot.Packed>> future = getPendingRequests().get(requestId);
        if (future == null) {
            return;
        }
        try {
            final DataSnapshot.Packed data = DataSnapshot.deserialize(getPlugin(), message.getPayload());
            future.complete(Optional.of(data));
        } catch (Throwable e) {
            getPlugin().log(Level.SEVERE, "An exception occurred returning user data from Redis", e);
            future.complete(Optional.empty());
        }
        getPendingRequests().remove(requestId);
    }

    @NotNull
    Map<UUID, CompletableFuture<Optional<DataSnapshot.Packed>>> getPendingRequests();

    @NotNull
    HuskSync getPlugin();

}

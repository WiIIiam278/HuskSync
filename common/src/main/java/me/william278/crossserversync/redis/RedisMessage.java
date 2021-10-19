package me.william278.crossserversync.redis;

import me.william278.crossserversync.Settings;
import redis.clients.jedis.Jedis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.StringJoiner;
import java.util.UUID;

public class RedisMessage {

    public static String REDIS_CHANNEL = "CrossServerSync";

    public static String MESSAGE_META_SEPARATOR = "♦";
    public static String MESSAGE_DATA_SEPARATOR = "♣";

    private final String messageData;
    private final MessageType messageType;
    private MessageTarget messageTarget;

    /**
     * Create a new RedisMessage
     * @param type The type of the message
     * @param target Who will receive this message
     * @param messageData The message data elements
     */
    public RedisMessage(MessageType type, MessageTarget target, String... messageData) {
        final StringJoiner messageDataJoiner = new StringJoiner(MESSAGE_DATA_SEPARATOR);
        for (String dataElement : messageData) {
            messageDataJoiner.add(dataElement);
        }
        this.messageData = messageDataJoiner.toString();
        this.messageType = type;
        this.messageTarget = target;
    }

    /**
     * Get a new RedisMessage from an incoming message string
     * @param messageString The message string to parse
     */
    public RedisMessage(String messageString) {
        String[] messageMetaElements = messageString.split(MESSAGE_META_SEPARATOR);
        messageType = MessageType.valueOf(messageMetaElements[0]);
        messageData = messageMetaElements[2];

        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(messageMetaElements[1].getBytes()))) {
            messageTarget = (MessageTarget) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the full, formatted message string with type, target & data
     * @return The fully formatted message
     */
    private String getFullMessage() {
        return new StringJoiner(MESSAGE_META_SEPARATOR)
                .add(messageType.toString()).add(messageTarget.toString()).add(messageData)
                .toString();
    }

    /**
     * Send the redis message
     */
    public void send() {
            try (Jedis publisher = new Jedis(Settings.redisHost, Settings.redisPort)) {
                final String jedisPassword = Settings.redisPassword;
                if (!jedisPassword.equals("")) {
                    publisher.auth(jedisPassword);
                }
                publisher.connect();
                publisher.publish(REDIS_CHANNEL, getFullMessage());
            }
    }

    public String getMessageData() {
        return messageData;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public MessageTarget getMessageTarget() {
        return messageTarget;
    }

    /**
     * Defines the type of the message
     */
    public enum MessageType {
        /**
         * Sent by Bukkit servers to proxy when a player disconnects with a player's updated data, alongside the UUID of the last loaded {@link me.william278.crossserversync.PlayerData} for the user
         */
        PLAYER_DATA_UPDATE,

        /**
         * Sent by Bukkit servers to proxy to request {@link me.william278.crossserversync.PlayerData} from the proxy.
         */
        PLAYER_DATA_REQUEST,

        /**
         * Sent by the Proxy to reply to a {@code MessageType.PLAYER_DATA_REQUEST}, contains the latest {@link me.william278.crossserversync.PlayerData} for the requester.
         */
        PLAYER_DATA_REPLY
    }

    /**
     * A record that defines the target of a plugin message; a spigot server or the proxy server(s).
     * For Bukkit servers, the name of the server must also be specified
     */
    public record MessageTarget(Settings.ServerType targetServerType, UUID targetPlayerName) implements Serializable { }
}
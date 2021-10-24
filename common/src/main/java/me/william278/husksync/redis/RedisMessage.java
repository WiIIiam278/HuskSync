package me.william278.husksync.redis;

import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.UUID;

public class RedisMessage {

    public static String REDIS_CHANNEL = "HuskSync";

    public static String MESSAGE_META_SEPARATOR = "♦";
    public static String MESSAGE_DATA_SEPARATOR = "♣";

    private final String messageData;
    private final MessageType messageType;
    private final MessageTarget messageTarget;

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
    public RedisMessage(String messageString) throws IOException, ClassNotFoundException {
        String[] messageMetaElements = messageString.split(MESSAGE_META_SEPARATOR);
        messageType = MessageType.valueOf(messageMetaElements[0]);
        messageTarget = (MessageTarget) RedisMessage.deserialize(messageMetaElements[1]);
        messageData = messageMetaElements[2];
    }

    /**
     * Returns the full, formatted message string with type, target & data
     * @return The fully formatted message
     */
    private String getFullMessage() throws IOException {
        return new StringJoiner(MESSAGE_META_SEPARATOR)
                .add(messageType.toString()).add(RedisMessage.serialize(messageTarget)).add(messageData)
                .toString();
    }

    /**
     * Send the redis message
     */
    public void send() throws IOException {
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

    public String[] getMessageDataElements() { return messageData.split(MESSAGE_DATA_SEPARATOR); }

    public MessageType getMessageType() {
        return messageType;
    }

    public MessageTarget getMessageTarget() {
        return messageTarget;
    }

    /**
     * Defines the type of the message
     */
    public enum MessageType implements Serializable {
        /**
         * Sent by Bukkit servers to proxy when a player disconnects with a player's updated data, alongside the UUID of the last loaded {@link PlayerData} for the user
         */
        PLAYER_DATA_UPDATE,

        /**
         * Sent by Bukkit servers to proxy to request {@link PlayerData} from the proxy if they are set as needing to request data on join.
         */
        PLAYER_DATA_REQUEST,

        /**
         * Sent by the Proxy to reply to a {@code MessageType.PLAYER_DATA_REQUEST}, contains the latest {@link PlayerData} for the requester.
         */
        PLAYER_DATA_SET,

        /**
         * Sent by the proxy to a Bukkit server to have them request data on join; contains no data otherwise
         */
        REQUEST_DATA_ON_JOIN,

        /**
         * Sent by the proxy to ask the Bukkit server to send the full plugin information, contains information about the proxy brand and version
         */
        SEND_PLUGIN_INFORMATION,

        /**
         * Sent by the proxy to show a player the contents of another player's inventory, contains their username and {@link PlayerData}
         */
        OPEN_INVENTORY,

        /**
         * Sent by the proxy to show a player the contents of another player's ender chest, contains their username and {@link PlayerData}
         */
        OPEN_ENDER_CHEST,

        /**
         * Sent by both the proxy and bukkit servers to confirm cross-server communication has been established
         */
        CONNECTION_HANDSHAKE,

        /**
         * Sent by both the proxy and bukkit servers to terminate communications (if a bukkit / the proxy goes offline)
         */
        TERMINATE_HANDSHAKE,

        /**
         * Sent by a proxy to a bukkit server to decode MPDB data
         */
        DECODE_MPDB_DATA,

        /**
         * Sent by a bukkit server back to the proxy with the correctly decoded MPDB data
         */
        DECODED_MPDB_DATA_SET,

        /**
         * Sent by the proxy to a bukkit server to initiate a reload
         */
        RELOAD_CONFIG
    }

    public enum RequestOnJoinUpdateType {
        ADD_REQUESTER,
        REMOVE_REQUESTER
    }

    /**
     * A record that defines the target of a plugin message; a spigot server or the proxy server(s).
     * For Bukkit servers, the name of the server must also be specified
     */
    public record MessageTarget(Settings.ServerType targetServerType, UUID targetPlayerUUID) implements Serializable { }

    /**
     * Deserialize an object from a Base64 string
     */
    public static Object deserialize(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(s);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return objectInputStream.readObject();
        }
    }

    /**
     * Serialize an object to a Base64 string
     */
    public static String serialize(Serializable o) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(o);
        }
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }
}
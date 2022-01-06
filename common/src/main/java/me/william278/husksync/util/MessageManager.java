package me.william278.husksync.util;

import java.util.HashMap;

public class MessageManager {

    private static HashMap<String, String> messages = new HashMap<>();

    public static void setMessages(HashMap<String, String> newMessages) {
        messages = new HashMap<>(newMessages);
    }

    public static String getMessage(String messageId) {
        return messages.get(messageId);
    }

    public static StringBuilder PLUGIN_INFORMATION = new StringBuilder().append("[HuskSync](#00fb9a bold) [| %proxy_brand% Version %proxy_version% (%bukkit_brand% v%bukkit_version%)](#00fb9a)\n")
            .append("[%plugin_description%](gray)\n")
            .append("[• Author:](white) [William278](gray show_text=&7Click to pay a visit open_url=https://youtube.com/William27528)\n")
            .append("[• Contributors:](white) [HarvelsX](gray show_text=&7Code)\n")
            .append("[• Plugin Info:](white) [[Link]](#00fb9a show_text=&7Click to open link open_url=https://github.com/WiIIiam278/HuskSync/)\n")
            .append("[• Report Issues:](white) [[Link]](#00fb9a show_text=&7Click to open link open_url=https://github.com/WiIIiam278/HuskSync/issues)\n")
            .append("[• Support Discord:](white) [[Link]](#00fb9a show_text=&7Click to join open_url=https://discord.gg/tVYhJfyDWG)");

    public static StringBuilder PLUGIN_STATUS = new StringBuilder().append("[HuskSync](#00fb9a bold) [| Current system status:](#00fb9a)\n")
            .append("[• Connected servers:](white) [%1%](#00fb9a)")
            .append("[• Cached player data:](white) [%2%](#00fb9a)");

}
package net.zyonmc.zyonhelp.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getString(String path) {
        return config.getString(path, path);
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path, false);
    }

    // Config Paths
    public boolean isDiscordEnabled() {
        return getBoolean("discord-bot.enabled");
    }

    public String getDiscordToken() {
        return getString("discord-bot.token");
    }

    public String getGuildId() {
        return getString("discord-bot.guild-id");
    }

    public String getCategoryId() {
        return getString("discord-bot.category-id");
    }

    public String getStaffRoleId() {
        return getString("discord-bot.staff-role-id");
    }

    public String getStaffPermission() {
        return getString("options.staff-permission");
    }
    
    public boolean isDeleteOnClose() {
        return getBoolean("options.delete-on-close");
    }

    // MySQL
    public boolean isMysqlEnabled() {
        return getBoolean("mysql.enabled");
    }

    public String getMysqlHost() {
        return getString("mysql.host");
    }

    public int getMysqlPort() {
        return config.getInt("mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return getString("mysql.database");
    }

    public String getMysqlUsername() {
        return getString("mysql.username");
    }

    public String getMysqlPassword() {
        return getString("mysql.password");
    }

    // Transcripts
    public boolean isTranscriptsEnabled() {
        return getBoolean("transcripts.enabled");
    }

    public boolean isTranscriptsSaveLocal() {
        return getBoolean("transcripts.save-local");
    }

    public String getMessagePrefix() {
        return getString("messages.prefix");
    }

    public String getMessageTicketClosed() {
        return getString("messages.ticket-closed");
    }

    public String getFormatHelpopToStaff() {
        return getString("messages.format-helpop-to-staff");
    }

    public String getFormatMcStaff() {
        return getString("messages.format-mc-staff");
    }

    public String getFormatDiscordStaff() {
        return getString("messages.format-discord-staff");
    }

    public String getFormatStaffReplyToStaff() {
        return getString("messages.format-staff-reply-to-staff");
    }

    public String getFormatToDiscord() {
        return getString("messages.format-to-discord"); // Discord formatting uses **, etc.
    }

    public String getFormatReplyToDiscord() {
        return getString("messages.format-reply-to-discord");
    }

    // Inactivity
    public boolean isInactivityEnabled() {
        return getBoolean("inactivity.enabled");
    }

    public int getInactivityWaitTime() {
        return config.getInt("inactivity.wait-time", 5);
    }

    public String getInactivityMessage() {
        return getString("inactivity.message");
    }
}

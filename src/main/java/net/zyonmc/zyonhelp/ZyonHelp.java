package net.zyonmc.zyonhelp;

import net.zyonmc.zyonhelp.command.HelpOpCommand;
import net.zyonmc.zyonhelp.command.ZyonHelpCommand;
import net.zyonmc.zyonhelp.manager.ConfigManager;
import net.zyonmc.zyonhelp.manager.DatabaseManager;
import net.zyonmc.zyonhelp.manager.DiscordManager;
import net.zyonmc.zyonhelp.manager.TicketManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ZyonHelp extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private TicketManager ticketManager;
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        // Initialize Managers
        this.configManager = new ConfigManager(this);
        
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.init();

        this.ticketManager = new TicketManager(this.databaseManager);
        
        this.discordManager = new DiscordManager(this);
        this.discordManager.init();

        // Register Commands
        getCommand("helpop").setExecutor(new HelpOpCommand(this));
        getCommand("zyonhelp").setExecutor(new ZyonHelpCommand(this));

        // Inactivity Task (Check every minute)
        getServer().getScheduler().runTaskTimer(this, () -> {
            ticketManager.checkInactivity(this);
        }, 20 * 60L, 20 * 60L);

        getLogger().info("ZyonHelp ha sido habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        if (discordManager != null) {
            discordManager.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("ZyonHelp ha sido deshabilitado correctamente.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TicketManager getTicketManager() {
        return ticketManager;
    }
    
    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}

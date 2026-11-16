package net.zyonmc.zyonhelp.manager;

import net.zyonmc.zyonhelp.ZyonHelp;
import net.zyonmc.zyonhelp.model.Ticket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TicketManager {

    // Key: Player UUID -> Value: Ticket instance
    private final Map<UUID, Ticket> activeTickets;
    
    // Key: Discriminator (ChannelID) -> Value: Player UUID
    private final Map<Long, UUID> channelToPlayer;

    private final DatabaseManager databaseManager;

    public TicketManager(DatabaseManager databaseManager) {
        this.activeTickets = new HashMap<>();
        this.channelToPlayer = new HashMap<>();
        this.databaseManager = databaseManager;

        loadTicketsFromDatabase();
    }

    public void checkInactivity(ZyonHelp plugin) {
        if (!plugin.getConfigManager().isInactivityEnabled()) return;

        long waitTimeMillis = (long) plugin.getConfigManager().getInactivityWaitTime() * 60 * 1000;
        long currentTime = System.currentTimeMillis();
        MiniMessage mm = MiniMessage.miniMessage();

        List<UUID> toRemove = activeTickets.values().stream()
                .filter(ticket -> (currentTime - ticket.getLastActivity()) > waitTimeMillis)
                .map(Ticket::getPlayerUuid)
                .toList();

        for (UUID uuid : toRemove) {
            Ticket ticket = activeTickets.get(uuid);
            if (ticket == null) continue;

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + plugin.getConfigManager().getInactivityMessage()));
            }

            plugin.getDiscordManager().closeTicketChannel(ticket.getChannelId(), ticket.getPlayerName());
            removeTicket(uuid);
            plugin.getLogger().info("Ticket de " + ticket.getPlayerName() + " cerrado por inactividad.");
        }
    }

    private void loadTicketsFromDatabase() {
        if (databaseManager == null) return;
        List<Ticket> loadedTickets = databaseManager.loadAllTickets();
        for (Ticket ticket : loadedTickets) {
            activeTickets.put(ticket.getPlayerUuid(), ticket);
            channelToPlayer.put(ticket.getChannelId(), ticket.getPlayerUuid());
        }
    }

    public void addTicket(UUID playerUuid, String playerName, long channelId) {
        Ticket ticket = new Ticket(playerUuid, playerName, channelId);
        activeTickets.put(playerUuid, ticket);
        channelToPlayer.put(channelId, playerUuid);

        // Persistent storage
        if (databaseManager != null) {
            databaseManager.insertTicket(ticket);
        }
    }

    public void removeTicket(UUID playerUuid) {
        Ticket ticket = activeTickets.remove(playerUuid);
        if (ticket != null) {
            channelToPlayer.remove(ticket.getChannelId());
            if (databaseManager != null) {
                databaseManager.deleteTicket(playerUuid);
            }
        }
    }

    public void removeTicketByChannel(long channelId) {
        UUID playerUuid = channelToPlayer.get(channelId);
        if (playerUuid != null) {
            removeTicket(playerUuid);
        }
    }

    public boolean hasTicket(UUID playerUuid) {
        return activeTickets.containsKey(playerUuid);
    }

    public Ticket getTicket(UUID playerUuid) {
        return activeTickets.get(playerUuid);
    }

    public Ticket getTicketByName(String name) {
        for (Ticket ticket : activeTickets.values()) {
            if (ticket.getPlayerName().equalsIgnoreCase(name)) {
                return ticket;
            }
        }
        return null; // Not found
    }

    public UUID getPlayerUuid(long channelId) {
        return channelToPlayer.get(channelId);
    }
}

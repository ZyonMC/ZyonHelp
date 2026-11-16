package net.zyonmc.zyonhelp.model;

import java.util.UUID;

public class Ticket {

    private final UUID playerUuid;
    private final String playerName;
    private final long channelId;
    private long lastActivity;

    public Ticket(UUID playerUuid, String playerName, long channelId) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.channelId = channelId;
        this.lastActivity = System.currentTimeMillis();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }
}

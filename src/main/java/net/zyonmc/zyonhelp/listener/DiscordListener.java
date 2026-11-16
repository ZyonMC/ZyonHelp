package net.zyonmc.zyonhelp.listener;

import net.zyonmc.zyonhelp.ZyonHelp;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.zyonmc.zyonhelp.model.Ticket;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final ZyonHelp plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DiscordListener(ZyonHelp plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getChannelType() != ChannelType.TEXT) return;

        long channelId = event.getChannel().getIdLong();
        UUID playerUuid = plugin.getTicketManager().getPlayerUuid(channelId);

        // If this channel is not mapped to a player ticket, ignore
        if (playerUuid == null) return;

        Ticket ticket = plugin.getTicketManager().getTicket(playerUuid);
        if (ticket != null) {
            ticket.updateLastActivity();
        }

        String message = event.getMessage().getContentDisplay();
        User author = event.getAuthor();
        String staffName = author.getEffectiveName();

        // Format message to send to Minecraft
        String formattedMessage = plugin.getConfigManager().getMessagePrefix() + plugin.getConfigManager().getFormatDiscordStaff()
                .replace("{author}", staffName)
                .replace("{message}", message);

        Player targetPlayer = Bukkit.getPlayer(playerUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(mm.deserialize(formattedMessage));
        }

        // Also send to all online staff
        String staffPermission = plugin.getConfigManager().getStaffPermission();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(staffPermission)) {
                if (targetPlayer == null || !onlinePlayer.getUniqueId().equals(targetPlayer.getUniqueId())) {
                    onlinePlayer.sendMessage(mm.deserialize(formattedMessage));
                }
            }
        }
    }
}

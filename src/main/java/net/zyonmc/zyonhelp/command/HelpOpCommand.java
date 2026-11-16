package net.zyonmc.zyonhelp.command;

import net.zyonmc.zyonhelp.ZyonHelp;
import net.zyonmc.zyonhelp.model.Ticket;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import org.bukkit.command.TabCompleter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;

public class HelpOpCommand implements CommandExecutor, TabCompleter {

    private final ZyonHelp plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public HelpOpCommand(ZyonHelp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zyonhelp.helpop")) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>No tienes permiso para usar este comando."));
            return true;
        }
    
        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>Uso: /helpop <mensaje> | /helpop reply <jugador> <mensaje> | /helpop close <jugador>"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reply")) {
            return handleReply(sender, args);
        } else if (subCommand.equals("close")) {
            return handleClose(sender, args);
        } else {
            return handleHelpOp(sender, args);
        }
    }

    private boolean handleHelpOp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();
        String message = String.join(" ", args);

        // Send to online staff
        String staffPermission = plugin.getConfigManager().getStaffPermission();
        String prefix = plugin.getConfigManager().getMessagePrefix();
        String staffFormat = prefix + plugin.getConfigManager().getFormatHelpopToStaff()
                .replace("{player}", player.getName())
                .replace("{message}", message);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(staffPermission)) {
                onlinePlayer.sendMessage(mm.deserialize(staffFormat));
            }
        }

        // Create discord channel if ticket doesn't exist
        if (!plugin.getTicketManager().hasTicket(playerUuid)) {
            plugin.getDiscordManager().createTicketChannel(playerUuid, player.getName(), message);
        } else {
            // Channel already exists, just send message
            Ticket ticket = plugin.getTicketManager().getTicket(playerUuid);
            ticket.updateLastActivity();
            long channelId = ticket.getChannelId();
            
            // Re-using format to discord for subsequent messages
            String formattedMessage = plugin.getConfigManager().getFormatToDiscord()
                    .replace("{player}", player.getName())
                    .replace("{message}", message);
            
            if(plugin.getDiscordManager().getJda() != null) {
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = plugin.getDiscordManager().getJda().getTextChannelById(channelId);
                if (channel != null) {
                    channel.sendMessage(formattedMessage).queue();
                }
            }
        }

        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!sender.hasPermission(plugin.getConfigManager().getStaffPermission())) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>No tienes permiso."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>Uso: /helpop reply <jugador> <mensaje>"));
            return true;
        }

        String targetName = args[1];
        Ticket ticket = plugin.getTicketManager().getTicketByName(targetName);

        if (ticket == null) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>Este jugador no tiene un ticket activo."));
            return true;
        }

        ticket.updateLastActivity();
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String staffName = sender.getName();

        // Send to player in MC if online
        Player targetPlayer = Bukkit.getPlayer(ticket.getPlayerUuid());
        String prefix = plugin.getConfigManager().getMessagePrefix();
        String formattedToPlayer = prefix + plugin.getConfigManager().getFormatMcStaff()
                .replace("{author}", staffName)
                .replace("{message}", message);
                
        if (targetPlayer != null && targetPlayer.isOnline()) {
             targetPlayer.sendMessage(mm.deserialize(formattedToPlayer));
        } else {
             sender.sendMessage(mm.deserialize(prefix + "<yellow>El jugador no está en línea en Minecraft, pero el mensaje fue enviado a Discord."));
        }

        // Send to all staff in MC
        String staffPermission = plugin.getConfigManager().getStaffPermission();
        String formattedToStaff = prefix + plugin.getConfigManager().getFormatStaffReplyToStaff()
                .replace("{player}", ticket.getPlayerName())
                .replace("{author}", staffName)
                .replace("{message}", message);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(staffPermission)) {
                onlinePlayer.sendMessage(mm.deserialize(formattedToStaff));
            }
        }


        // Send to Discord
        plugin.getDiscordManager().sendReplyToDiscord(ticket.getChannelId(), staffName, message);

        return true;
    }

    private boolean handleClose(CommandSender sender, String[] args) {
        if (!sender.hasPermission(plugin.getConfigManager().getStaffPermission())) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>No tienes permiso."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>Uso: /helpop close <jugador>"));
            return true;
        }

        String targetName = args[1];
        Ticket ticket = plugin.getTicketManager().getTicketByName(targetName);

        if (ticket == null) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>Ese jugador no tiene un ticket abierto."));
            return true;
        }

        // Send close message if online
        Player targetPlayer = Bukkit.getPlayer(ticket.getPlayerUuid());
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + plugin.getConfigManager().getMessageTicketClosed()));
        }

        sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<green>Has cerrado el ticket de " + ticket.getPlayerName() + "."));

        // Delete from discord (And save transcript)
        plugin.getDiscordManager().closeTicketChannel(ticket.getChannelId(), ticket.getPlayerName());

        // Remove from map & DB
        plugin.getTicketManager().removeTicket(ticket.getPlayerUuid());

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("zyonhelp.helpop")) {
            return completions;
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            // Staff complete subcommands
            if (sender.hasPermission(plugin.getConfigManager().getStaffPermission())) {
                List<String> subCommands = Arrays.asList("reply", "close");
                for (String sub : subCommands) {
                    if (sub.startsWith(partial)) {
                        completions.add(sub);
                    }
                }
            }
        } else if (args.length == 2 && sender.hasPermission(plugin.getConfigManager().getStaffPermission())) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("reply") || subCommand.equals("close")) {
                String partialPlayer = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partialPlayer)) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}

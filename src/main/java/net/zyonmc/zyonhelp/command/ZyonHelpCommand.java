package net.zyonmc.zyonhelp.command;

import net.zyonmc.zyonhelp.ZyonHelp;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import org.bukkit.command.TabExecutor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZyonHelpCommand implements TabExecutor {

    private final ZyonHelp plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ZyonHelpCommand(ZyonHelp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>Uso: /zyonhelp <help|reload>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("zyonhelp.admin")) {
                sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>No tienes permiso para usar este comando."));
                return true;
            }
            plugin.getConfigManager().reloadConfig();
            plugin.getDiscordManager().init();
            sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<green>Configuración recargada correctamente."));
            return true;
        } else if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(mm.deserialize("<white><dark_gray><st>----------------------------------------<white>"));
            sender.sendMessage(mm.deserialize("<white><cyan><bold>ZyonHelp <gray>- v" + plugin.getDescription().getVersion() + "<white>"));
            sender.sendMessage(mm.deserialize("<white><gray>Desarrollado por: <white>Sebastian Cheikh<white>"));
            sender.sendMessage(mm.deserialize(""));
            sender.sendMessage(mm.deserialize("<white><yellow>Comandos:<white>"));
            sender.sendMessage(mm.deserialize("<white> <white>/helpop <mensaje> <gray>- Solicita ayuda al staff.<white>"));
            sender.sendMessage(mm.deserialize("<white> <white>/helpop reply <jugador> <mensaje> <gray>- Responde a un ticket.<white>"));
            sender.sendMessage(mm.deserialize("<white> <white>/helpop close <jugador> <gray>- Cierra un ticket.<white>"));
            sender.sendMessage(mm.deserialize("<white> <white>/zyonhelp help <gray>- Muestra este menú de ayuda.<white>"));
            sender.sendMessage(mm.deserialize("<white> <white>/zyonhelp reload <gray>- Recarga la configuración.<white>"));
            sender.sendMessage(mm.deserialize("<white><dark_gray><st>----------------------------------------<white>"));
            return true;
        }

        sender.sendMessage(mm.deserialize(plugin.getConfigManager().getMessagePrefix() + "<red>Uso: /zyonhelp <help|reload>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> subCommands = new ArrayList<>();
            subCommands.add("help");
            if (sender.hasPermission("zyonhelp.admin")) {
                subCommands.add("reload");
            }
            
            for (String sub : subCommands) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        }
        
        return completions;
    }
}

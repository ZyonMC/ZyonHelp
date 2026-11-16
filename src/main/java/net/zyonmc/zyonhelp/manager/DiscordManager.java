package net.zyonmc.zyonhelp.manager;

import net.zyonmc.zyonhelp.ZyonHelp;
import net.zyonmc.zyonhelp.listener.DiscordListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.UUID;
import java.util.function.Consumer;

public class DiscordManager {

    private final ZyonHelp plugin;
    private JDA jda;

    public DiscordManager(ZyonHelp plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        shutdown(); // Cerrar sesión previa si existe
        if (!plugin.getConfigManager().isDiscordEnabled()) {
            plugin.getLogger().info("ZyonHelp: Discord está desactivado en la configuración.");
            return false;
        }

        String token = plugin.getConfigManager().getDiscordToken();
        if (token == null || token.isEmpty() || token.equals("your-token-here") || token.equals("TU_TOKEN_AQUI")) {
            plugin.getLogger().warning("El token de Discord no es válido o no ha sido configurado. Las funciones de Discord estarán desactivadas.");
            return false;
        }

        try {
            plugin.getLogger().info("ZyonHelp: Conectando con Discord...");
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordListener(plugin))
                    .build();
            jda.awaitReady();
            plugin.getLogger().info("ZyonHelp: ¡Bot de Discord conectado y listo!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error crítico al conectar con Discord: " + e.getMessage());
            return false;
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    public JDA getJda() {
        return jda;
    }

    public Guild getGuild() {
        if (jda == null) return null;
        String guildId = plugin.getConfigManager().getGuildId();
        return jda.getGuildById(guildId);
    }

    public Category getCategory() {
        Guild guild = getGuild();
        if (guild == null) return null;
        String categoryId = plugin.getConfigManager().getCategoryId();
        return guild.getCategoryById(categoryId);
    }

    public void createTicketChannel(UUID playerUuid, String playerName, String initialMessage) {
        if (jda == null) return;

        Category category = getCategory();
        if (category == null) {
            plugin.getLogger().warning("Categoria de Discord no encontrada.");
            return;
        }

        String channelName = "zyon-ticket-" + playerName.toLowerCase();
        String staffRoleId = plugin.getConfigManager().getStaffRoleId();

        net.dv8tion.jda.api.requests.restaction.ChannelAction<net.dv8tion.jda.api.entities.channel.concrete.TextChannel> action = category.createTextChannel(channelName);
        
        // Deny @everyone
        action = action.addRolePermissionOverride(getGuild().getPublicRole().getIdLong(), null, java.util.EnumSet.of(net.dv8tion.jda.api.Permission.VIEW_CHANNEL));
        
        // Allow Staff Role
        if (staffRoleId != null && !staffRoleId.isEmpty() && !staffRoleId.equals("your-role-id")) {
            try {
                long roleId = Long.parseLong(staffRoleId);
                action = action.addRolePermissionOverride(roleId, 
                    java.util.EnumSet.of(net.dv8tion.jda.api.Permission.VIEW_CHANNEL, net.dv8tion.jda.api.Permission.MESSAGE_SEND, net.dv8tion.jda.api.Permission.MESSAGE_HISTORY, net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS, net.dv8tion.jda.api.Permission.MESSAGE_ATTACH_FILES), 
                    null);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid staff-role-id in config: " + staffRoleId);
            }
        }

        action.queue(channel -> {
            // Save inside TicketManager
            plugin.getTicketManager().addTicket(playerUuid, playerName, channel.getIdLong());

            // Send initial message
            String formattedMessage = plugin.getConfigManager().getFormatToDiscord()
                    .replace("{player}", playerName)
                    .replace("{message}", initialMessage);
            
            channel.sendMessage(formattedMessage).queue();
            plugin.getLogger().info("Canal de Discord creado: " + channel.getName());
        }, error -> {
            plugin.getLogger().severe("No se pudo crear el canal de Discord: " + error.getMessage());
        });
    }

    public void sendReplyToDiscord(long channelId, String staffName, String message) {
        if (jda == null) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String formattedMessage = plugin.getConfigManager().getFormatReplyToDiscord()
                    .replace("{staff}", staffName)
                    .replace("{message}", message);
            channel.sendMessage(formattedMessage).queue();
        }
    }

    public void closeTicketChannel(long channelId, String playerName) {
        if (jda == null) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            
            // Save transcript if enabled
            if (plugin.getConfigManager().isTranscriptsEnabled() && plugin.getConfigManager().isTranscriptsSaveLocal()) {
                channel.getIterableHistory().takeAsync(100).thenAccept(messages -> {
                    StringBuilder transcript = new StringBuilder();
                    transcript.append("=== Transcript for Ticket: ").append(playerName).append(" ===\n\n");
                    
                    // Messages are retrieved from newest to oldest, so we reverse it
                    java.util.Collections.reverse(messages);
                    
                    for (net.dv8tion.jda.api.entities.Message msg : messages) {
                        transcript.append("[").append(msg.getTimeCreated().toLocalDateTime().toString()).append("] ");
                        transcript.append(msg.getAuthor().getEffectiveName()).append(": ");
                        transcript.append(msg.getContentDisplay()).append("\n");
                        if (!msg.getAttachments().isEmpty()) {
                            for (net.dv8tion.jda.api.entities.Message.Attachment attachment : msg.getAttachments()) {
                                transcript.append(" [Attachment: ").append(attachment.getUrl()).append("]\n");
                            }
                        }
                    }

                    saveTranscriptLocally(playerName, transcript.toString());

                    // Delete channel after transcript is saved
                    if (plugin.getConfigManager().isDeleteOnClose()) {
                        channel.delete().queue(
                                success -> plugin.getLogger().info("Canal de Discord eliminado y transcrito (ticket cerrado)."),
                                error -> plugin.getLogger().warning("No se pudo eliminar el canal de Discord: " + error.getMessage())
                        );
                    }
                }).exceptionally(e -> {
                    plugin.getLogger().severe("Error generando transcripción: " + e.getMessage());
                    // Fallback to delete channel if config says so, even if transcript failed
                    if (plugin.getConfigManager().isDeleteOnClose()) {
                        channel.delete().queue();
                    }
                    return null;
                });
            } else {
                // Just delete without transcript
                if (plugin.getConfigManager().isDeleteOnClose()) {
                    channel.delete().queue(
                            success -> plugin.getLogger().info("Canal de Discord eliminado (ticket cerrado)."),
                            error -> plugin.getLogger().warning("No se pudo eliminar el canal de Discord: " + error.getMessage())
                    );
                }
            }
        }
    }

    private void saveTranscriptLocally(String playerName, String content) {
        java.io.File dir = new java.io.File(plugin.getDataFolder(), "transcripts");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = playerName + "-" + System.currentTimeMillis() + ".txt";
        java.io.File file = new java.io.File(dir, fileName);

        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(content);
            plugin.getLogger().info("Transcripción guardada en: transcripts/" + fileName);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Error guardando archivo de transcripción: " + e.getMessage());
        }
    }
}

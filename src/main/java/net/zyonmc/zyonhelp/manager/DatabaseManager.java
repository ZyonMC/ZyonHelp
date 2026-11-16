package net.zyonmc.zyonhelp.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.zyonmc.zyonhelp.ZyonHelp;
import net.zyonmc.zyonhelp.model.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final ZyonHelp plugin;
    private HikariDataSource dataSource;
    private final boolean enabled;

    public DatabaseManager(ZyonHelp plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigManager().isMysqlEnabled();
    }

    public boolean init() {
        if (!enabled) {
            plugin.getLogger().info("ZyonHelp MySQL está deshabilitado. Se usarán datos volátiles en memoria.");
            return true;
        }

        try {
            HikariConfig config = new HikariConfig();
            
            String host = plugin.getConfigManager().getMysqlHost();
            int port = plugin.getConfigManager().getMysqlPort();
            String database = plugin.getConfigManager().getMysqlDatabase();
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            config.setUsername(plugin.getConfigManager().getMysqlUsername());
            config.setPassword(plugin.getConfigManager().getMysqlPassword());
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);

            createTable();
            plugin.getLogger().info("Conectado a MySQL exitosamente.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error conectando a MySQL: " + e.getMessage());
            return false;
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void createTable() {
        if (!enabled) return;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS zyonhelp_tickets (" +
                             "uuid VARCHAR(36) PRIMARY KEY, " +
                             "player_name VARCHAR(16) NOT NULL, " +
                             "channel_id BIGINT NOT NULL" +
                             ");"
             )) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("No se pudo crear la tabla MySQL: " + e.getMessage());
        }
    }

    public void insertTicket(Ticket ticket) {
        if (!enabled) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO zyonhelp_tickets (uuid, player_name, channel_id) VALUES (?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE channel_id = ?;"
                 )) {
                stmt.setString(1, ticket.getPlayerUuid().toString());
                stmt.setString(2, ticket.getPlayerName());
                stmt.setLong(3, ticket.getChannelId());
                stmt.setLong(4, ticket.getChannelId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error guardando ticket en MySQL: " + e.getMessage());
            }
        });
    }

    public void deleteTicket(UUID playerUuid) {
        if (!enabled) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM zyonhelp_tickets WHERE uuid = ?;"
                 )) {
                stmt.setString(1, playerUuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error eliminando ticket de MySQL: " + e.getMessage());
            }
        });
    }

    public List<Ticket> loadAllTickets() {
        List<Ticket> tickets = new ArrayList<>();
        if (!enabled) return tickets;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM zyonhelp_tickets;");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String playerName = rs.getString("player_name");
                long channelId = rs.getLong("channel_id");
                
                tickets.add(new Ticket(uuid, playerName, channelId));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando tickets de MySQL: " + e.getMessage());
        }
        return tickets;
    }
}

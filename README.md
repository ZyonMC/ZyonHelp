# ZyonHelp

Paper plugin for **hybrid help tickets**: a player runs `/helpop`, a Discord channel is created for the ticket, and staff
can answer from Discord or from inside Minecraft. Messages are relayed both ways.

## Features

- `/helpop <message>` opens a ticket and a private Discord channel
- Staff reply from Discord or from Minecraft, with separate chat formats
- Optional MySQL/MariaDB storage so tickets survive restarts
- Transcripts saved on close (local `transcripts/` folder)
- Auto-close after inactivity (default 5 minutes)

## Requirements

- Paper 1.21, Java 21
- A Discord bot (token, guild, ticket category and staff role)

## Commands and permissions

| Command | Permission | Description |
|---|---|---|
| `/helpop <message>` | `zyonhelp.helpop` | Ask the staff for help |
| `/zyonhelp reload` | `zyonhelp.admin` | Reload the configuration |

Staff see and answer tickets in-game with `zyonhelp.staff` (configurable in `options.staff-permission`).

## Configuration

`plugins/ZyonHelp/config.yml`:

```yaml
discord-bot:
  enabled: true
  token: "your-token-here"
  guild-id: "your-guild-id-here"
  category-id: "your-category-id-here"
  staff-role-id: "your-role-id-here"

mysql:
  enabled: false
  host: "localhost"
  port: 3306
  database: "zyonhelp"
  username: "root"
  password: ""
```

Never commit your real bot token. Message formats use MiniMessage.

## Build

```bash
./gradlew shadowJar   # JDA and HikariCP are shaded; jar in build/libs/
./gradlew runServer   # test server
```

Package: `net.zyonmc.zyonhelp`

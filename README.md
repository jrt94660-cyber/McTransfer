# McTransfer 1.1.0

Paper plugin for direct native player transfers without BungeeCord/Velocity, plus PlaceholderAPI status placeholders for FancyHolograms and FancyNpcs.

## Requirements
- Paper-compatible server and Java 21.
- PlaceholderAPI for display placeholders.
- FancyHolograms for hologram rendering.
- FancyNpcs for clickable NPC interaction.
- Geyser-Spigot on the source server for Bedrock-player detection.

## Transfer
Install McTransfer on the lobby/source server. Configure the destination Java host/port and Bedrock Geyser host/UDP port in `plugins/McTransfer/config.yml`. Players use `/transfer flovexsmp`. The destination Java server must accept native transfers (where supported), and the address must be reachable from player devices.

## FancyHolograms placeholders
- `%mctransfer_flovexsmp_name%` — display name
- `%mctransfer_flovexsmp_online%` — online player count
- `%mctransfer_flovexsmp_max%` — maximum player count
- `%mctransfer_flovexsmp_players%` — online/max or OFFLINE
- `%mctransfer_flovexsmp_status%` — ONLINE/OFFLINE
- `%mctransfer_flovexsmp_click%` — configured click text
- `%mctransfer_flovexsmp_motd_line1%` and `%mctransfer_flovexsmp_motd_line2%` — server-specific MOTD lines
- `%mctransfer_motd_line1%`, `%mctransfer_motd_line2%`, `%mctransfer_motd_icon%` — global MOTD display values

Example FancyHolograms text:
```
%mctransfer_flovexsmp_motd_line1%
%mctransfer_flovexsmp_motd_line2%
<gray>Players: <white>%mctransfer_flovexsmp_players%
%mctransfer_flovexsmp_click%
```

Status is checked against the configured Java status endpoint every 30 seconds (configurable). The endpoint must permit server-list status pings. A failed ping displays OFFLINE and 0 players.

## FancyNpcs click-to-join
FancyHolograms text is not clickable by itself. Place a FancyNpcs INTERACTION NPC over the hologram and add a player command action:

```
/npc type flovex_join INTERACTION
/npc action flovex_join RIGHT_CLICK add player_command transfer flovexsmp
```

Adjust the interaction NPC hitbox to cover the hologram. This deliberately uses FancyNpcs' player_command action to invoke McTransfer's direct transfer command. Do not use FancyNpcs' send_to_server action, which relies on proxy messaging.

## MOTD note
The two configured lines are an MOTD-style hologram display. They do not replace the actual multiplayer server-list MOTD of the lobby or destination. The icon setting is retained as metadata; it does not render a server-list icon in FancyHolograms.

## Commands
- `/transfer <server>`
- `/transfer reload` (permission `mctransfer.admin`)

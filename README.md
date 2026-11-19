# McTransfer 1.1.8

Paper plugin for direct native transfers (no proxy required) with a PlaceholderAPI online-player-count placeholder.

## Requirements
- Paper-compatible server, Java 21.
- PlaceholderAPI for online-player placeholders.
- Geyser-Spigot on the source server for Bedrock player detection.

## Transfers
Use `/transfer <server-key>`. The default destination key is `economy`: `/transfer economy`.

## PlaceholderAPI
Format: `%mctransfer_<server-key>_players%`
For the default destination: `%mctransfer_economy_players%` returns only the online count, or `0` if status ping is unavailable. Compatible with MiniMOTD, FancyHolograms, FancyNpcs, and any PAPI-compatible plugin.

## Servers
Up to 5 destination entries under `servers:`. Default economy endpoint template: `play.yourserver.net:19132` (change this to your server's address and port). Status updates every 30 seconds by default (`status.refresh-seconds`).

## Commands
- `/transfer <server-key>`
- `/transfer reload` (permission `mctransfer.admin`)


## Server Selector GUI
Players receive a glowing compass in the center hotbar slot (slot 5). Right-click it while aiming at a block within 100 blocks to teleport to that block's top, if a safe landing spot exists. Uses ray-tracing from the player's eyes to identify the block under the crosshair, then checks up to four blocks above it for a safe landing (two clear blocks plus solid floor). The compass no longer opens the server selector GUI.

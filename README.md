# McTransfer 1.1.1

Paper plugin for direct native transfers (no proxy required) with a simple PlaceholderAPI online-player-count placeholder.

## Requirements
- Paper-compatible server, Java 21.
- PlaceholderAPI for the player count placeholder.
- Geyser-Spigot on the source server for Bedrock player detection.

## Transfers
Use `/transfer <server-key>`. Destination keys and Java/Bedrock host/ports are configured in `plugins/McTransfer/config.yml`.

## PlaceholderAPI (players only)
Placeholder format: `%mctransfer_<server-key>_players%`

For the configured `flovexsmp` entry:
`%mctransfer_flovexsmp_players%`

This returns only the online count (example `24`). If status ping fails, it returns `0`. It can be used with MiniMOTD, FancyHolograms, FancyNpcs, or any plugin that supports PlaceholderAPI placeholders.

## Server slots
Configure up to five destination entries total under `servers:`. The existing `flovexsmp` entry is retained as the same key so current transfer command/tab completion references continue working; its host is now `manila.playwithbao.com` and port remains `33984`. Add up to four more server entries using the commented examples.

Status is polled every 30 seconds by default; change `status.refresh-seconds` to adjust.

## Commands
- `/transfer <server-key>`
- `/transfer reload` (permission `mctransfer.admin`)

Build version: 1.1.2.

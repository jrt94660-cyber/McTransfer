# McTransfer

A lightweight Paper plugin that requests direct player transfers between independently hosted Minecraft servers using Minecraft's native transfer packet. It does not use BungeeCord or Velocity.

## Requirements

- Paper (or a compatible Paper fork) with the native player transfer API.
- Java clients: supported by Paper's native transfer packet.
- Bedrock clients: Geyser-Spigot must be installed on the source server. Each Bedrock destination must expose its own reachable Geyser address and port.
- No proxy is required.

## Install

1. Build the project with Java 21 and Maven, or download the JAR from the GitHub Actions artifact.
2. Put `McTransfer-1.0.1.jar` in the source server's `plugins` folder.
3. Start the server once to generate `plugins/McTransfer/config.yml`.
4. Edit the `servers` section to add destination servers.
5. On every Java destination server, enable incoming transfers with `accept-transfers=true` in `server.properties` (if supported by that server version) and restart it.

## Commands

- `/transfer <server>` — request a transfer to a configured destination.
- `/transfer reload` — reload the configuration (permission: `mctransfer.admin`).

## Default destination

The default command is:

```text
/transfer flovexsmp
```

It uses this destination configuration:

```yaml
servers:
  flovexsmp:
    display-name: "FlovexSMP"
    java:
      host: "smpflovex.playwithbao.com"
      port: 33984
    bedrock:
      host: "smpflovex.playwithbao.com"
      port: 33984
```

The Java host/port must be the destination server's public Java endpoint. The Bedrock host/port must be the destination Geyser endpoint (often a different UDP port). Confirm these values with your host; do not assume the Java and Bedrock ports are the same.

## Important notes

- Install this plugin on the server players are transferring **from**. The destination does not need McTransfer just to receive a native transfer, but it must accept native transfers.
- The target address must be directly reachable from each player's device. A plugin cannot make private, firewalled, or unallocated server ports reachable.
- For Bedrock, Geyser must be running and listening on the configured Bedrock endpoint at the destination.
- This is a connection transfer, not shared inventory/world synchronization. Each server remains independent.
- The plugin does not transfer players automatically on join; players must run `/transfer flovexsmp`.
- Direct arbitrary host transfers are disabled by default. Prefer named destinations so Java and Bedrock players get the correct endpoint.

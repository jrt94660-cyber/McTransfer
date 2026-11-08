# McTransfer

A lightweight Paper plugin that transfers players directly between independently hosted Minecraft servers using the native Minecraft transfer packet. It does not use BungeeCord or Velocity.

## Requirements

- Paper (or a compatible Paper fork) with the native player transfer API.
- Java clients: supported by Paper's native transfer packet.
- Bedrock clients: Geyser-Spigot must be installed on the source server. Each Bedrock destination must expose its own reachable Geyser address and port.
- No proxy is required.

## Install

1. Build the project with Java 21 and Maven, or download the JAR from the GitHub Actions artifact.
2. Put `McTransfer-1.0.0.jar` in the source server's `plugins` folder.
3. Start the server once to generate `plugins/McTransfer/config.yml`.
4. Edit the `servers` section to add destination servers.
5. On every Java destination server, set `accept-transfers=true` in `server.properties` and restart it.

## Commands

- `/transfer <server>` — transfer to a configured destination.
- `/transfer reload` — reload the configuration (permission: `mctransfer.admin`).

## Configuration example

```yaml
servers:
  smpflovex:
    display-name: "FlovexSMP"
    java:
      host: "smpflovex.playwithbao.com"
      port: 33984
    bedrock:
      host: "smpflovex.playwithbao.com"
      port: 33984
```

Use the actual public Geyser endpoint in the `bedrock` section. The Bedrock port is often different from the Java port. If the same address is used for both editions, the values can be identical.

Add more named destinations by copying the `smpflovex` block and changing its name, host, and ports. Players use the key as the command argument, for example `/transfer survival`.

## Important notes

- Install this plugin on the server players are transferring **from**. The destination does not need McTransfer just to receive a native transfer, but its server software must accept transfers.
- The target address must be directly reachable from each player's device. A plugin cannot make private, firewalled, or unallocated server ports reachable.
- For Bedrock, Geyser must be running and listening on the configured Bedrock endpoint at the destination. McTransfer detects Geyser players and sends them to the Bedrock address/port instead of the Java endpoint.
- This is a connection transfer, not shared inventory/world synchronization. Each server remains independent.
- Direct arbitrary host transfers are disabled by default. Prefer named destinations so Java and Bedrock players get the correct endpoint.

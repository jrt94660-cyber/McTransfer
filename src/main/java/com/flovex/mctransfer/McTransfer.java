package com.flovex.mctransfer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class McTransfer extends JavaPlugin implements TabExecutor {
    private final Map<String, ServerStatus> statuses = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateLegacyDestination();
        var command = getCommand("transfer");
        if (command == null) {
            getLogger().severe("The transfer command is missing from plugin.yml; disabling McTransfer.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new McTransferExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            getLogger().info("PlaceholderAPI not found; server display placeholders are disabled.");
        }
        startStatusPolling();
        getLogger().info("McTransfer enabled. Native transfers do not require a proxy.");
    }

    @Override
    public void onDisable() {
        if (getServer().getScheduler() != null) {
            getServer().getScheduler().cancelTasks(this);
        }
    }

    private void migrateLegacyDestination() {
        var legacy = getConfig().getConfigurationSection("servers.smpflovex");
        if (legacy == null || getConfig().isConfigurationSection("servers.flovexsmp")) return;
        Map<String, Object> values = legacy.getValues(true);
        getConfig().createSection("servers.flovexsmp", values);
        getConfig().set("servers.smpflovex", null);
        saveConfig();
        getLogger().info("Migrated destination key 'smpflovex' to 'flovexsmp'.");
    }

    private void startStatusPolling() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            var section = getConfig().getConfigurationSection("servers");
            if (section == null) return;
            for (String key : section.getKeys(false)) {
                String path = "servers." + key + ".java.";
                String host = getConfig().getString(path + "host", "");
                int port = getConfig().getInt(path + "port", -1);
                if (host == null || host.isBlank() || port < 1 || port > 65535) {
                    statuses.put(key.toLowerCase(Locale.ROOT), ServerStatus.offline());
                    continue;
                }
                statuses.put(key.toLowerCase(Locale.ROOT), ping(host, port));
            }
        }, 20L, Math.max(20L, getConfig().getLong("status.refresh-seconds", 30L) * 20L));
    }

    private ServerStatus ping(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1800);
            socket.setSoTimeout(1800);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshakeBytes);
            writeVarInt(handshake, 0);
            writeVarInt(handshake, -1);
            writeString(handshake, host);
            handshake.writeShort(port);
            writeVarInt(handshake, 1);
            writePacket(out, handshakeBytes.toByteArray());

            ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
            DataOutputStream request = new DataOutputStream(requestBytes);
            writeVarInt(request, 0);
            writePacket(out, requestBytes.toByteArray());

            readVarInt(in); // packet length
            int packetId = readVarInt(in);
            if (packetId != 0) return ServerStatus.offline();
            int jsonLength = readVarInt(in);
            if (jsonLength < 1 || jsonLength > 1_000_000) return ServerStatus.offline();
            byte[] jsonBytes = new byte[jsonLength];
            in.readFully(jsonBytes);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            int online = jsonInt(json, "online");
            int max = jsonInt(json, "max");
            if (online < 0 || max < 0) return ServerStatus.offline();
            return new ServerStatus(true, online, max);
        } catch (Exception ignored) {
            return ServerStatus.offline();
        }
    }

    private int jsonInt(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private void writePacket(DataOutputStream out, byte[] payload) throws IOException {
        writeVarInt(out, payload.length);
        out.write(payload);
        out.flush();
    }

    private void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte current;
        do {
            current = in.readByte();
            value |= (current & 127) << position++ * 7;
            if (position > 5) throw new IOException("VarInt is too big");
        } while ((current & 128) == 128);
        return value;
    }

    ServerStatus getStatus(String server) {
        return statuses.getOrDefault(server.toLowerCase(Locale.ROOT), ServerStatus.offline());
    }

    String getServerDisplayName(String server) {
        return getConfig().getString("servers." + server + ".display-name", server);
    }

    String getMotdLine(String line) {
        return getConfig().getString("motd." + line, "");
    }

    String getMotdIcon() {
        return getConfig().getString("motd.icon", "random");
    }

    record ServerStatus(boolean online, int players, int maxPlayers) {
        static ServerStatus offline() { return new ServerStatus(false, 0, 0); }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mctransfer.admin")) {
                send(sender, "no-permission");
                return true;
            }
            reloadConfig();
            migrateLegacyDestination();
            send(sender, "reloaded");
            return true;
        }
        if (!(sender instanceof Player player)) {
            send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("mctransfer.use")) {
            send(player, "no-permission");
            return true;
        }
        if (args.length != 1) {
            send(player, "usage");
            return true;
        }

        String requested = args[0].toLowerCase(Locale.ROOT);
        String base = "servers." + requested;
        if (!getConfig().isConfigurationSection(base)) {
            if (getConfig().getBoolean("allow-direct-java-addresses", false) && !isBedrockPlayer(player)) {
                return transfer(player, args[0], args[0], 25565);
            }
            send(player, "unknown-server", "%server%", args[0]);
            return true;
        }

        boolean bedrock = isBedrockPlayer(player);
        String editionPath = base + (bedrock ? ".bedrock" : ".java");
        String host = getConfig().getString(editionPath + ".host", "").trim();
        int port = getConfig().getInt(editionPath + ".port", -1);
        String displayName = getConfig().getString(base + ".display-name", requested);
        if (host.isEmpty() || port < 1 || port > 65535) {
            getLogger().warning("Invalid " + (bedrock ? "Bedrock" : "Java") + " destination: " + requested);
            send(player, "transfer-failed");
            return true;
        }
        getLogger().info("Transfer request: " + player.getName() + " -> " + displayName + " (" + host + ":" + port + ")");
        return transfer(player, displayName, host, port);
    }

    private boolean transfer(Player player, String displayName, String host, int port) {
        send(player, "transferring", "%server%", displayName);
        try {
            player.transfer(host, port);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            getLogger().warning("Transfer failed for " + player.getName() + ": " + exception.getMessage());
            send(player, "transfer-failed");
        } catch (RuntimeException exception) {
            getLogger().warning("Unexpected transfer error for " + player.getName() + ": " + exception.getMessage());
            send(player, "transfer-failed");
        }
        return true;
    }

    private boolean isBedrockPlayer(Player player) {
        if (getServer().getPluginManager().getPlugin("Geyser-Spigot") == null
                && getServer().getPluginManager().getPlugin("Geyser") == null) return false;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object api = apiClass.getMethod("api").invoke(null);
            if (api == null) return false;
            Method check = apiClass.getMethod("isBedrockPlayer", java.util.UUID.class);
            return Boolean.TRUE.equals(check.invoke(api, player.getUniqueId()));
        } catch (ReflectiveOperationException | LinkageError exception) {
            getLogger().warning("Could not query Geyser API; treating " + player.getName() + " as Java.");
            return false;
        }
    }

    private void send(CommandSender sender, String key, String... replacements) {
        String message = getConfig().getString("messages." + key, "&cMissing message: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.prefix", "") + message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> suggestions = new ArrayList<>();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        if (sender.hasPermission("mctransfer.admin") && "reload".startsWith(prefix)) suggestions.add("reload");
        var section = getConfig().getConfigurationSection("servers");
        if (section != null) for (String server : section.getKeys(false)) {
            if (server.toLowerCase(Locale.ROOT).startsWith(prefix)) suggestions.add(server);
        }
        return suggestions;
    }
}

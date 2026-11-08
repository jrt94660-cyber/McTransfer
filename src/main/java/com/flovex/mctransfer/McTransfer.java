package com.flovex.mctransfer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class McTransfer extends JavaPlugin implements TabExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        var command = getCommand("transfer");
        if (command == null) {
            getLogger().severe("The transfer command is missing from plugin.yml; disabling McTransfer.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
        getLogger().info("McTransfer enabled. No proxy is required for native transfers.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mctransfer.admin")) {
                send(sender, "no-permission");
                return true;
            }
            reloadConfig();
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
            if (getConfig().getBoolean("allow-direct-java-addresses", false)
                    && !isBedrockPlayer(player)) {
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
            getLogger().warning("Invalid " + (bedrock ? "Bedrock" : "Java")
                    + " destination configured for server '" + requested + "'.");
            send(player, "transfer-failed");
            return true;
        }

        return transfer(player, displayName, host, port);
    }

    private boolean transfer(Player player, String displayName, String host, int port) {
        send(player, "transferring", "%server%", displayName);
        try {
            // Paper sends the native client transfer packet. Geyser translates this
            // packet for Bedrock clients, using the Bedrock endpoint selected above.
            player.transfer(host, port);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            getLogger().warning("Transfer request failed for " + player.getName()
                    + " to " + host + ":" + port + " (" + exception.getMessage() + ")");
            send(player, "transfer-failed");
        } catch (RuntimeException exception) {
            getLogger().warning("Unexpected transfer error for " + player.getName()
                    + ": " + exception.getMessage());
            send(player, "transfer-failed");
        }
        return true;
    }

    private boolean isBedrockPlayer(Player player) {
        if (getServer().getPluginManager().getPlugin("Geyser-Spigot") == null
                && getServer().getPluginManager().getPlugin("Geyser") == null) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object api = apiClass.getMethod("api").invoke(null);
            if (api == null) {
                return false;
            }
            Method check = apiClass.getMethod("isBedrockPlayer", java.util.UUID.class);
            return Boolean.TRUE.equals(check.invoke(api, player.getUniqueId()));
        } catch (ReflectiveOperationException | LinkageError exception) {
            getLogger().warning("Could not query the Geyser API; treating "
                    + player.getName() + " as a Java player. Check your Geyser version.");
            return false;
        }
    }

    private void send(CommandSender sender, String key, String... replacements) {
        String message = getConfig().getString("messages." + key, "&cMissing message: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        String prefix = getConfig().getString("messages.prefix", "");
        sender.sendMessage(color(prefix + message));
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> suggestions = new ArrayList<>();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        if (sender.hasPermission("mctransfer.admin") && "reload".startsWith(prefix)) {
            suggestions.add("reload");
        }
        var section = getConfig().getConfigurationSection("servers");
        if (section != null) {
            for (String server : section.getKeys(false)) {
                if (server.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    suggestions.add(server);
                }
            }
        }
        return suggestions;
    }
}
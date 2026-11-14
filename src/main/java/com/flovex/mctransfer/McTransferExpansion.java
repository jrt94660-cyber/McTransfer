package com.flovex.mctransfer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class McTransferExpansion extends PlaceholderExpansion {
    private final McTransfer plugin;
    public McTransferExpansion(McTransfer plugin) { this.plugin = plugin; }
    @Override public @NotNull String getIdentifier() { return "mctransfer"; }
    @Override public @NotNull String getAuthor() { return "Flovex"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (key.equals("motd_line1")) return plugin.getMotdLine("line1");
        if (key.equals("motd_line2")) return plugin.getMotdLine("line2");
        if (key.equals("motd_icon")) return plugin.getMotdIcon();

        String[] parts = key.split("_");
        if (parts.length < 2) return null;
        String server = parts[0];
        if (!plugin.getConfig().isConfigurationSection("servers." + server)) return null;
        McTransfer.ServerStatus status = plugin.getStatus(server);
        String field = String.join("_", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return switch (field) {
            case "name" -> plugin.getServerDisplayName(server);
            case "online" -> status.online() ? String.valueOf(status.players()) : "0";
            case "max" -> status.online() ? String.valueOf(status.maxPlayers()) : "0";
            case "players" -> status.online() ? status.players() + "/" + status.maxPlayers() : "OFFLINE";
            case "status" -> status.online() ? "ONLINE" : "OFFLINE";
            case "click" -> plugin.getConfig().getString("servers." + server + ".click-text", "<green>CLICK TO JOIN</green>");
            case "motd_line1" -> plugin.getConfig().getString("servers." + server + ".motd.line1", plugin.getMotdLine("line1"));
            case "motd_line2" -> plugin.getConfig().getString("servers." + server + ".motd.line2", plugin.getMotdLine("line2"));
            default -> null;
        };
    }
}

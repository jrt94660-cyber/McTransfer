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

    // Placeholder format: %mctransfer_<server-key>_players%
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (!key.endsWith("_players")) return null;
        String server = key.substring(0, key.length() - "_players".length());
        if (server.isBlank() || !plugin.getConfig().isConfigurationSection("servers." + server)) return null;
        McTransfer.ServerStatus status = plugin.getStatus(server);
        return status.online() ? String.valueOf(status.players()) : "0";
    }
}

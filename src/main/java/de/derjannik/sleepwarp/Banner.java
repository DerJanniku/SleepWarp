package de.derjannik.sleepwarp;

import org.bukkit.plugin.java.JavaPlugin;

final class Banner {

    private Banner() {
    }

    static void print(JavaPlugin plugin) {
        String line = "=".repeat(80);
        plugin.getLogger().info(line);
        plugin.getLogger().info("███████╗██╗     ███████╗███████╗██████╗ ██╗    ██╗ █████╗ ██████╗ ██████╗");
        plugin.getLogger().info("██╔════╝██║     ██╔════╝██╔════╝██╔══██╗██║    ██║██╔══██╗██╔══██╗██╔══██╗");
        plugin.getLogger().info("███████╗██║     █████╗  █████╗  ██████╔╝██║ █╗ ██║███████║██████╔╝██████╔╝");
        plugin.getLogger().info("╚════██║██║     ██╔══╝  ██╔══╝  ██╔═══╝ ██║███╗██║██╔══██║██╔══██╗██╔═══╝");
        plugin.getLogger().info("███████║███████╗███████╗███████╗██║     ╚███╔███╔╝██║  ██║██║  ██║██║");
        plugin.getLogger().info("╚══════╝╚══════╝╚══════╝╚══════╝╚═╝      ╚══╝╚══╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝");
        plugin.getLogger().info("");
        plugin.getLogger().info("                                                        by DerJannik");
        plugin.getLogger().info(line);
    }
}

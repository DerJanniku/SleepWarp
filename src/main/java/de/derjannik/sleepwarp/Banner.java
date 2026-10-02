package de.derjannik.sleepwarp;

import org.bukkit.plugin.java.JavaPlugin;

final class Banner {

    static final String FIVERR = "https://de.fiverr.com/s/xXgY29x";

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
        plugin.getLogger().info("        Made by DerJannik | " + FIVERR);
        plugin.getLogger().info(line);
    }
}

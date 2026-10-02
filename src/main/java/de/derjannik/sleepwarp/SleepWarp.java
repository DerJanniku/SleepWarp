package de.derjannik.sleepwarp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SleepWarp extends JavaPlugin {

    private WarpEngine engine;

    @Override
    public void onEnable() {
        Banner.print(this);
        saveDefaultConfig();
        this.engine = new WarpEngine(this);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(engine, this);

        // Start warp task
        engine.runTaskTimer(this, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            engine.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("sleepwarp.admin")) {
                    sender.sendMessage(message("no-permission"));
                    return true;
                }
                reloadConfig();
                engine.shutdown();
                sender.sendMessage(message("reloaded"));
            }
            case "info" -> {
                sender.sendMessage(ChatColor.BLUE + "SleepWarp " + ChatColor.GRAY + "v" + getDescription().getVersion()
                        + " by " + ChatColor.YELLOW + "DerJannik");
                sender.sendMessage(ChatColor.GRAY + "Custom plugins: " + ChatColor.AQUA + Banner.FIVERR);
            }
            default -> {
                World world = sender instanceof Player player ? player.getWorld() : Bukkit.getWorlds().get(0);
                sender.sendMessage(engine.status(world));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("status", "info", "reload")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    out.add(option);
                }
            }
        }
        return out;
    }

    String message(String key) {
        return color(getConfig().getString("messages." + key, ""));
    }

    static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

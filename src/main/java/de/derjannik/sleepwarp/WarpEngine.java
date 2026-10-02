package de.derjannik.sleepwarp;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class WarpEngine extends BukkitRunnable implements Listener {

    private static final long NIGHT_START = 12542;
    private static final long NIGHT_END = 23460;

    private record Count(int sleeping, int total) {
    }

    private final SleepWarp plugin;
    private final Map<UUID, Long> lastActive = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Long> warpStart = new HashMap<>();
    private final Map<UUID, Long> announced = new HashMap<>();

    WarpEngine(SleepWarp plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL
                    || plugin.getConfig().getStringList("disabled-worlds").contains(world.getName())) {
                continue;
            }
            tick(world);
        }
    }

    private void tick(World world) {
        Count count = count(world);
        long time = world.getTime();
        boolean night = time >= NIGHT_START && time < NIGHT_END;
        boolean storm = world.isThundering();
        if (count.sleeping() == 0 || (!night && !storm)) {
            // Players wake up on their own at sunrise, so a running warp that reaches day is a finished night
            if (warpStart.containsKey(world.getUID()) && !night && !storm) {
                morning(world);
            } else {
                stop(world);
            }
            return;
        }
        int speed = speed(count);
        warpStart.putIfAbsent(world.getUID(), System.currentTimeMillis());
        showBar(world, count, speed, night ? (time - NIGHT_START) / (double) (NIGHT_END - NIGHT_START) : 1.0);

        if (!night) {
            // Daytime thunderstorm, only the weather is fast-forwarded
            if (plugin.getConfig().getBoolean("warp-storms")) {
                world.setThunderDuration(Math.max(1, world.getThunderDuration() - speed));
                world.setWeatherDuration(Math.max(1, world.getWeatherDuration() - speed));
            }
            return;
        }
        long extra = Math.min(speed - 1L, NIGHT_END - time);
        world.setFullTime(world.getFullTime() + extra);
    }

    private void morning(World world) {
        FileConfiguration config = plugin.getConfig();
        Long started = warpStart.get(world.getUID());
        long seconds = started == null ? 0 : Math.max(1, (System.currentTimeMillis() - started) / 1000);
        if (config.getBoolean("morning.clear-weather")) {
            world.setStorm(false);
            world.setThundering(false);
        }
        String message = plugin.message("morning").replace("{seconds}", String.valueOf(seconds));
        for (Player player : world.getPlayers()) {
            if (config.getBoolean("morning.reset-phantoms")) {
                player.setStatistic(Statistic.TIME_SINCE_REST, 0);
            }
            if (!message.isEmpty()) {
                player.sendMessage(message);
            }
            if (config.getBoolean("morning.sound")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
            }
        }
        stop(world);
    }

    private void stop(World world) {
        warpStart.remove(world.getUID());
        BossBar bar = bars.remove(world.getUID());
        if (bar != null) {
            bar.removeAll();
        }
    }

    void shutdown() {
        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }
        bars.clear();
        warpStart.clear();
    }

    private Count count(World world) {
        FileConfiguration config = plugin.getConfig();
        long afkMillis = config.getLong("ignore.afk-minutes") * 60_000L;
        long now = System.currentTimeMillis();
        int sleeping = 0;
        int total = 0;
        for (Player player : world.getPlayers()) {
            if (player.isSleeping()) {
                sleeping++;
                total++;
                continue;
            }
            if (player.isSleepingIgnored() || player.hasPermission("sleepwarp.bypass")) {
                continue;
            }
            if (player.getGameMode() == GameMode.SPECTATOR && config.getBoolean("ignore.spectators")) {
                continue;
            }
            if (player.getGameMode() == GameMode.CREATIVE && config.getBoolean("ignore.creative")) {
                continue;
            }
            if (afkMillis > 0 && now - lastActive.getOrDefault(player.getUniqueId(), now) > afkMillis) {
                continue;
            }
            total++;
        }
        return new Count(sleeping, Math.max(1, total));
    }

    private int speed(Count count) {
        FileConfiguration config = plugin.getConfig();
        double min = Math.max(1, config.getDouble("speed.min"));
        double max = Math.max(min, config.getDouble("speed.max"));
        double ratio = Math.min(1.0, count.sleeping() / (double) count.total());
        // One sleeper out of many starts at min, everyone asleep reaches max
        double share = count.total() <= 1 ? 1.0 : (count.sleeping() - 1) / (double) (count.total() - 1);
        double curved = Math.pow(Math.min(ratio, share), Math.max(0.1, config.getDouble("speed.curve", 1.0)));
        return (int) Math.round(min + (max - min) * curved);
    }

    private void showBar(World world, Count count, int speed, double progress) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("bossbar.enabled")) {
            return;
        }
        BossBar bar = bars.computeIfAbsent(world.getUID(), id -> Bukkit.createBossBar("", barColor(), BarStyle.SOLID));
        bar.setTitle(fill(SleepWarp.color(config.getString("bossbar.title", "")), count, speed));
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        for (Player player : world.getPlayers()) {
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
        for (Player player : bar.getPlayers()) {
            if (player.getWorld() != world) {
                bar.removePlayer(player);
            }
        }
    }

    private BarColor barColor() {
        try {
            return BarColor.valueOf(plugin.getConfig().getString("bossbar.color", "BLUE").toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.BLUE;
        }
    }

    private String fill(String text, Count count, int speed) {
        return text.replace("{speed}", String.valueOf(speed))
                .replace("{sleeping}", String.valueOf(count.sleeping()))
                .replace("{total}", String.valueOf(count.total()));
    }

    String status(World world) {
        Count count = count(world);
        int speed = count.sleeping() == 0 ? 1 : speed(count);
        return fill(plugin.message("status"), count, speed).replace("{world}", world.getName());
    }

    // Vanilla would jump straight to morning, the warp takes over instead
    @EventHandler(ignoreCancelled = true)
    public void onTimeSkip(TimeSkipEvent event) {
        if (isNightSkip(event)
                && event.getWorld().getEnvironment() == World.Environment.NORMAL
                && !plugin.getConfig().getStringList("disabled-worlds").contains(event.getWorld().getName())) {
            event.setCancelled(true);
        }
    }

    // The skip reason enum moved to another class in 26.1, so it is read by name
    private boolean isNightSkip(TimeSkipEvent event) {
        try {
            return "NIGHT_SKIP".equals(String.valueOf(event.getClass().getMethod("getSkipReason").invoke(event)));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = announced.put(player.getUniqueId(), now);
        if (last != null && now - last < 10_000L) {
            return;
        }
        World world = player.getWorld();
        // The sleeping state is only set after this event
        Bukkit.getScheduler().runTask(plugin, () -> {
            String message = plugin.message("bed-enter");
            if (!player.isSleeping() || message.isEmpty()) {
                return;
            }
            Count count = count(world);
            String text = fill(message, count, speed(count)).replace("{player}", player.getName());
            for (Player other : world.getPlayers()) {
                other.sendMessage(text);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() != null && (event.getFrom().getYaw() != event.getTo().getYaw()
                || event.getFrom().getPitch() != event.getTo().getPitch())) {
            lastActive.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        lastActive.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        lastActive.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        lastActive.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastActive.remove(id);
        announced.remove(id);
        for (BossBar bar : bars.values()) {
            bar.removePlayer(event.getPlayer());
        }
    }
}

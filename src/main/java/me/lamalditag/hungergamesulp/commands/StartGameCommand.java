package me.lamalditag.hungergamesulp.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.lamalditag.hungergamesulp.HungerGamesULP;

import java.io.File;
import java.util.List;

public class StartGameCommand implements CommandExecutor {
    private final HungerGamesULP plugin;
    private boolean gameStarting = false;

    public StartGameCommand(HungerGamesULP plugin) {
        this.plugin = plugin;
        createSetSpawnConfig();
        createArenaConfig();
    }

    private FileConfiguration setspawnConfig = null;
    private File setspawnFile = null;

    private FileConfiguration arenaConfig = null;
    private File arenaFile = null;

    public void createSetSpawnConfig() {
        setspawnFile = new File(plugin.getDataFolder(), "setspawn.yml");
        if (!setspawnFile.exists()) {
            setspawnFile.getParentFile().mkdirs();
            plugin.saveResource("setspawn.yml", false);
        }

        setspawnConfig = YamlConfiguration.loadConfiguration(setspawnFile);
    }

    public void createArenaConfig() {
        arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!arenaFile.exists()) {
            arenaFile.getParentFile().mkdirs();
            plugin.saveResource("arena.yml", false);
        }

        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("start")) {
            if (sender instanceof Player p) {
                if (plugin.getServer().getOnlinePlayers().size() < 2) {
                    p.sendMessage(ChatColor.RED + "There must be at least 2 players to start the game!");
                    return true;
                }
                List<String> spawnpoints = setspawnConfig.getStringList("spawnpoints");
                String world = arenaConfig.getString("region.world");
                if (spawnpoints.isEmpty() && world == null) {
                    p.sendMessage(ChatColor.RED + "Set up arena and spawnpoints first!");
                    return true;
                } else if (spawnpoints.isEmpty()) {
                    p.sendMessage(ChatColor.RED + "Set up spawnpoints first!");
                    return true;
                } else if (world == null) {
                    p.sendMessage(ChatColor.RED + "Set up arena first!");
                    return true;
                }

                if (plugin.gameStarted) {
                    sender.sendMessage(ChatColor.RED + "The game has already started!");
                    return true;
                }

                if (gameStarting) {
                    sender.sendMessage(ChatColor.RED + "The game is already starting!");
                    return true;
                }

                gameStarting = true;

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    plugin.getGameHandler().startGame();
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    }
                    gameStarting = false;
                }, 20L * 1); // ! cambiar el "* 1" por un "* 20"

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "15 seconds left!");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                }, 20L * 5);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "10 seconds left!");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                }, 20L * 10);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "5 seconds left!");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                }, 20L * 15);

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "4 seconds left!");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                }, 20L * 16);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "3 seconds left!");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                }, 20L * 17);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "2 seconds left!");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                }, 20L * 18);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    plugin.getServer().broadcastMessage(ChatColor.GOLD + "1 second left!");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                }, 20L * 19);

                Bukkit.broadcastMessage(ChatColor.GOLD + "The game will start in 20 seconds!");
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                }
                return true;
            }
        }
        return false;
    }
}


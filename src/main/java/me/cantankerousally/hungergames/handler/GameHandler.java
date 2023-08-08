package me.cantankerousally.hungergames.handler;

import me.cantankerousally.hungergames.HungerGames;
import me.cantankerousally.hungergames.commands.ChestRefillCommand;
import me.cantankerousally.hungergames.commands.SupplyDropCommand;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public class GameHandler implements Listener {

    private HungerGames plugin;
    private SetSpawnHandler setSpawnHandler;

    public GameHandler(HungerGames plugin, SetSpawnHandler setSpawnHandler) {
        this.plugin = plugin;
        this.setSpawnHandler = setSpawnHandler;
    }

    private int timerTaskId;
    private int timeLeft;
    private List<Player> playersAlive;
    private int gracePeriodTaskId;
    private BukkitTask supplyDropTask;
    public Map<UUID, Location> deathLocations = new HashMap<>();


    public void startGame() {
        // Start game
        plugin.gameStarted = true;
        WorldBorderHandler worldBorderHandler = new WorldBorderHandler(plugin);
        worldBorderHandler.startBorderShrink();

        // Set the time left
        timeLeft = plugin.getConfig().getInt("game-time");

        // Initialize the list of players alive
        playersAlive = new ArrayList<>();

        // Add players to boss bar and list of players alive
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.bossBar.addPlayer(player);
            playersAlive.add(player);
        }

        // Send message to players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "The game has started!");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "The grace period has started! PvP is disabled!");
        }

        // Turn off PvP
        for (World world : plugin.getServer().getWorlds()) {
            world.setPVP(false);
        }

        // Schedule a delayed task to turn PvP back on
        int gracePeriod = plugin.getConfig().getInt("grace-period");
        gracePeriodTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // Turn PvP back on
            for (World world : plugin.getServer().getWorlds()) {
                world.setPVP(true);
            }

            // Send message to players
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "The grace period has ended! PvP is now enabled!");
            }
        }, gracePeriod * 20L);

        // Create a scoreboard to display the timer and number of players alive
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("gameinfo", "dummy", "Game Info");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Score timeLeftScore = objective.getScore("Time Left:");
        timeLeftScore.setScore(timeLeft);
        Score playersAliveScore = objective.getScore("Players Alive:");
        playersAliveScore.setScore(playersAlive.size());

        // Set the scoreboard for all players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }

        // Schedule a repeating task to update the boss bar's progress
        timerTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                // Update the boss bar's progress
                plugin.bossBar.setProgress((double) timeLeft / plugin.getConfig().getInt("game-time"));

                // Decrement the time left
                timeLeft--;
                timeLeftScore.setScore(timeLeft);

                // Check if there is only one player alive
                if (playersAlive.size() == 1) {
                    // Cancel the task
                    plugin.getServer().getScheduler().cancelTask(timerTaskId);

                    // Send message to players
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "The game has ended!");
                    }

                    // Declare the winner
                    Player winner = playersAlive.get(0);
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.sendMessage(ChatColor.LIGHT_PURPLE + winner.getName() + " is the winner!");
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }

                    // End the game
                    endGame();
                }

                // Check if the time is up
                if (timeLeft < 0) {
                    // Cancel the task
                    plugin.getServer().getScheduler().cancelTask(timerTaskId);

                    // End the game
                    endGame();
                }
            }
        }, 0L, 20L);
// Schedule supply drops
        int supplyDropInterval = plugin.getConfig().getInt("supplydrop.interval") * 20; // Convert seconds to ticks
        SupplyDropCommand supplyDropCommand = new SupplyDropCommand(plugin);
        PluginCommand supplyDropPluginCommand = plugin.getCommand("supplydrop");

        supplyDropTask = new BukkitRunnable() {
            @Override
            public void run() {
                supplyDropCommand.onCommand(plugin.getServer().getConsoleSender(), supplyDropPluginCommand, "supplydrop", new String[0]);
            }
        }.runTaskTimer(plugin, supplyDropInterval, supplyDropInterval);

        // Refill chests at start of game
        ChestRefillCommand chestRefillCommand = new ChestRefillCommand(plugin);
        PluginCommand chestRefillPluginCommand = plugin.getCommand("chestrefill");
        chestRefillCommand.onCommand(plugin.getServer().getConsoleSender(), chestRefillPluginCommand, "chestrefill", new String[0]);

        // Schedule a delayed task to refill chests again at specified time
        int chestRefillTime = plugin.getConfig().getInt("chestrefill.time") * 20; // Convert seconds to ticks
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                chestRefillCommand.onCommand(plugin.getServer().getConsoleSender(), chestRefillPluginCommand, "chestrefill", new String[0]);
            }
        }, chestRefillTime);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove player from boss bar and list of players alive
        if (playersAlive != null) {
            // Remove player from boss bar and list of players alive
            plugin.bossBar.removePlayer(player);
            playersAlive.remove(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.deathLocations.put(player.getUniqueId(), player.getLocation());
        if (playersAlive != null) {
            // Remove player from list of players alive
            playersAlive.remove(player);
        }
        updatePlayersAliveScore();
    }


    public void updatePlayersAliveScore() {
        // Update the playersAliveScore for each player
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Get the player's scoreboard
            Scoreboard scoreboard = player.getScoreboard();

            // Get the playersAliveScore
            Objective objective = scoreboard.getObjective("gameinfo");
            Score playersAliveScore = objective.getScore("Players Alive:");

            // Update the playersAliveScore
            playersAliveScore.setScore(playersAlive.size());
        }
    }


    public void endGame() {
        // End game
        plugin.gameStarted = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        // Get the server
        Server server = plugin.getServer();

        // Execute the /kill command to remove all item entities
        server.dispatchCommand(server.getConsoleSender(), "kill @e[type=item]");

        // Execute the /kill command to remove all experience orb entities
        server.dispatchCommand(server.getConsoleSender(), "kill @e[type=experience_orb]");


        plugin.getServer().getScheduler().cancelTask(timerTaskId);

        // Remove players from boss bar and clear list of players alive
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard emptyScoreboard = manager.getNewScoreboard();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.bossBar.removePlayer(player);
            player.setScoreboard(emptyScoreboard);
        }
        playersAlive.clear();

        setSpawnHandler.clearOccupiedSpawnPoints();

        // Teleport players to world spawn location
        World world = plugin.getServer().getWorld("world");
        Location spawnLocation = world.getSpawnLocation();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.teleport(spawnLocation);
        }

        if (supplyDropTask != null) {
            supplyDropTask.cancel();
            supplyDropTask = null;
        }

        // Remove all red shulker boxes in the world
        for (Chunk chunk : world.getLoadedChunks()) {
            for (BlockState blockState : chunk.getTileEntities()) {
                if (blockState instanceof ShulkerBox) {
                    ShulkerBox shulkerBox = (ShulkerBox) blockState;
                    if (shulkerBox.getColor() == DyeColor.RED) {
                        shulkerBox.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }
}
package me.lamalditag.hungergamesulp.handler;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import me.lamalditag.hungergamesulp.HungerGamesULP;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class SetSpawnHandler implements Listener {

    private final HungerGamesULP plugin;
    private FileConfiguration setSpawnConfig = null;
    private File setSpawnFile = null;

    public SetSpawnHandler(HungerGamesULP plugin) {
        this.plugin = plugin;
        createSetSpawnConfig();
        createArenaConfig();
    }

    private final Set<String> occupiedSpawnPoints = new HashSet<>();
    private final Map<Player, String> playerSpawnPoints = new HashMap<>();

    public void createSetSpawnConfig() {
        setSpawnFile = new File(plugin.getDataFolder(), "setspawn.yml");
        if (!setSpawnFile.exists()) {
            setSpawnFile.getParentFile().mkdirs();
            plugin.saveResource("setspawn.yml", false);
        }

        setSpawnConfig = YamlConfiguration.loadConfiguration(setSpawnFile);
    }

    public void createArenaConfig() {
        File arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!arenaFile.exists()) {
            arenaFile.getParentFile().mkdirs();
            plugin.saveResource("arena.yml", false);
        }

        FileConfiguration arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
    }

    public FileConfiguration getSetSpawnConfig() {
        if (setSpawnConfig == null) {
            createSetSpawnConfig();
        }
        return setSpawnConfig;
    }

    public void saveSetSpawnConfig() {
        if (setSpawnConfig == null || setSpawnFile == null) {
            return;
        }
        try {
            getSetSpawnConfig().save(setSpawnFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not save config to " + setSpawnFile, ex);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.STICK && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            if (meta.getDisplayName().equals(ChatColor.AQUA + "Spawn Point Selector")) {
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    Location location = Objects.requireNonNull(event.getClickedBlock()).getLocation();
                    List<String> spawnPoints = getSetSpawnConfig().getStringList("spawnpoints");
                    String newSpawnPoint = Objects.requireNonNull(location.getWorld()).getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
                    if (spawnPoints.contains(newSpawnPoint)) {
                        player.sendMessage(ChatColor.RED + "You can't choose the same block for two spawn points!");
                        return;
                    }
                    if (spawnPoints.size() < 24) {
                        spawnPoints.add(newSpawnPoint);
                        getSetSpawnConfig().set("spawnpoints", spawnPoints);
                        saveSetSpawnConfig();
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "Spawn point " + ChatColor.GOLD + spawnPoints.size() + ChatColor.LIGHT_PURPLE + " set at X: " + location.getBlockX() + " Y: " + location.getBlockY() + " Z: " + location.getBlockZ());
                    } else if (spawnPoints.size() ==  24){
                        player.sendMessage(ChatColor.RED + "You have reached the maximum number of spawn points!");
                    }
                    event.setCancelled(true);
                }
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            assert block != null;
            if (block.getState() instanceof Sign sign) {
                if (sign.getLine(0).equalsIgnoreCase("[Join]")) {
                    if (plugin.gameStarted) {
                        player.sendMessage(ChatColor.RED + "The game has already started!");
                        return;
                    }
                    // Teleport player to an unoccupied spawn point
                    List<String> spawnPoints = getSetSpawnConfig().getStringList("spawnpoints");
                    List<String> availableSpawnPoints = new ArrayList<>(spawnPoints);
                    availableSpawnPoints.removeAll(occupiedSpawnPoints);
                    if (!availableSpawnPoints.isEmpty()) {
                        String spawnPoint = availableSpawnPoints.get(new Random().nextInt(availableSpawnPoints.size()));
                        String[] coords = spawnPoint.split(",");
                        World world = plugin.getServer().getWorld(coords[0]);
                        double x = Double.parseDouble(coords[1]);
                        double y = Double.parseDouble(coords[2]) + 1;
                        double z = Double.parseDouble(coords[3]);
                        player.teleport(new Location(world, x, y, z));
                        occupiedSpawnPoints.add(spawnPoint);
                        player.setGameMode(GameMode.ADVENTURE);
                        player.getInventory().clear();
                        player.setExp(0);
                        player.setLevel(30);
                        player.setHealth(20);
                        player.setFoodLevel(20);
                        for (PotionEffect effect : player.getActivePotionEffects()) {
                            player.removePotionEffect(effect.getType());
                        }
                        // Broadcast a message to all players
                        plugin.getServer().broadcastMessage(ChatColor.AQUA + player.getName() + " has joined [" + occupiedSpawnPoints.size() + "/" + spawnPoints.size() + "]");
                        // Add player to the map of players and their spawn points
                        playerSpawnPoints.put(player, spawnPoint);
                    } else {
                        player.sendMessage(ChatColor.RED + "All spawn points are currently occupied!");
                    }
                }
            }
        }
    }

    public void clearOccupiedSpawnPoints() {
        occupiedSpawnPoints.clear();
    }

    public void removeOccupiedSpawnPoint(String spawnPoint) {
        occupiedSpawnPoints.remove(spawnPoint);
    }

    public Map<Player, String> getPlayerSpawnPoints() {
        return playerSpawnPoints;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getBlock().getType() == Material.ANVIL || event.getBlock().getType() == Material.CHIPPED_ANVIL || event.getBlock().getType() == Material.DAMAGED_ANVIL) {
            event.setCancelled(true);
        }
    }
}
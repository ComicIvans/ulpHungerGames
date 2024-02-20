package me.lamalditag.hungergamesulp.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class CompassHandler {

    public CompassHandler(JavaPlugin plugin) {

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.COMPASS) {
                    Player nearestPlayer = findNearestPlayer(player);
                    if (nearestPlayer != null) {
                        player.setCompassTarget(nearestPlayer.getLocation());
                    }
                }
            }
        }, 0L, 20L);
    }

    private Player findNearestPlayer(Player player) {
        double closestDistance = Double.MAX_VALUE;
        Player closestPlayer = null;
        for (Player otherPlayer : Bukkit.getServer().getOnlinePlayers()) {
            if (otherPlayer != player && otherPlayer.getWorld() == player.getWorld()) {
                double distance = player.getLocation().distance(otherPlayer.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = otherPlayer;
                }
            }
        }
        return closestPlayer;
    }
}

package top.iseason.bukkit.pwpsetting;

import cz._heropwp.playerworldspro.api.API;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.UUID;

public class Listener implements org.bukkit.event.Listener {
    private static HashMap<UUID, Long> cooldown = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!checkNotMember(event.getBlock().getLocation().getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.destroy.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.destroy")) return;
        event.setCancelled(true);
        sendMessage(player, "settings.destroy.message");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!checkNotMember(event.getBlock().getLocation().getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.place.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.place")) return;
        event.setCancelled(true);
        sendMessage(player, "settings.place.message");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!checkNotMember(player.getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.block-interact.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.block-interact"))
            return;
        event.setCancelled(true);
        sendMessage(player, "settings.block-interact.message");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!checkNotMember(player.getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.entity-interact.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.entity-interact"))
            return;
        event.setCancelled(true);
        sendMessage(player, "settings.entity-interact.message");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Player player = null;
        if (damager instanceof Player)
            player = (Player) damager;
        else if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                player = (Player) shooter;
            }
        }
        if (player == null) return;
        if (!checkNotMember(player.getWorld(), player)) return;
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            if (PWPSetting.config.getBoolean("settings.pvp.enable")) return;
            if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.pvp")) return;
            event.setCancelled(true);
            sendMessage(player, "settings.pvp.message");
            return;
        }
        if (entity instanceof LivingEntity) {
            if (PWPSetting.config.getBoolean("settings.pve.enable")) return;
            if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.pve")) return;
            event.setCancelled(true);
            sendMessage(player, "settings.pve.message");
            return;
        }
        if (PWPSetting.config.getBoolean("settings.pvd.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.pvd")) return;
        event.setCancelled(true);
        sendMessage(player, "settings.pvd.message");
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
        Entity damager = event.getRemover();
        Player player = null;
        if (damager instanceof Player)
            player = (Player) damager;
        else if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                player = (Player) shooter;
            }
        }
        if (player == null) return;
        if (!checkNotMember(player.getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.hanging.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.hanging")) return;
        event.setCancelled(true);
        sendMessage(player, "settings.hanging.message");
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingPlaceEvent(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (!checkNotMember(player.getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.hanging.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.hanging")) return;
        event.setCancelled(true);
        sendMessage(player, "settings.hanging.message");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!checkNotMember(player.getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.drop.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.drop")) return;
        event.setCancelled(true);
        sendMessage(player, "settings.drop.message");
    }

    @EventHandler
    public void onEntityPickupItemEvent(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        Player player = (Player) entity;
        if (!checkNotMember(player.getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.pickup.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.pickup")) return;
        event.setCancelled(true);
        sendMessage(player, "settings.pickup.message");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFishEvent(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!checkNotMember(player.getWorld(), player)) return;
        if (PWPSetting.config.getBoolean("settings.fish.enable")) return;
        if (player.isOp() || player.hasPermission("pwpsetting.*") || player.hasPermission("pwpsetting.fish")) return;
        event.setCancelled(true);
        sendMessage(player, "settings.fish.message");
    }


    private boolean checkNotMember(World world, Player player) {
        String ownerUUID = PWPSetting.getOwnerUUID(world.getName());
        if (ownerUUID == null) return false;
        return !API.isMember(player, ownerUUID);
    }

    private String toColor(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void sendMessage(Player player, String key) {
        String string = PWPSetting.config.getString(key, "");
        if (string.isEmpty()) return;
        UUID uniqueId = player.getUniqueId();
        Long aLong = cooldown.get(uniqueId);
        long l = System.currentTimeMillis();
        if (aLong != null && l - aLong < 3000L) return;
        player.sendMessage(toColor(string));
        cooldown.put(uniqueId, l);
    }
}

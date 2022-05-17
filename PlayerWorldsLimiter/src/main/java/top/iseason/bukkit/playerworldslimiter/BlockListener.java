package top.iseason.bukkit.playerworldslimiter;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.util.HashMap;
import java.util.UUID;

public class BlockListener implements Listener {

    private static Player spawningEgg = null;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Block block = event.getBlock();
        String name = block.getWorld().getName();
        //只对家园生效
        if (PlayerWorldsLimiter.getOwnerUUID(name) == null) {
            return;
        }
        String type = ConfigManager.getBlockID(block);
        Integer max = ConfigManager.getMax(name, type, "block");
        if (max == null) return;
        HashMap<String, Integer> map = ConfigManager.getBlockData().get(name);
        int count = 0;
        HashMap<String, Integer> data = map;
        if (map != null) {
            Integer current = map.get(type);
            if (current != null) {
                count = current;
            }
        } else {
            data = new HashMap<>();
            ConfigManager.getBlockData().put(name, data);
        }
        if (count + 1 > max) {
            event.setCancelled(true);
            String blockMessage = ConfigManager.getBlockMessage();
            if (!blockMessage.isEmpty())
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', blockMessage.replace("%max%", max.toString())));
        } else data.put(type, count + 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        reduceBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFadeEvent(BlockFadeEvent event) {
        reduceBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplodeEvent(BlockExplodeEvent event) {
        reduceBlock(event.getBlock());
        reduceBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        reduceBlocks(event.blockList());
    }

    public void reduceBlocks(Iterable<Block> blocks) {
        for (Block block : blocks) {
            reduceBlock(block);
        }
    }


    public void reduceBlock(Block block) {
        String name = block.getWorld().getName();
        //只对家园生效
        if (PlayerWorldsLimiter.getOwnerUUID(name) == null) {
            return;
        }
        String type = ConfigManager.getBlockID(block);
        Integer max = ConfigManager.getMax(name, type, "block");
        //说明不限制
        if (max == null) return;
        HashMap<String, Integer> map = ConfigManager.getBlockData().get(name);
        int count = 0;
        HashMap<String, Integer> data = map;
        if (map != null) {
            Integer current = map.get(type);
            if (current != null) {
                count = current;
            }
        } else {
            //还没有数据
            data = new HashMap<>();
            ConfigManager.getBlockData().put(name, data);
        }
        count--;
        count = Math.max(count, 0);
        data.put(type, count);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Player player = null;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            player = spawningEgg;
            spawningEgg = null;
        }
        if (event.isCancelled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        String name = event.getLocation().getWorld().getName();
        String type = entity.toString();
        Integer max = ConfigManager.getMax(name, type, "entity");
        if (max == null) return;
        HashMap<String, HashMap<String, Integer>> entityData = ConfigManager.getEntityData();
        if (entityData == null) return;
        HashMap<String, Integer> map = entityData.get(name);
        int count = 0;
        HashMap<String, Integer> data = map;
        if (map != null) {
            Integer current = map.get(type);
            if (current != null) {
                count = current;
            }
        } else {
            data = new HashMap<>();
            ConfigManager.getEntityData().put(name, data);
        }
        if (count + 1 > max) {
            event.setCancelled(true);
            if (player != null) {
                String entityMessage = ConfigManager.getEntityMessage();
                if (!entityMessage.isEmpty())
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', entityMessage.replace("%max%", max.toString())));
            }
        } else data.put(type, count + 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null) return;
            if (!item.hasItemMeta()) return;
            if (!(item.getItemMeta() instanceof SpawnEggMeta)) return;
            spawningEgg = event.getPlayer();
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String name = entity.getLocation().getWorld().getName();
        String type = entity.toString();
        Integer max = ConfigManager.getMax(name, type, "entity");
        //说明不限制
        if (max == null) return;
        HashMap<String, Integer> map = ConfigManager.getEntityData().get(name);
        int count = 0;
        HashMap<String, Integer> data = map;
        if (map != null) {
            Integer current = map.get(type);
            if (current != null) {
                count = current;
            }
        } else {
            data = new HashMap<>();
            ConfigManager.getEntityData().put(name, data);
        }
        count--;
        count = Math.max(count, 0);
        data.put(type, count);
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ConfigManager.savePlayerPermissionAsy(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        UUID uniqueId = player.getUniqueId();
        if (SelectCommand.entitySelectors.contains(uniqueId)) {
            String type = event.getRightClicked().toString();
            Integer integer = ConfigManager.getGlobalEntities().get(type);
            int apply = 1;
            if (player.isSneaking()) apply = -1;
            if (integer == null) {
                if (apply == -1) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.GREEN + "实体 " + type + ChatColor.GREEN + " 没有限制");
                    return;
                }
                ConfigManager.getGlobalEntities().put(type, 1);
                integer = 1;
            } else {
                integer += apply;
                if (integer < 0) {
                    ConfigManager.getGlobalEntities().remove(type);
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.GREEN + "已取消实体 " + type + ChatColor.GREEN + "的限制!");
                    return;
                }
                ConfigManager.getGlobalEntities().put(type, integer);
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.GREEN + "实体 " + type + ChatColor.GREEN + " 限制数量为: " + ChatColor.YELLOW + integer);
        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        UUID uniqueId = player.getUniqueId();
        if (SelectCommand.blockSelectors.contains(uniqueId)) {
            String type = ConfigManager.getBlockID(event.getClickedBlock());
            Integer integer = ConfigManager.getGlobalBlocks().get(type);
            int apply = 1;
            if (player.isSneaking()) apply = -1;
            if (integer == null) {
                if (apply == -1) {
                    player.sendMessage(ChatColor.GREEN + "方块 " + type + ChatColor.GREEN + " 没有限制");
                    event.setCancelled(true);
                    return;
                }
                ConfigManager.getGlobalBlocks().put(type, 1);
                integer = 1;
            } else {
                integer += apply;
                if (integer < 0) {
                    ConfigManager.getGlobalBlocks().remove(type);
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.GREEN + "已取消方块 " + type + ChatColor.GREEN + "的限制!");
                    return;
                }
                ConfigManager.getGlobalBlocks().put(type, integer);
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.GREEN + "方块 " + type + ChatColor.GREEN + " 限制数量为: " + ChatColor.YELLOW + integer);

        }
    }

}

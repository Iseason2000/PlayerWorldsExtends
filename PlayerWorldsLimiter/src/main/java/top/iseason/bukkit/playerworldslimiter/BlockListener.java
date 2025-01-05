package top.iseason.bukkit.playerworldslimiter;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlockListener implements Listener {

    private static Player spawningEgg = null;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (event.getPlayer().isOp()) {
            return;
        }
        World world = block.getWorld();
        String worldName = world.getName();
        if (PlayerWorldsLimiter.getOwnerUUID(worldName) == null) {
            return;
        }
        ItemStack item = event.getItemInHand();
        String type = ConfigManager.getItemID(item);

        if (ConfigManager.getGlobalBlocks().get(type) == null) {
            return;
        }

        ConfigManager.updateBlockTypeData(world, type);
        int max = addCount(block, false, worldName, type);
        if (max == -1) return;
        event.setCancelled(true);
        String blockMessage = ConfigManager.getBlockMessage();
        if (!blockMessage.isEmpty())
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', blockMessage.replace("%max%", String.valueOf(max))));
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
        String type = entity.toString().replace(" ", "_");
        Integer max = ConfigManager.getMax(name, type, "entity");
        if (max == null) return;
        ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> entityData = ConfigManager.getEntityData();
        if (entityData == null) return;
        ConcurrentHashMap<String, Integer> map = entityData.get(name);
        int count = 0;
        ConcurrentHashMap<String, Integer> data = map;
        if (map != null) {
            Integer current = map.get(type);
            if (current != null) {
                count = current;
            }
        } else {
            data = new ConcurrentHashMap<>();
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
        String type = entity.toString().replace(" ", "_");
        Integer max = ConfigManager.getMax(name, type, "entity");
        //说明不限制
        if (max == null) return;
        ConcurrentHashMap<String, Integer> map = ConfigManager.getEntityData().computeIfAbsent(name, k -> new ConcurrentHashMap<>());
        int count = 0;
        Integer current = map.get(type);
        if (current != null) {
            count = current;
        }
        count--;
        count = Math.max(count, 0);
        map.put(type, count);
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
            String type = event.getRightClicked().toString().replace(" ", "_");
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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlaceEvent2(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID uniqueId = player.getUniqueId();
        if (!SelectCommand.blockSelectors.contains(uniqueId)) return;

        System.out.println("Item NBT：" + NBT.itemStackToNBT(event.getItemInHand()));
//        System.out.println("Block NBT：" + NBT.get(event.getBlock().getState(), ReadableNBT::toString));
        String type = ConfigManager.getItemID(event.getItemInHand());

        Integer integer = ConfigManager.getGlobalBlocks().get(type);
        int apply = 1;
        if (player.isSneaking()) apply = -1;
        if (integer == null) {
            if (apply == -1) {
                player.sendMessage(ChatColor.YELLOW + "方块 " + ChatColor.GRAY + type + ChatColor.YELLOW + " 没有限制");
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
                player.sendMessage(ChatColor.GREEN + "已取消方块 " + ChatColor.YELLOW + type + ChatColor.GREEN + " 的限制!");
                return;
            }
            ConfigManager.getGlobalBlocks().put(type, integer);
        }
        player.sendMessage(ChatColor.GREEN + "方块 " + ChatColor.YELLOW + type + ChatColor.GREEN + " 限制数量为: " + ChatColor.AQUA + integer);
        Location location = event.getBlock().getLocation();
        Bukkit.getScheduler().runTaskLater(PlayerWorldsLimiter.getINSTANCE(), () -> {
            System.out.println("Block NBT：" + NBT.get(event.getBlock().getState(), ReadableNBT::toString));
            Block block = location.getBlock();
            if (block.isEmpty()) return;
            String type2 = ConfigManager.getBlockID(block);
            if (!ConfigManager.getIdMapper().containsKey(type2)) {
                ConfigManager.getIdMapper().put(type2, type);
                player.sendMessage(ChatColor.GREEN + "检测到方块在放下之后id为 " + ChatColor.YELLOW + type2 + ChatColor.GREEN + " 已自动添加该映射");
            }
        }, 20L);
    }

    public static void addBlock(Block block, String worldName, boolean force) {
        //只对家园生效
        String id = ConfigManager.getBlockID(block);
        String type = ConfigManager.getIdMapper().get(id);

        if (type == null) return;
        if (".*".equals(type)) {
            Collection<ItemStack> drops = block.getDrops();
            Iterator<ItemStack> iterator = drops.iterator();
            if (iterator.hasNext()) {
                ItemStack next = iterator.next();
                type = ConfigManager.getItemID(next);
            } else return;
        }

        addCount(block, force, worldName, type);
    }

    // 返回 max 达到限制 -1不限制
    private static int addCount(Block block, boolean force, String name, String type) {
        Integer max = ConfigManager.getMax(name, type, "block");
        if (max == null) return -1;
        ConcurrentHashMap<String, Set<Position>> map = ConfigManager
                .getBlockData()
                .computeIfAbsent(name, k -> new ConcurrentHashMap<>());
        Set<Position> current = map.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet());
        if (current.size() + 1 > max && !force) {
            return max;
        } else {
            Position position1 = Position.fromBlock(block);
            current.add(position1);
            return -1;
        }
    }

}

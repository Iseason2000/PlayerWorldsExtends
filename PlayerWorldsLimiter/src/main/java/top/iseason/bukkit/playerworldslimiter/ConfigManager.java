package top.iseason.bukkit.playerworldslimiter;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ConfigManager {
    @Getter
    private static ConcurrentHashMap<String, Integer> globalBlocks = null;
    @Getter
    private static ConcurrentHashMap<String, Integer> globalEntities = null;
    @Getter
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, Set<Position>>> blockData = null;
    @Getter
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> entityData = null;
    private static PlayerWorldsLimiter plugin;
    @Getter
    private static ConcurrentHashMap<UUID, List<String>> offlinePermissions = new ConcurrentHashMap<>();
    @Getter
    private static Map<String, String> idMapper = null;
    @Getter
    private static String blockMessage = "";
    @Getter
    private static String entityMessage = "";

    public static void loadConfig() {
        plugin = PlayerWorldsLimiter.getINSTANCE();
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        globalBlocks = loadMap(config.getConfigurationSection("global.blocks"));
        globalEntities = loadMap(config.getConfigurationSection("global.entities"));
        blockMessage = config.getString("block-message", "");
        entityMessage = config.getString("entity-message", "");
        idMapper = loadStringMap(config.getConfigurationSection("mapper"));
        plugin.getLogger().info("已加载 " + globalBlocks.size() + " 个全局方块限制");
        plugin.getLogger().info("已加载 " + globalEntities.size() + " 个全局实体限制");
    }

    public static void saveConfig() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection blocks = config.getConfigurationSection("global.blocks");
        if (blocks == null) blocks = config.createSection("global.blocks");
        ConfigurationSection finalBlocks = blocks;
        globalBlocks.forEach(finalBlocks::set);

        ConfigurationSection mapper = config.getConfigurationSection("mapper");
        if (mapper == null) mapper = config.createSection("mapper");
        idMapper.forEach(mapper::set);

        ConfigurationSection entities = config.getConfigurationSection("global.entities");
        if (entities == null) entities = config.createSection("global.entities");
        globalEntities.forEach(entities::set);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("配置保存异常");
        }
    }


    public static void loadData() {
        String pathStr = plugin.getDataFolder().toString() + File.separatorChar + "worldData";
        Path path = Paths.get(pathStr);
        File file1 = path.toFile();
        if (!file1.exists()) {
            file1.mkdir();
        }
        ConcurrentHashMap<String, ConcurrentHashMap<String, Set<Position>>> wMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> eMap = new ConcurrentHashMap<>();
        try (Stream<Path> paths = Files.walk(path)) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                                String fileName = file.getFileName().toString();
                                if (!fileName.endsWith(".yml")) return;
                                String worldName = fileName.replace(".yml", "");
                                YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
                                ConcurrentHashMap<String, Set<Position>> map = loadBlockMap(config.getConfigurationSection("blocks"));
                                wMap.put(worldName, map);
                                ConcurrentHashMap<String, Integer> ap2 = loadMap(config.getConfigurationSection("entities"));
                                eMap.put(worldName, ap2);
                            }
                    );
        } catch (Exception e) {
            e.printStackTrace();
        }
        blockData = wMap;
        entityData = eMap;
    }


    public static void loadOfflinePerm() {
        File file = new File(plugin.getDataFolder(), "offline_permissions.yml");
        if (!file.exists()) {
            plugin.saveResource("offline_permissions.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConcurrentHashMap<UUID, List<String>> wMap = new ConcurrentHashMap<>();
        for (String uuids : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuids);
                wMap.put(uuid, config.getStringList(uuids));
            } catch (Exception ignored) {
            }
        }
        offlinePermissions = wMap;
    }

    public static void savePlayerPermissionAsy(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> playerPermissions = getPlayerPermissions(player);
            if (playerPermissions.isEmpty()) return;
            UUID uniqueId = player.getUniqueId();
            File file = new File(plugin.getDataFolder(), "offline_permissions.yml");
            if (!file.exists()) {
                plugin.saveResource("offline_permissions.yml", false);
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set(uniqueId.toString(), playerPermissions);
            offlinePermissions.put(uniqueId, playerPermissions);
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("权限保存异常");
            }
        });
    }

    private static ConcurrentHashMap<String, Integer> loadMap(ConfigurationSection section) {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            map.put(key, section.getInt(key, 0));
        }
        return map;
    }

    private static Map<String, String> loadStringMap(ConfigurationSection section) {
        HashMap<String, String> map = new HashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            map.put(key, section.getString(key));
        }
        return map;
    }

    private static ConcurrentHashMap<String, Set<Position>> loadBlockMap(ConfigurationSection section) {
        ConcurrentHashMap<String, Set<Position>> map = new ConcurrentHashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            map.put(key, Position.fromStringList(section.getStringList(key)));
        }
        return map;
    }

    public static void saveData() {
        if (blockData == null) return;
        File file = getDataFile();
        blockData.forEach((worldName, dMap) -> {
            saveBlock(worldName, file, dMap);
        });
        entityData.forEach((worldName, dMap) -> {
            saveEntity(worldName, file, dMap);
        });

    }

    public static File getDataFile() {
        String pathStr = plugin.getDataFolder().toString() + File.separatorChar + "worldData";
        Path path = Paths.get(pathStr);
        File file = path.toFile();
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    public static void saveData(String worldName) {
        if (blockData == null) return;
        File file = getDataFile();
        ConcurrentHashMap<String, Set<Position>> blockMap = blockData.get(worldName);
        if (blockMap != null) {
            saveBlock(worldName, file, blockMap);
        }
        ConcurrentHashMap<String, Integer> entityData = ConfigManager.entityData.get(worldName);
        if (entityData != null) {
            saveEntity(worldName, file, entityData);
        }
    }

    private static void saveEntity(String worldName, File file1, ConcurrentHashMap<String, Integer> entityData) {
        File file = new File(file1, worldName + ".yml");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configuration.createSection("entities");
        entityData.forEach(section::set);
        try {
            configuration.save(file);
        } catch (IOException ignored) {
        }
    }

    private static void saveBlock(String worldName, File file1, ConcurrentHashMap<String, Set<Position>> blockMap) {
        File file = new File(file1, worldName + ".yml");
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection section = configuration.createSection("blocks");
        blockMap.forEach((k, p) -> {
            ArrayList<String> strings = new ArrayList<>();
            for (Position position : p) {
                strings.add(position.toString());
            }
            section.set(k, strings);
        });
        try {
            configuration.save(file);
        } catch (IOException ignored) {
        }
    }

    public static boolean canUpdate(World world) {
        return blockData.containsKey(world.getName());
    }


    public static void updateBlockData(World world) {
        ConcurrentHashMap<String, Set<Position>> map = blockData.get(world.getName());
        if (map == null) return;
        map.forEach((k, pos) ->
                pos.removeIf(
                        p -> {
                            String blockID = getBlockID(world.getBlockAt(p.getX(), p.getY(), p.getZ()));
                            String type = ConfigManager.getIdMapper().get(blockID);
                            return !k.equals(type);
                        }
                )
        );
    }

    public static void updateBlockTypeData(World world, String type) {
        ConcurrentHashMap<String, Set<Position>> map = blockData.get(world.getName());
        if (map == null) return;
        Set<Position> positions = map.get(type);
        if (positions == null) return;
        positions.removeIf(
                p -> {
                    Block blockAt = world.getBlockAt(p.getX(), p.getY(), p.getZ());
                    String blockID = getBlockID(blockAt);
//                    if ("nae2:crafting_storage".equals(blockID)) return false; //ae储存方块所有数据都一样，很傻逼
//                    if ("appliedenergistics2:crafting_storage".equals(blockID)) return false; //ae储存方块所有数据都一样，很傻逼
                    String mapper = ConfigManager.getIdMapper().get(blockID);
                    if (".*".equals(mapper)) {
                        Collection<ItemStack> drops = blockAt.getDrops();
                        Iterator<ItemStack> iterator = drops.iterator();
                        if (iterator.hasNext()) {
                            ItemStack next = iterator.next();
                            mapper = getItemID(next);
                        }
                    }
                    return !type.equals(mapper);
                }
        );
    }

    public static void updateBlockDataAsync(World world) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> updateBlockData(world));
    }

    public static void runAsynchronously(Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static void runSync(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public static void loadConfigAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, ConfigManager::loadConfig);
    }

    public static void loadOfflinePermAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, ConfigManager::loadOfflinePerm);
    }

    public static void saveConfigAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, ConfigManager::saveConfig);
    }

    public static void loadDataAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, ConfigManager::loadData);
    }

    public static void saveDataAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, ConfigManager::saveData);
    }

    public static List<String> getPlayerPermissions(Player player) {
        ArrayList<String> permissions = new ArrayList<>();
        for (PermissionAttachmentInfo effectivePermission : player.getEffectivePermissions()) {
            String permission = effectivePermission.getPermission();
            if (permission.startsWith("playerworldslimiter")) {
                permissions.add(permission);
            }
        }
        return permissions;
    }

    /**
     * @param worldName 世界名字
     * @param type      材料/实体类型
     * @param pre       类型 block 或者 entity
     * @return 如果有则返回，没有限制返回null
     */
    public static Integer getMax(String worldName, String type, String pre) {
        String ownerUUID = PlayerWorldsLimiter.getOwnerUUID(worldName);
        if (ownerUUID == null) return null;
        UUID uuid;
        try {
            uuid = UUID.fromString(ownerUUID);
        } catch (Exception e) {
            return null;
        }
        OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(uuid);
        if (offlinePlayer.isOnline()) {
            for (PermissionAttachmentInfo effectivePermission : offlinePlayer.getPlayer().getEffectivePermissions()) {
                String permission = effectivePermission.getPermission();
                Integer m = matchMax(permission, pre, type);
                if (m != null) return m;
            }
            //没有权限
        } else {
            //不在线
            List<String> strings = ConfigManager.getOfflinePermissions().get(uuid);
            if (strings != null) {
                for (String string : strings) {
                    Integer m = matchMax(string, pre, type);
                    if (m != null) return m;
                }
            }
            //没有权限
        }
        if ("block".equals(pre))
            return ConfigManager.getGlobalBlocks().get(type);
        else return ConfigManager.getGlobalEntities().get(type);
    }


    public static Integer matchMax(String perm, String prefix, String type) {
        String p = "playerworldslimiter." + prefix;
        if (perm.startsWith(p + ".all.")) {
            //匹配全部
            //  playerworldslimiter.block.all.5
            String replace = perm.replace(p + ".all.", "");
            try {
                return Integer.parseInt(replace);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        String pre = p + "." + type + ".";
        if (perm.startsWith(pre)) {
            String replace = perm.replace(pre, "");
            try {
                return Integer.parseInt(replace);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static String getBlockID(Block block) {
        try {
            if (block.isEmpty()) return "air";
            return NBT.get(block.getState(), it -> {
                StringBuilder id = new StringBuilder(it.getString("id"));
                if (it.hasTag("parentMachine")) id.append("_").append(it.getString("parentMachine"));

                return id.toString();
            });
        } catch (Exception e) {
            byte data = block.getData();
            String lowerCase = block.getType().toString().toLowerCase();
            if (data != 0) return lowerCase + "_" + data;

            return lowerCase;
        }
    }

    public static String getItemID(ItemStack item) {
        try {
            if (item.getType() == Material.AIR) return "air";
            ReadWriteNBT readWriteNBT = NBT.itemStackToNBT(item);
            String id = readWriteNBT.getString("id");
            Short damage = readWriteNBT.getShort("Damage");
            if (damage > 0) return id + "_" + damage;
            return id;
        } catch (Exception e) {
            String lowerCase = item.getType().toString().toLowerCase();
            byte data = item.getData().getData();
            if (data != 0) return lowerCase + "_" + data;
            return lowerCase;
        }

    }

    public static void scanAll(CommandSender sender) {
        long l = System.currentTimeMillis();
        sender.sendMessage(ChatColor.YELLOW + "开始扫描所有世界...");
        for (World world : Bukkit.getWorlds()) {
            scanWorld(world);
        }
        saveData();
        sender.sendMessage(ChatColor.GREEN + "扫描结束,耗时: " + ChatColor.GREEN + (System.currentTimeMillis() - l) + " 毫秒");
    }

    public static void scanWorld(World world) {
        String worldName = world.getName();
        String ownerUUID = PlayerWorldsLimiter.getOwnerUUID(worldName);
        if (ownerUUID == null) return;
        ConcurrentHashMap<String, Set<Position>> map = ConfigManager.getBlockData().get(worldName);
        if (map != null) {
            map.clear();
        }

        for (Chunk loadedChunk : world.getLoadedChunks()) {
            for (int x = 0; x <= 15; x++) {
                for (int y = 0; y <= 255; y++) {
                    for (int z = 0; z <= 15; z++) {
                        Block block = loadedChunk.getBlock(x, y, z);
                        if (block.isEmpty() || block.isLiquid()) continue;
                        BlockListener.addBlock(block, worldName, true);
                    }
                }
            }
        }
    }

    public static void scanWorld(World world, CommandSender sender) {
        long l = System.currentTimeMillis();
        sender.sendMessage(ChatColor.YELLOW + "开始扫描世界...");
        scanWorld(world);
        saveData(world.getName());
        sender.sendMessage(ChatColor.GREEN + "扫描结束,耗时: " + ChatColor.GREEN + (System.currentTimeMillis() - l) + " 毫秒");
    }
}

package top.iseason.bukkit.playerworldslimiter;

import com.tuershen.nbtlibrary.NBTLibraryMain;
import com.tuershen.nbtlibrary.api.NBTTagCompoundApi;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class ConfigManager {
    @Getter
    private static HashMap<String, Integer> globalBlocks = null;
    @Getter
    private static HashMap<String, Integer> globalEntities = null;
    @Getter
    private static HashMap<String, HashMap<String, List<Position>>> blockData = null;
    @Getter
    private static HashMap<String, HashMap<String, Integer>> entityData = null;
    private static PlayerWorldsLimiter plugin;
    @Getter
    private static HashMap<UUID, List<String>> offlinePermissions = new HashMap<>();
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
        HashMap<String, HashMap<String, List<Position>>> wMap = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> eMap = new HashMap<>();
        try (Stream<Path> paths = Files.walk(path)) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                                String fileName = file.getFileName().toString();
                                if (!fileName.endsWith(".yml")) return;
                                String worldName = fileName.replace(".yml", "");
                                YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
                                HashMap<String, List<Position>> map = loadBlockMap(config.getConfigurationSection("blocks"));
                                wMap.put(worldName, map);
                                HashMap<String, Integer> ap2 = loadMap(config.getConfigurationSection("entities"));
                                eMap.put(worldName, ap2);
                            }
                    );
        } catch (IOException ignored) {
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
        HashMap<UUID, List<String>> wMap = new HashMap<>();
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

    private static HashMap<String, Integer> loadMap(ConfigurationSection section) {
        HashMap<String, Integer> map = new HashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            map.put(key, section.getInt(key, 0));
        }
        return map;
    }

    private static HashMap<String, List<Position>> loadBlockMap(ConfigurationSection section) {
        HashMap<String, List<Position>> map = new HashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            map.put(key, Position.fromStringList(section.getStringList(key)));
        }
        return map;
    }

    public static void saveData() {
        if (blockData == null) return;
        String pathStr = plugin.getDataFolder().toString() + File.separatorChar + "worldData";
        Path path = Paths.get(pathStr);
        File file1 = path.toFile();
        if (!file1.exists()) {
            file1.mkdir();
        }
        blockData.forEach((worldName, dMap) -> {
            File file = new File(file1, worldName + ".yml");
            YamlConfiguration configuration = new YamlConfiguration();
            ConfigurationSection section = configuration.createSection("blocks");
            dMap.forEach((k, p) -> {
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
        });
        entityData.forEach((worldName, dMap) -> {
            File file = new File(file1, worldName + ".yml");
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = configuration.createSection("entities");
            dMap.forEach(section::set);
            try {
                configuration.save(file);
            } catch (IOException ignored) {
            }
        });

    }

    public static void updateBlockData(World world) {
        HashMap<String, List<Position>> map = blockData.get(world.getName());
        if (map == null) return;
        map.forEach((k, pos) -> pos.removeIf(p -> !k.equals(getBlockID(world.getBlockAt(p.getX(), p.getY(), p.getZ())))));
    }

    public static void updateBlockDataAsync(World world) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> updateBlockData(world));
    }

    public static void runAsynchronously(Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
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
            NBTTagCompoundApi api = NBTLibraryMain.libraryApi.getTileEntityCompoundApi(block).getNBTTagCompound();
//            NBTImp_v1_12_R1 ser = (NBTImp_v1_12_R1)api;
//            System.out.println(ser.deserializeNBTTagCompound(api.getNBTTagCompoundApi()));
//            System.out.println(block.getType());
//            System.out.println(block.getTypeId());
            if (api.hasKey("id")) {
                if (api.hasKey("subTileName")) {
                    return api.getString("id") + "_" + api.getString("subTileName");
                } else if (api.hasKey("def:0")) {
                    NBTTagCompoundApi compound = api.getCompound("def:0");
                    if (compound.hasKey("Damage")) return api.getString("id") + "_" + compound.getByte("Damage");
                }
                String id = api.getString("id").replace(":", "_");
                String s = block.getType().toString().toLowerCase();
                if (s.startsWith(id) && s.length() != id.length()) return s;
                if (block.getData() != 0) return api.getString("id") + "_" + block.getData();
                return id;
            } else return block.getType().toString().toLowerCase();
        } catch (Exception e) {
            return block.getType().toString().toLowerCase();
        }
    }

    public static void scanAll(CommandSender sender) {
        long l = System.currentTimeMillis();
        sender.sendMessage(ChatColor.YELLOW + "开始扫描所有世界...");
        for (World world : Bukkit.getWorlds()) {
            scanWorld(world);
        }
        sender.sendMessage(ChatColor.GREEN + "扫描结束,耗时: " + ChatColor.GREEN + (System.currentTimeMillis() - l) + " 毫秒");
    }

    public static void scanWorld(World world) {
        String ownerUUID = PlayerWorldsLimiter.getOwnerUUID(world.getName());
        if (ownerUUID == null) return;
        for (Chunk loadedChunk : world.getLoadedChunks()) {
            for (int x = 0; x <= 15; x++) {
                for (int y = 0; y <= 255; y++) {
                    for (int z = 0; z <= 15; z++) {
                        Block block = loadedChunk.getBlock(x, y, z);
                        if (block.isEmpty() || block.isLiquid()) continue;
                        BlockListener.addBlock(block, true);
                    }
                }
            }
        }
    }

    public static void scanWorld(World world, CommandSender sender) {
        long l = System.currentTimeMillis();
        sender.sendMessage(ChatColor.YELLOW + "开始扫描世界...");
        scanWorld(world);
        sender.sendMessage(ChatColor.GREEN + "扫描结束,耗时: " + ChatColor.GREEN + (System.currentTimeMillis() - l) + " 毫秒");
    }
}

package top.iseason.bukkit.playerworldslimiter;

import com.tuershen.nbtlibrary.NBTLibraryMain;
import com.tuershen.nbtlibrary.api.NBTTagCompoundApi;
import com.tuershen.nbtlibrary.common.difference.NBTImp_v1_12_R1;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ConfigManager {
    @Getter
    private static HashMap<String, Integer> globalBlocks = null;
    @Getter
    private static HashMap<String, Integer> globalEntities = null;
    @Getter
    private static HashMap<String, HashMap<String, Integer>> blockData = null;
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
        File file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            plugin.saveResource("data.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        HashMap<String, HashMap<String, Integer>> wMap = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> eMap = new HashMap<>();
        for (String worldName : config.getKeys(false)) {
            HashMap<String, Integer> map = loadMap(config.getConfigurationSection(worldName + ".blocks"));
            wMap.put(worldName, map);
            HashMap<String, Integer> ap2 = loadMap(config.getConfigurationSection(worldName + ".entities"));
            eMap.put(worldName, ap2);
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

    public static void saveData() {
        if (blockData == null) return;
        File file = new File(plugin.getDataFolder(), "data.yml");
        YamlConfiguration configuration = new YamlConfiguration();
        blockData.forEach((worldName, dMap) -> {
            ConfigurationSection section = configuration.createSection(worldName + ".blocks");
            dMap.forEach(section::set);
        });
        entityData.forEach((worldName, dMap) -> {
            ConfigurationSection section = configuration.createSection(worldName + ".entities");
            dMap.forEach(section::set);
        });
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("数据保存异常!");
        }
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
            if ("block".equals(pre))
                return ConfigManager.getGlobalBlocks().get(type);
            else return ConfigManager.getGlobalEntities().get(type);
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
            if ("block".equals(pre))
                return ConfigManager.getGlobalBlocks().get(type);
            else return ConfigManager.getGlobalEntities().get(type);
        }
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
}

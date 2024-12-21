package top.iseason.bukkit.pwpdifficulty;

import cz.heroify.playerworldspro.Main;
import cz.heroify.playerworldspro.api.API;

import cz.heroify.playerworldspro.utils.ConfigManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PWPDifficulty extends JavaPlugin implements Listener {
    private static List<Pattern> globalList = new ArrayList<>();
    private static List<Pattern> naturalList = new ArrayList<>();
    public static YamlConfiguration config;
    private static Main pwpMain = null;
    private static PWPDifficulty INSTANCE;

    public static void reload() {
        File file = new File(INSTANCE.getDataFolder(), "config.yml");
        if (!file.exists()) {
            INSTANCE.saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(file);
        globalList = config.getStringList("global-whitelist").stream().map(Pattern::compile).collect(Collectors.toList());
        naturalList = config.getStringList("natural-whitelist").stream().map(Pattern::compile).collect(Collectors.toList());
    }

    public static void save() {
        config.set("global-whitelist", globalList.stream().map(Pattern::toString).collect(Collectors.toList()));
        config.set("natural-whitelist", naturalList.stream().map(Pattern::toString).collect(Collectors.toList()));
        try {
            config.save(new File(INSTANCE.getDataFolder(), "config.yml"));
        } catch (IOException ignored) {
        }
    }

    public static String getOwnerUUID(String worldName) {
        return pwpMain.getBasicManager().getUUIDFromWorldName(worldName);
    }

    public static void setDifficulties(Difficulty difficulty) {
        Bukkit.getServer().getScheduler().runTaskAsynchronously(INSTANCE, () -> {
            for (World world : Bukkit.getWorlds()) {
                setDifficulty(world.getName(), difficulty);
            }
            pwpMain.getConfig().set("Default-Settings.Difficulty", difficulty.toString());
            pwpMain.saveConfig();
        });
    }

    private static void setDifficulty(String worldName, Difficulty difficulty) {
        String ownerUUID = getOwnerUUID(worldName);
        if (ownerUUID == null) {
            return;
        }
        if (difficulty == null) return;
        ConfigManager configManager = pwpMain.getConfigManager();
        FileConfiguration config = configManager.getConfig(ConfigManager.Configurations.DATA);
        config.set("Worlds." + ownerUUID + ".1.Difficulty", difficulty.toString());
        configManager.saveConfig(ConfigManager.Configurations.DATA);

        for (final String worlds : pwpMain.getWorldManager().getAvailableWorlds(worldName)) {
            final World world = Bukkit.getWorld(worlds);
            if (world != null) {
                world.setDifficulty(difficulty);
            }
        }

    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        INSTANCE = this;
        pwpMain = (Main) getServer().getPluginManager().getPlugin("PlayerWorldsPro");
        if (pwpMain == null) {
            getLogger().warning("前置 PlayerWorldsPro 不存在，注销插件");
            onDisable();
            return;
        }
        Bukkit.getPluginCommand("pwpdifficulty").setExecutor(new MainCommand());
        Bukkit.getPluginCommand("entitySelector").setExecutor(new SelectCommand());
        reload();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> {
                    for (World world : Bukkit.getWorlds()) {
                        String name = world.getName();
                        String uuid = getOwnerUUID(name);
                        if (uuid == null) {
                            continue;
                        }
                        updatedWorld(world, uuid);
                    }
                }, 20L, 20L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldLoadEvent(final WorldLoadEvent event) {
        World world = event.getWorld();
        String uuid = getOwnerUUID(world.getName());
        if (uuid == null) {
            return;
        }
        updatedWorld(world, uuid);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleportEvent(final PlayerTeleportEvent event) {
        Location to = event.getTo();
        World world = to.getWorld();
        String uuid = getOwnerUUID(world.getName());
        if (uuid == null) {
            return;
        }
        updatedWorld(world, uuid);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        String type = event.getEntity().toString();
        if (getOwnerUUID(event.getLocation().getWorld().getName()) == null) {
            return;
        }

        if ((event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CHUNK_GEN) && !isMatch(naturalList, type)) {
            event.setCancelled(true);
            return;
        }
        if (!isMatch(globalList, type)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        UUID uniqueId = player.getUniqueId();
        if (SelectCommand.blackSelectors.contains(uniqueId)) {
            String type = event.getRightClicked().toString();
            globalList.add(Pattern.compile("^" + type.replace("{", "\\{").replace("}", "\\}") + "$"));
            player.sendMessage(ChatColor.GREEN + "实体 " + type + " 已添加进黑名单");
        } else if (SelectCommand.whiteSelectors.contains(uniqueId)) {
            String type = event.getRightClicked().toString();
            naturalList.add(Pattern.compile("^" + type + "$"));
            player.sendMessage(ChatColor.GREEN + "实体 " + type + " 已添加进白名单");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public boolean isMatch(List<Pattern> patterns, String entity) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(entity).find()) return true;
        }
        return false;
    }

    private void updatedWorld(World world, String uuid) {
        resetDifficulty(world, uuid);
        resetBorder(world);
    }

    private void resetDifficulty(World world, String uuid) {
        String difficulty = API.getDifficulty(uuid);
        if (!world.getDifficulty().toString().equals(difficulty)) {
            Difficulty d = Difficulty.PEACEFUL;
            try {
                d = Difficulty.valueOf(difficulty);
            } catch (IllegalArgumentException ignored) {
            }
            world.setDifficulty(d);
        }
    }

    private void resetBorder(World world) {
        WorldBorder worldBorder = world.getWorldBorder();
        double size = worldBorder.getSize();
        worldBorder.setSize(size);
        worldBorder.setCenter(0.0, 0.0);
    }
}

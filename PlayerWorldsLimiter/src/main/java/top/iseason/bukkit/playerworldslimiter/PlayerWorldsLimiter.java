package top.iseason.bukkit.playerworldslimiter;

import cz._heropwp.playerworldspro.Main;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerWorldsLimiter extends JavaPlugin {
    @Getter
    private static PlayerWorldsLimiter INSTANCE;
    private static Main pwpMain = null;

    public static String getOwnerUUID(String worldName) {
        if (pwpMain == null) return null;
        return pwpMain.B().a(worldName);
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
        ConfigManager.loadConfig();
        ConfigManager.loadDataAsync();
        ConfigManager.loadOfflinePerm();
        Bukkit.getPluginCommand("limitSelector").setExecutor(new SelectCommand());
        Bukkit.getPluginCommand("playerworldslimiter").setExecutor(new ReloadCommand());
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        ConfigManager.saveData();
    }

}

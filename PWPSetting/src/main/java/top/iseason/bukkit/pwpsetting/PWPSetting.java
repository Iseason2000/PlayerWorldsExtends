package top.iseason.bukkit.pwpsetting;

import cz.heroify.playerworldspro.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PWPSetting extends JavaPlugin implements CommandExecutor {
    public static YamlConfiguration config;
    private static Main pwpMain = null;

    public static PWPSetting getINSTANCE() {
        return INSTANCE;
    }

    private static PWPSetting INSTANCE;

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
        reload();
        Bukkit.getServer().getPluginManager().registerEvents(new Listener(), this);
        Bukkit.getPluginCommand("PWPSetting").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void reload() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static String getOwnerUUID(String worldName) {
        return pwpMain.getBasicManager().getUUIDFromWorldName(worldName);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("pwpsetting.reload")) return true;
        reload();
        sender.sendMessage(ChatColor.GREEN + "[PWPSetting]配置已重载!");
        return true;
    }

}

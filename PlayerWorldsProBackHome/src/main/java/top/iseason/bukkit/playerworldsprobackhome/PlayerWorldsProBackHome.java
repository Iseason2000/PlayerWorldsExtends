package top.iseason.bukkit.playerworldsprobackhome;

import cz._heropwp.playerworldspro.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerWorldsProBackHome extends JavaPlugin implements CommandExecutor {
    private static Main pwpMain = null;

    @Override
    public void onEnable() {
        // Plugin startup logic
        pwpMain = (Main) getServer().getPluginManager().getPlugin("PlayerWorldsPro");
        if (pwpMain == null) {
            getLogger().warning("前置 PlayerWorldsPro 不存在，注销插件");
            onDisable();
            return;
        }
        getServer().getPluginCommand("PlayerWorldsProBackHome").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        if (args.length >= 1 && sender.isOp()) {
            player = Bukkit.getPlayer(args[0]);
        }
        if (player == null && sender instanceof Player) {
            player = (Player) sender;
        }
        if (player == null) return true;
        String uuid = player.getUniqueId().toString();
        if (!pwpMain.G().d(uuid)) {
            return true;
        }
        pwpMain.G().a(player, uuid, true);
        return true;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

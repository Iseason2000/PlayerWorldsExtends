package top.iseason.bukkit.playerworldslimiter;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.isOp()) {
            ConfigManager.loadConfigAsync();
            ConfigManager.loadOfflinePermAsync();
            sender.sendMessage(ChatColor.GREEN + "插件配置已重载!");
        }
        return true;
    }

}

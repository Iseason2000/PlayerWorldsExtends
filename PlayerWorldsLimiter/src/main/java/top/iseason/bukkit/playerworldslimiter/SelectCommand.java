package top.iseason.bukkit.playerworldslimiter;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class SelectCommand implements CommandExecutor, TabCompleter {
    public static final HashSet<UUID> blockSelectors = new HashSet<>();
    public static final HashSet<UUID> entitySelectors = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return false;
        String arg = args[0];
        if (!sender.isOp() && !sender.hasPermission("pwpdifficulty.admin")) return true;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该命令只能玩家使用");
            return true;
        }
        Player player = (Player) sender;
        UUID uniqueId = player.getUniqueId();
        if ("block".equals(arg)) {
            if (blockSelectors.contains(uniqueId)) {
                blockSelectors.remove(uniqueId);
                sender.sendMessage(ChatColor.YELLOW + "方块选择模式已关闭");
                ConfigManager.saveConfigAsync();
            } else {
                entitySelectors.remove(uniqueId);
                blockSelectors.add(uniqueId);
                sender.sendMessage(ChatColor.GREEN + "方块选择模式已开启");
            }
            return true;
        }
        if ("entity".equals(arg)) {
            if (entitySelectors.contains(uniqueId)) {
                entitySelectors.remove(uniqueId);
                sender.sendMessage(ChatColor.YELLOW + "实体选择模式已关闭");
                ConfigManager.saveConfigAsync();
            } else {
                blockSelectors.remove(uniqueId);
                entitySelectors.add(uniqueId);
                sender.sendMessage(ChatColor.GREEN + "实体选择模式已开启");
            }
            return true;
        }
        if ("update".equals(arg)) {
            ConfigManager.updateBlockDataAsync(player.getWorld());
            sender.sendMessage(ChatColor.GREEN + "已更新当前世界的黑名单方块数量");
        }
        if ("scan".equals(arg)) {
            ConfigManager.runSync(() -> ConfigManager.scanWorld(player.getWorld(), player));
        }
        if ("scanAll".equals(arg)) {
            ConfigManager.runSync(() -> ConfigManager.scanAll(player));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("block");
            list.add("entity");
            list.add("update");
            list.add("scan");
            list.add("scanAll");
            list.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return list;
        }
        return null;
    }
}

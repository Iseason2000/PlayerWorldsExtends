package top.iseason.bukkit.pwpdifficulty;

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
    public static final HashSet<UUID> whiteSelectors = new HashSet<>();
    public static final HashSet<UUID> blackSelectors = new HashSet<>();

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
        if ("white".equals(arg)) {
            if (whiteSelectors.contains(uniqueId)) {
                whiteSelectors.remove(uniqueId);
                sender.sendMessage(ChatColor.YELLOW + "白名单选择模式已关闭");
                PWPDifficulty.save();
            } else {
                blackSelectors.remove(uniqueId);
                whiteSelectors.add(uniqueId);
                sender.sendMessage(ChatColor.GREEN + "白名单选择模式已开启");
            }
            return true;
        }
        if ("black".equals(arg)) {
            if (blackSelectors.contains(uniqueId)) {
                blackSelectors.remove(uniqueId);
                sender.sendMessage(ChatColor.YELLOW + "黑名单选择模式已关闭");
                PWPDifficulty.save();
            } else {
                whiteSelectors.remove(uniqueId);
                blackSelectors.add(uniqueId);
                sender.sendMessage(ChatColor.GREEN + "黑名单选择模式已开启");
            }
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("white");
            list.add("black");
            list.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return list;
        }
        return null;
    }
}

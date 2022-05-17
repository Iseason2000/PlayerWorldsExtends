package top.iseason.bukkit.pwpdifficulty;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String str, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("setall");
            list.add("reload");
            list.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return list;
        } else if (args.length == 2 && args[0].equals("setall")) {
            List<String> list2 = new ArrayList<>();
            list2.add("PEACEFUL");
            list2.add("EASY");
            list2.add("HARD");
            list2.add("NORMAL");
            list2.removeIf(s -> !s.startsWith(args[1].toLowerCase()));
            return list2;
        } else {
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (!sender.isOp() && !sender.hasPermission("pwpdifficulty.admin")) return true;
        if (args[0].equals("setall")) {
            if (args.length != 2) {
                return false;
            }
            Difficulty difficulty = null;
            String dif = args[1].toUpperCase();
            try {
                difficulty = Difficulty.valueOf(dif);
            } catch (IllegalArgumentException ignored) {
            }
            if (difficulty == null) {
                sender.sendMessage("请输入正确的难度");
                return true;
            }

            PWPDifficulty.setDifficulties(difficulty);
            sender.sendMessage("家园难度已设置为: " + difficulty);
            return true;
        } else if (args[0].equals("reload")) {
            PWPDifficulty.reload();
            sender.sendMessage(ChatColor.GREEN + "[PWPDifficulty]配置已重载!");
        }
        return true;
    }
}

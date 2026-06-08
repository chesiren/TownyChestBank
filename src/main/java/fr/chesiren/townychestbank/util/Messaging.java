package fr.chesiren.townychestbank.util;

import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Messaging {

    public static void send(CommandSender sender, String key, Object... args) {
        sender.sendMessage(prefix() + Translatable.of(key, args).forLocale(sender));
    }

    public static void sendError(CommandSender sender, String key, Object... args) {
        sender.sendMessage(prefix() + ChatColor.RED + Translatable.of(key, args).forLocale(sender));
    }

    public static void sendSuccess(CommandSender sender, String key, Object... args) {
        sender.sendMessage(prefix() + ChatColor.GREEN + Translatable.of(key, args).forLocale(sender));
    }

    public static void sendInfo(CommandSender sender, String key, Object... args) {
        sender.sendMessage(prefix() + ChatColor.YELLOW + Translatable.of(key, args).forLocale(sender));
    }

    public static String t(CommandSender sender, String key, Object... args) {
        return Translatable.of(key, args).forLocale(sender);
    }

    public static String tDefault(String key, Object... args) {
        return Translatable.of(key, args).defaultLocale();
    }

    private static String prefix() {
        return ChatColor.GRAY + "[" + ChatColor.GOLD + "TownyChestBank" + ChatColor.GRAY + "] " + ChatColor.RESET;
    }
}

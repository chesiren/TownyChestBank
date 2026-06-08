package fr.chesiren.townychestbank.command;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.WorldCoord;
import fr.chesiren.townychestbank.TownyChestBankPlugin;
import fr.chesiren.townychestbank.util.Messaging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class SetBankChestCommand implements CommandExecutor, TabCompleter {

    private final TownyChestBankPlugin plugin;

    public SetBankChestCommand(TownyChestBankPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messaging.sendError(sender, "tcb_player_only");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "set":    cmdSet(player);    break;
            case "remove": cmdRemove(player); break;
            case "info":   cmdInfo(player);   break;
            default:       sendHelp(player);  break;
        }
        return true;
    }

    private void cmdSet(Player player) {
        if (!player.hasPermission("townychestbank.set") && !player.hasPermission("townychestbank.admin")) {
            Messaging.sendError(player, "tcb_no_permission");
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.CHEST) {
            Messaging.sendError(player, "tcb_no_chest");
            return;
        }

        Town town = getTownOfPlayer(player);
        if (town == null) return;

        if (!player.hasPermission("townychestbank.admin") && !isMayorOrAssistant(player, town)) {
            Messaging.sendError(player, "tcb_no_permission");
            return;
        }

        if (!isValidChestLocation(target.getLocation(), town)) {
            Messaging.sendError(player, "tcb_not_homeblock");
            return;
        }

        boolean moving = plugin.getChestBankManager().getChestForTown(town.getUUID()) != null;
        plugin.getChestBankManager().setChestForTown(town.getUUID(), target.getLocation());
        if (moving) {
            Messaging.sendSuccess(player, "tcb_chest_moved", town.getName());
        } else {
            Messaging.sendSuccess(player, "tcb_chest_set", town.getName());
        }
    }

    private void cmdRemove(Player player) {
        Town town = getTownOfPlayer(player);
        if (town == null) return;

        if (!player.hasPermission("townychestbank.admin") && !isMayorOrAssistant(player, town)) {
            Messaging.sendError(player, "tcb_no_permission");
            return;
        }

        plugin.getChestBankManager().removeChestForTown(town.getUUID());
        Messaging.sendInfo(player, "tcb_chest_removed", town.getName());
    }

    private void cmdInfo(Player player) {
        Town town = getTownOfPlayer(player);
        if (town == null) return;

        Location loc = plugin.getChestBankManager().getChestForTown(town.getUUID());
        if (loc == null) {
            Messaging.sendError(player, "tcb_no_bank_chest");
            return;
        }

        Messaging.sendInfo(player, "tcb_info_header", town.getName());
        Messaging.send(player, "tcb_info_world", loc.getWorld().getName());
        Messaging.send(player, "tcb_info_position",
            String.valueOf(loc.getBlockX()),
            String.valueOf(loc.getBlockY()),
            String.valueOf(loc.getBlockZ()));
        try {
            double balance = town.getAccount().getHoldingBalance();
            Messaging.send(player, "tcb_info_balance", String.format("%.2f", balance));
        } catch (Exception ignored) {}
    }

    private Town getTownOfPlayer(Player player) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown()) {
                Messaging.sendError(player, "tcb_not_in_town");
                return null;
            }
            return resident.getTown();
        } catch (NotRegisteredException e) {
            Messaging.sendError(player, "tcb_not_in_town");
            return null;
        }
    }

    private boolean isMayorOrAssistant(Player player, Town town) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null) return false;
            return town.getMayor().equals(resident) || resident.hasTownRank("assistant");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidChestLocation(Location loc, Town town) {
        try {
            WorldCoord wc = WorldCoord.parseWorldCoord(loc);
            TownBlock tb = wc.getTownBlock();
            Town blockTown = tb.getTownOrNull();
            if (blockTown == null || !blockTown.getUUID().equals(town.getUUID())) return false;
            return tb.equals(town.getHomeBlock()) || tb.getType().equals(TownBlockType.BANK);
        } catch (Exception e) {
            return false;
        }
    }

    private void sendHelp(Player player) {
        Messaging.sendInfo(player, "tcb_aide_titre");
        Messaging.send(player, "tcb_aide_set");
        Messaging.send(player, "tcb_aide_remove");
        Messaging.send(player, "tcb_aide_info");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("set", "remove", "info");
        return java.util.Collections.emptyList();
    }
}

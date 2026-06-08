package fr.chesiren.townychestbank.listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import fr.chesiren.townychestbank.TownyChestBankPlugin;
import fr.chesiren.townychestbank.util.Messaging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class BankChestListener implements Listener {

    private final TownyChestBankPlugin plugin;

    public BankChestListener(TownyChestBankPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;
        if (!plugin.getChestBankManager().isBankChest(block.getLocation())) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!hasSwitchPermission(player, block.getLocation())) {
            Messaging.sendError(player, "tcb_no_towny_permission");
            return;
        }

        Town town = plugin.getChestBankManager().getTownForChest(block.getLocation());
        if (town == null) return;
        plugin.getBankChestGUI().openForPlayer(player, town);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.getChestBankManager().isBankChest(block.getLocation())) return;

        Player player = event.getPlayer();
        if (player.hasPermission("townychestbank.admin")) {
            plugin.getChestBankManager().removeChestForTown(
                plugin.getChestBankManager().getTownUUIDForChest(block.getLocation())
            );
            Messaging.sendInfo(player, "tcb_chest_removed_short");
            return;
        }

        UUID townUUID = plugin.getChestBankManager().getTownUUIDForChest(block.getLocation());
        if (townUUID != null) {
            try {
                Town town = TownyUniverse.getInstance().getTown(townUUID);
                if (town != null) {
                    Resident resident = TownyAPI.getInstance().getResident(player);
                    if (resident != null && town.getMayor().equals(resident)) {
                        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
                        plugin.getChestBankManager().dropBankContents(townUUID, dropLoc);
                        plugin.getChestBankManager().removeChestForTown(townUUID);
                        Messaging.sendInfo(player, "tcb_chest_destroyed_warning");
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        event.setCancelled(true);
        Messaging.sendError(player, "tcb_no_permission");
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b ->
            b.getType() == Material.CHEST && plugin.getChestBankManager().isBankChest(b.getLocation())
        );
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b ->
            b.getType() == Material.CHEST && plugin.getChestBankManager().isBankChest(b.getLocation())
        );
    }

    public static boolean hasSwitchPermission(Player player, Location chestLoc) {
        if (player.hasPermission("townychestbank.admin")) return true;
        try {
            return PlayerCacheUtil.getCachePermission(player, chestLoc, Material.CHEST, TownyPermission.ActionType.SWITCH);
        } catch (Exception e) {
            return false;
        }
    }
}

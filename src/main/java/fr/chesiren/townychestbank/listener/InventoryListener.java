package fr.chesiren.townychestbank.listener;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import fr.chesiren.townychestbank.TownyChestBankPlugin;
import fr.chesiren.townychestbank.config.PluginConfig;
import fr.chesiren.townychestbank.gui.BankChestGUI;
import fr.chesiren.townychestbank.util.Messaging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryListener implements Listener {

    private final TownyChestBankPlugin plugin;

    public InventoryListener(TownyChestBankPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        BankChestGUI gui = plugin.getBankChestGUI();
        if (!gui.isBankGUI(event.getView().getTitle())) return;

        // Un double-clic (COLLECT_TO_CURSOR) collecterait les items virtuels des boutons de retrait
        // (slots 9-17) sans passer par handleWithdraw -> items gratuits. Bloquer sans condition.
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        Inventory topInv = event.getView().getTopInventory();

        UUID townUUID = gui.getTownUUIDForPlayer(player.getUniqueId());
        if (townUUID == null) { event.setCancelled(true); return; }

        // Shift-clic depuis l'inventaire joueur
        if (rawSlot >= topSize && event.isShiftClick()) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            PluginConfig cfg = plugin.getPluginConfig();
            if (cfg.isCurrencyItem(item.getType())) {
                if (!checkPermission(player, townUUID)) return;
                Town town = getTown(townUUID);
                if (town == null) return;
                double value = cfg.getItemValue(item.getType()) * item.getAmount();
                try {
                    town.getAccount().deposit(value, "TownyChestBank");
                    event.setCurrentItem(null);
                    Messaging.sendSuccess(player, "tcb_deposited", String.format("%.2f", value));
                    gui.refreshInventory(topInv, town);
                } catch (Exception e) {
                    Messaging.sendError(player, "tcb_error_deposit");
                }
            } else {
                // Article non-monetaire: placer dans la zone de depot si de la place
                for (int i = 18; i <= 26; i++) {
                    ItemStack existing = topInv.getItem(i);
                    if (existing == null || existing.getType() == Material.AIR) {
                        topInv.setItem(i, item.clone());
                        event.setCurrentItem(null);
                        return;
                    } else if (existing.isSimilar(item)) {
                        int space = existing.getMaxStackSize() - existing.getAmount();
                        if (space > 0) {
                            int toAdd = Math.min(space, item.getAmount());
                            existing.setAmount(existing.getAmount() + toAdd);
                            item.setAmount(item.getAmount() - toAdd);
                            if (item.getAmount() <= 0) {
                                event.setCurrentItem(null);
                            } else {
                                event.setCurrentItem(item);
                            }
                            return;
                        }
                    }
                }
                Messaging.sendError(player, "tcb_deposit_full");
            }
            return;
        }

        // Clic dans l'inventaire joueur (sans shift): laisser le comportement normal
        if (rawSlot >= topSize) return;

        // Zone de depot (rangee 3, slots 18-26): interactions libres, check Towny a la fermeture
        if (gui.isDepositSlot(rawSlot)) return;

        // Slots de retrait et d'info: bloques par defaut
        event.setCancelled(true);

        if (gui.isWithdrawSlot(rawSlot)) {
            handleWithdraw(player, rawSlot, event.isRightClick(), topInv, townUUID);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!plugin.getBankChestGUI().isBankGUI(event.getView().getTitle())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && (slot < 18 || slot > 26)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        BankChestGUI gui = plugin.getBankChestGUI();
        if (!gui.isBankGUI(event.getView().getTitle())) return;

        UUID townUUID = gui.getTownUUIDForPlayer(player.getUniqueId());
        gui.onClose(player.getUniqueId());
        if (townUUID == null) return;
        Town town = getTown(townUUID);
        if (town == null) return;

        Inventory inv = event.getInventory();
        PluginConfig cfg = plugin.getPluginConfig();

        boolean canDeposit = checkPermission(player, townUUID);

        double totalDeposited = 0;

        for (int i = 18; i <= 26; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            inv.setItem(i, null);

            if (!canDeposit) {
                giveOrDrop(player, item);
                continue;
            }

            if (cfg.isCurrencyItem(item.getType())) {
                double value = cfg.getItemValue(item.getType()) * item.getAmount();
                try {
                    town.getAccount().deposit(value, "TownyChestBank");
                    totalDeposited += value;
                } catch (Exception e) {
                    giveOrDrop(player, item);
                }
            } else {
                giveOrDrop(player, item);
            }
        }

        if (!canDeposit) {
            return;
        }

        if (totalDeposited > 0) {
            Messaging.sendSuccess(player, "tcb_deposited", String.format("%.2f", totalDeposited));
        }
    }

    private void handleWithdraw(Player player, int rawSlot, boolean rightClick, Inventory topInv, UUID townUUID) {
        if (!checkPermission(player, townUUID)) return;

        BankChestGUI gui = plugin.getBankChestGUI();
        PluginConfig cfg = plugin.getPluginConfig();
        Town town = getTown(townUUID);
        if (town == null) return;

        List<Map.Entry<Material, Double>> currencies = new ArrayList<>(cfg.getCurrencyItems().entrySet());
        int idx = rawSlot - 9;
        if (idx >= currencies.size()) return;

        Map.Entry<Material, Double> entry = currencies.get(idx);
        Material mat = entry.getKey();
        double value = entry.getValue();

        double balance;
        try {
            balance = town.getAccount().getHoldingBalance();
        } catch (Exception e) {
            return;
        }

        int maxWithdraw = (value > 0) ? (int) Math.floor(balance / value) : 0;
        maxWithdraw = Math.min(maxWithdraw, 64);

        if (maxWithdraw <= 0) {
            Messaging.sendError(player, "tcb_not_enough_funds");
            return;
        }

        int amount = rightClick ? 1 : maxWithdraw;
        double cost = amount * value;

        try {
            town.getAccount().withdraw(cost, "TownyChestBank");
        } catch (Exception e) {
            Messaging.sendError(player, "tcb_error_withdraw");
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(mat, amount));
        leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));

        Messaging.sendSuccess(player, "tcb_withdrew",
            String.format("%.2f", cost),
            String.valueOf(amount),
            mat.name().toLowerCase().replace('_', ' ')
        );
        gui.refreshInventory(topInv, town);
    }

    private boolean checkPermission(Player player, UUID townUUID) {
        Location chestLoc = plugin.getChestBankManager().getChestForTown(townUUID);
        if (chestLoc == null) return false;
        if (BankChestListener.hasSwitchPermission(player, chestLoc)) return true;
        Messaging.sendError(player, "tcb_no_towny_permission");
        return false;
    }

    private Town getTown(UUID uuid) {
        return TownyUniverse.getInstance().getTown(uuid);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item.clone())
            .values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}

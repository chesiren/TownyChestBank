package fr.chesiren.townychestbank.gui;

import com.palmergames.bukkit.towny.object.Town;
import fr.chesiren.townychestbank.TownyChestBankPlugin;
import fr.chesiren.townychestbank.config.PluginConfig;
import fr.chesiren.townychestbank.util.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BankChestGUI {

    // Identifiant stable du titre pour retrouver l'inventaire dans les listeners
    private static final String TITLE_PREFIX = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Coffre-Banque: " + ChatColor.RESET;
    private static final Material SEPARATOR_MAT = Material.GRAY_STAINED_GLASS_PANE;

    private final TownyChestBankPlugin plugin;
    // playerUUID -> townUUID
    private final Map<UUID, UUID> openGUIs = new HashMap<>();

    public BankChestGUI(TownyChestBankPlugin plugin) {
        this.plugin = plugin;
    }

    public String buildTitle(Town town) {
        return TITLE_PREFIX + town.getName();
    }

    public boolean isBankGUI(String title) {
        return title.startsWith(TITLE_PREFIX);
    }

    public void openForPlayer(Player player, Town town) {
        Inventory inv = buildInventory(town);
        // openInventory ferme l'inventaire precedent de facon synchrone (InventoryCloseEvent)
        // avant que ce joueur soit associe a la nouvelle ville -> ordre critique pour eviter
        // le credit du depot sur la mauvaise ville
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), town.getUUID());
    }

    public Inventory buildInventory(Town town) {
        Inventory inv = Bukkit.createInventory(null, 27, buildTitle(town));
        double balance = getBalance(town);
        PluginConfig cfg = plugin.getPluginConfig();

        // Rangee 1 (slots 0-8): separateurs + solde au centre (slot 4)
        ItemStack sep = makeSeparator();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, i == 4 ? makeBalanceItem(town.getName(), balance) : sep);
        }

        // Rangee 2 (slots 9-17): boutons de retrait, un par type de monnaie
        List<Map.Entry<NamespacedKey, Double>> currencies = new ArrayList<>(cfg.getCurrencyItems().entrySet());
        for (int i = 0; i < 9; i++) {
            if (i < currencies.size()) {
                Map.Entry<NamespacedKey, Double> entry = currencies.get(i);
                inv.setItem(9 + i, makeWithdrawItem(entry.getKey(), entry.getValue(), balance));
            } else {
                inv.setItem(9 + i, sep);
            }
        }

        // Rangee 3 (slots 18-26): zone de depot, laissee vide
        return inv;
    }

    public void refreshInventory(Inventory inv, Town town) {
        double balance = getBalance(town);
        PluginConfig cfg = plugin.getPluginConfig();
        inv.setItem(4, makeBalanceItem(town.getName(), balance));
        List<Map.Entry<NamespacedKey, Double>> currencies = new ArrayList<>(cfg.getCurrencyItems().entrySet());
        for (int i = 0; i < currencies.size() && i < 9; i++) {
            Map.Entry<NamespacedKey, Double> entry = currencies.get(i);
            inv.setItem(9 + i, makeWithdrawItem(entry.getKey(), entry.getValue(), balance));
        }
    }

    private double getBalance(Town town) {
        try {
            return town.getAccount().getHoldingBalance();
        } catch (Exception e) {
            return 0;
        }
    }

    private ItemStack makeBalanceItem(String townName, double balance) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + Messaging.tDefault("tcb_gui_balance_title", townName));
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + Messaging.tDefault("tcb_gui_balance_lore_solde"),
            ChatColor.WHITE + String.format("%.2f", balance) + ChatColor.GRAY + " " + Messaging.tDefault("tcb_gui_balance_lore_po"),
            "",
            ChatColor.GRAY + Messaging.tDefault("tcb_gui_balance_lore_depot"),
            ChatColor.GRAY + Messaging.tDefault("tcb_gui_balance_lore_shift"),
            ChatColor.GRAY + Messaging.tDefault("tcb_gui_balance_lore_retrait")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeWithdrawItem(NamespacedKey key, double value, double balance) {
        int maxWithdraw = (value > 0) ? (int) Math.floor(balance / value) : 0;
        maxWithdraw = Math.min(maxWithdraw, 64);

        Material mat = plugin.getPluginConfig().getMaterial(key);
        // BARRIER comme icone de secours si le Material n'est pas resolvable sur ce serveur
        ItemStack item = new ItemStack(mat != null ? mat : Material.BARRIER, Math.max(1, maxWithdraw));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + formatKeyName(key));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + Messaging.tDefault("tcb_gui_withdraw_lore_valeur", String.format("%.2f", value)));
        if (maxWithdraw > 0) {
            lore.add(ChatColor.GRAY + Messaging.tDefault("tcb_gui_withdraw_lore_retirable", String.valueOf(maxWithdraw)));
            lore.add("");
            lore.add(ChatColor.GREEN + Messaging.tDefault("tcb_gui_withdraw_lore_clic_gauche", String.valueOf(maxWithdraw)));
            lore.add(ChatColor.GREEN + Messaging.tDefault("tcb_gui_withdraw_lore_clic_droit"));
        } else {
            lore.add(ChatColor.RED + Messaging.tDefault("tcb_gui_withdraw_lore_insuffisant"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSeparator() {
        ItemStack item = new ItemStack(SEPARATOR_MAT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private String formatKeyName(NamespacedKey key) {
        String[] words = key.getKey().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public UUID getTownUUIDForPlayer(UUID playerUUID) {
        return openGUIs.get(playerUUID);
    }

    public void onClose(UUID playerUUID) {
        openGUIs.remove(playerUUID);
    }

    public boolean isWithdrawSlot(int rawSlot) {
        return rawSlot >= 9 && rawSlot <= 17;
    }

    public boolean isDepositSlot(int rawSlot) {
        return rawSlot >= 18 && rawSlot <= 26;
    }
}

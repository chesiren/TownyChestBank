package fr.chesiren.townychestbank.bank;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import fr.chesiren.townychestbank.TownyChestBankPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChestBankManager {

    private final TownyChestBankPlugin plugin;
    private final Map<UUID, TownBankData> townData = new HashMap<>();
    private final Map<String, UUID> chestIndex = new HashMap<>();
    private final File dataFile;

    public ChestBankManager(TownyChestBankPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    private String locationKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public void setChestForTown(UUID townUUID, Location loc) {
        TownBankData existing = townData.get(townUUID);
        if (existing != null && existing.hasChest()) {
            chestIndex.remove(locationKey(existing.getChestLocation()));
        }
        TownBankData data = townData.computeIfAbsent(townUUID, TownBankData::new);
        data.setChestLocation(loc);
        chestIndex.put(locationKey(loc), townUUID);
        saveData();
    }

    public void removeChestForTown(UUID townUUID) {
        TownBankData data = townData.get(townUUID);
        if (data != null && data.hasChest()) {
            chestIndex.remove(locationKey(data.getChestLocation()));
            data.setChestLocation(null);
            saveData();
        }
    }

    public Location getChestForTown(UUID townUUID) {
        TownBankData data = townData.get(townUUID);
        return data != null ? data.getChestLocation() : null;
    }

    public UUID getTownUUIDForChest(Location loc) {
        return chestIndex.get(locationKey(loc));
    }

    public boolean isBankChest(Location loc) {
        return chestIndex.containsKey(locationKey(loc));
    }

    public Town getTownForChest(Location loc) {
        UUID uuid = getTownUUIDForChest(loc);
        if (uuid == null) return null;
        return TownyUniverse.getInstance().getTown(uuid);
    }

    public void loadData() {
        if (!dataFile.exists()) return;
        FileConfiguration fc = YamlConfiguration.loadConfiguration(dataFile);
        townData.clear();
        chestIndex.clear();
        if (!fc.isConfigurationSection("towns")) return;
        for (String uuidStr : fc.getConfigurationSection("towns").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String base = "towns." + uuidStr;
                if (!fc.isSet(base + ".world")) continue;
                World world = plugin.getServer().getWorld(fc.getString(base + ".world"));
                if (world == null) continue;
                Location loc = new Location(world,
                    fc.getInt(base + ".x"),
                    fc.getInt(base + ".y"),
                    fc.getInt(base + ".z"));
                TownBankData data = new TownBankData(uuid, loc);
                townData.put(uuid, data);
                chestIndex.put(locationKey(loc), uuid);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur chargement donnees pour " + uuidStr + ": " + e.getMessage());
            }
        }
    }

    /**
     * Convertit le solde de la banque en items droppés au sol.
     * Les items les plus précieux par stack sont droppés en premier pour minimiser le nombre d'entités.
     * Retourne le montant effectivement converti en items.
     */
    public double dropBankContents(UUID townUUID, Location dropLoc) {
        try {
            Town town = TownyUniverse.getInstance().getTown(townUUID);
            if (town == null) return 0;

            double remaining = town.getAccount().getHoldingBalance();
            if (remaining <= 0) return 0;

            List<Map.Entry<Material, Double>> sorted = new ArrayList<>(plugin.getPluginConfig().getCurrencyItems().entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            double totalDropped = 0;
            for (Map.Entry<Material, Double> entry : sorted) {
                Material mat = entry.getKey();
                double value = entry.getValue();
                if (value <= 0 || remaining < value) continue;

                int totalCount = (int) Math.floor(remaining / value);
                while (totalCount > 0) {
                    int stackSize = Math.min(64, totalCount);
                    dropLoc.getWorld().dropItemNaturally(dropLoc, new ItemStack(mat, stackSize));
                    double dropped = stackSize * value;
                    totalDropped += dropped;
                    remaining -= dropped;
                    totalCount -= stackSize;
                }
            }

            if (totalDropped > 0) {
                town.getAccount().withdraw(totalDropped, "Coffre-banque detruit - items droppes au sol");
            }
            return totalDropped;
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du drop du coffre-banque: " + e.getMessage());
            return 0;
        }
    }

    public void saveData() {
        FileConfiguration fc = new YamlConfiguration();
        for (Map.Entry<UUID, TownBankData> entry : townData.entrySet()) {
            if (!entry.getValue().hasChest()) continue;
            String base = "towns." + entry.getKey();
            Location loc = entry.getValue().getChestLocation();
            fc.set(base + ".world", loc.getWorld().getName());
            fc.set(base + ".x", loc.getBlockX());
            fc.set(base + ".y", loc.getBlockY());
            fc.set(base + ".z", loc.getBlockZ());
        }
        try {
            fc.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder data.yml: " + e.getMessage());
        }
    }
}

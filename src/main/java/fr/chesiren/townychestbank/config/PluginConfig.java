package fr.chesiren.townychestbank.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PluginConfig {

    private final Map<NamespacedKey, Double> currencyItems = new LinkedHashMap<>();
    private final Map<NamespacedKey, Material> currencyMaterials = new LinkedHashMap<>();

    public PluginConfig(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        currencyItems.clear();
        currencyMaterials.clear();
        if (!config.isConfigurationSection("currency")) return;

        // Une seule passe sur Material.values() pour indexer tous les materials (vanilla + Forge).
        // matchMaterial() ne suffit pas sur Arclight : sa HashMap interne est construite avant
        // que Forge injecte ses items dans l'enum, donc les modded items y sont absents.
        Map<NamespacedKey, Material> materialIndex = new HashMap<>();
        for (Material m : Material.values()) {
            if (!m.isLegacy()) materialIndex.put(m.getKey(), m);
        }

        for (String key : config.getConfigurationSection("currency").getKeys(false)) {
            double value = config.getDouble("currency." + key);

            Material mat = Material.matchMaterial(key); // vanilla rapide
            if (mat == null) {
                NamespacedKey nsk = NamespacedKey.fromString(key.toLowerCase());
                if (nsk != null) mat = materialIndex.get(nsk);
            }

            if (mat != null) {
                currencyItems.put(mat.getKey(), value);
                currencyMaterials.put(mat.getKey(), mat);
            } else {
                NamespacedKey nsk = NamespacedKey.fromString(key.toLowerCase());
                if (nsk != null) currencyItems.put(nsk, value);
            }
        }
    }

    public Map<NamespacedKey, Double> getCurrencyItems() {
        return currencyItems;
    }

    public boolean isCurrencyItem(NamespacedKey key) {
        return currencyItems.containsKey(key);
    }

    public double getItemValue(NamespacedKey key) {
        return currencyItems.getOrDefault(key, 0.0);
    }

    /**
     * Retourne le Material associé à la clé, ou null si le mod n'est pas chargé.
     */
    public Material getMaterial(NamespacedKey key) {
        return currencyMaterials.get(key);
    }
}

package fr.chesiren.townychestbank.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;

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
        for (String key : config.getConfigurationSection("currency").getKeys(false)) {
            double value = config.getDouble("currency." + key);

            // Essai vanilla d'abord ("GOLD_NUGGET", "minecraft:gold_nugget")
            Material mat = Material.matchMaterial(key);
            if (mat != null) {
                currencyItems.put(mat.getKey(), value);
                currencyMaterials.put(mat.getKey(), mat);
                continue;
            }

            // Clé namespaced pour items moddés ("coinsje:copper_coin")
            NamespacedKey nsk = NamespacedKey.fromString(key.toLowerCase());
            if (nsk == null) continue;
            currencyItems.put(nsk, value);
            // Sur Mohist/Arclight le Material est déjà enregistré, on l'associe
            Material modMat = Material.matchMaterial(nsk.toString());
            if (modMat != null) currencyMaterials.put(nsk, modMat);
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
     * Retourne le Material associé à la clé, ou null pour les items moddés
     * non enregistrés sur ce type de serveur.
     */
    public Material getMaterial(NamespacedKey key) {
        Material mat = currencyMaterials.get(key);
        if (mat != null) return mat;
        // Fallback dynamique : utile si le mod est chargé après le reload du config
        return Material.matchMaterial(key.toString());
    }
}

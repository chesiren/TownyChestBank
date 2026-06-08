package fr.chesiren.townychestbank.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public class PluginConfig {

    private final Map<Material, Double> currencyItems = new LinkedHashMap<>();

    public PluginConfig(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        currencyItems.clear();
        if (config.isConfigurationSection("currency")) {
            for (String key : config.getConfigurationSection("currency").getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    currencyItems.put(mat, config.getDouble("currency." + key));
                }
            }
        }
    }

    public Map<Material, Double> getCurrencyItems() {
        return currencyItems;
    }

    public boolean isCurrencyItem(Material material) {
        return currencyItems.containsKey(material);
    }

    public double getItemValue(Material material) {
        return currencyItems.getOrDefault(material, 0.0);
    }
}

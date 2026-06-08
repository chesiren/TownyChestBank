package fr.chesiren.townychestbank;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.TranslationLoader;
import fr.chesiren.townychestbank.bank.ChestBankManager;
import fr.chesiren.townychestbank.command.SetBankChestCommand;
import fr.chesiren.townychestbank.config.PluginConfig;
import fr.chesiren.townychestbank.gui.BankChestGUI;
import fr.chesiren.townychestbank.listener.BankChestListener;
import fr.chesiren.townychestbank.listener.CommandBlockListener;
import fr.chesiren.townychestbank.listener.InventoryListener;
import fr.chesiren.townychestbank.listener.TownListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TownyChestBankPlugin extends JavaPlugin {

    private static TownyChestBankPlugin instance;
    private PluginConfig pluginConfig;
    private ChestBankManager chestBankManager;
    private BankChestGUI bankChestGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny n'est pas installe. Desactivation.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!chargerLocalisation(false)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginConfig = new PluginConfig(getConfig());
        chestBankManager = new ChestBankManager(this);
        bankChestGUI = new BankChestGUI(this);

        getServer().getPluginManager().registerEvents(new TownListener(this), this);
        getServer().getPluginManager().registerEvents(new BankChestListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandBlockListener(this), this);

        SetBankChestCommand cmd = new SetBankChestCommand(this);
        getCommand("tchestbank").setExecutor(cmd);
        getCommand("tchestbank").setTabCompleter(cmd);

        getLogger().info("TownyChestBank active.");
    }

    @Override
    public void onDisable() {
        if (chestBankManager != null) {
            chestBankManager.saveData();
        }
        getLogger().info("TownyChestBank desactive.");
    }

    public static boolean chargerLocalisation(boolean reload) {
        try {
            Path langFolderPath = Paths.get(instance.getDataFolder().getPath()).resolve("lang");
            TranslationLoader loader = new TranslationLoader(langFolderPath, instance, TownyChestBankPlugin.class);
            loader.load();
            TownyAPI.getInstance().addTranslations(instance, loader.getTranslations());
        } catch (TownyInitException e) {
            e.printStackTrace();
            instance.getLogger().severe("Echec du chargement des fichiers de langue.");
            return false;
        }
        return true;
    }

    public static TownyChestBankPlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public ChestBankManager getChestBankManager() {
        return chestBankManager;
    }

    public BankChestGUI getBankChestGUI() {
        return bankChestGUI;
    }
}

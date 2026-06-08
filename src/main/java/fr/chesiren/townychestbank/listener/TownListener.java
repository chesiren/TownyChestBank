package fr.chesiren.townychestbank.listener;

import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.object.Town;
import fr.chesiren.townychestbank.TownyChestBankPlugin;
import fr.chesiren.townychestbank.util.ChestPlacer;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class TownListener implements Listener {

    private final TownyChestBankPlugin plugin;

    public TownListener(TownyChestBankPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNewTown(NewTownEvent event) {
        // Delai de 2 ticks pour s'assurer que le homeblock est bien enregistre
        new BukkitRunnable() {
            @Override
            public void run() {
                Town town = event.getTown();
                try {
                    if (!town.hasHomeBlock()) return;
                    Location chestLoc = ChestPlacer.placeChestAtSurface(town.getHomeBlock(), plugin);
                    if (chestLoc != null) {
                        plugin.getChestBankManager().setChestForTown(town.getUUID(), chestLoc);
                        plugin.getLogger().info("Coffre-banque place pour " + town.getName() + " en " + chestLoc);
                    } else {
                        plugin.getLogger().warning("Impossible de placer le coffre-banque pour " + town.getName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Erreur placement coffre pour " + town.getName() + ": " + e.getMessage());
                }
            }
        }.runTaskLater(plugin, 2L);
    }
}

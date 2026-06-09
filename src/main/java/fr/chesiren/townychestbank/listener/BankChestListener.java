package fr.chesiren.townychestbank.listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import fr.chesiren.townychestbank.TownyChestBankPlugin;
import fr.chesiren.townychestbank.util.AdminUtil;
import fr.chesiren.townychestbank.util.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Iterator;
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
        Town town = plugin.getChestBankManager().getTownForChest(block.getLocation());
        if (town == null) return;

        if (!hasSwitchPermission(player, town)) {
            Messaging.sendError(player, "tcb_no_towny_permission");
            return;
        }

        plugin.getBankChestGUI().openForPlayer(player, town);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.getChestBankManager().isBankChest(block.getLocation())) return;

        Player player = event.getPlayer();
        if (AdminUtil.isAdminMode(player)) {
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
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (b.getType() != Material.CHEST || !plugin.getChestBankManager().isBankChest(b.getLocation())) {
                continue;
            }
            Player attacker = getPlayerSource(event.getEntity());
            if (attacker != null && isEnemyOfTown(attacker, b.getLocation())) {
                // Explosion ennemie: drop du contenu, notif, dé-enregistrement
                // Le bloc est laisse dans la liste pour etre detruit naturellement
                UUID townUUID = plugin.getChestBankManager().getTownUUIDForChest(b.getLocation());
                if (townUUID != null) {
                    plugin.getChestBankManager().dropBankContents(townUUID, b.getLocation().add(0.5, 0.5, 0.5));
                    plugin.getChestBankManager().removeChestForTown(townUUID);
                    notifyTownMembers(townUUID, "tcb_chest_exploded_warning");
                }
            } else {
                // Explosion non ennemie (creeper, ender dragon, wither...): coffre protege
                it.remove();
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        // Explosions de blocs (lit, ancre...) jamais causees par un joueur ennemi: protection totale
        event.blockList().removeIf(b ->
            b.getType() == Material.CHEST && plugin.getChestBankManager().isBankChest(b.getLocation())
        );
    }

    /**
     * Retourne le joueur a l'origine de l'explosion, ou null si pas de source joueur.
     * Couvre: TNT allumee par un joueur, boule de feu lancee par un joueur.
     */
    private Player getPlayerSource(Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof TNTPrimed) {
            Entity source = ((TNTPrimed) entity).getSource();
            if (source instanceof Player) return (Player) source;
        }
        if (entity instanceof Fireball) {
            ProjectileSource shooter = ((Fireball) entity).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    /**
     * Retourne true si le joueur n'est pas membre de la ville possedant le coffre.
     */
    private boolean isEnemyOfTown(Player player, Location chestLoc) {
        UUID townUUID = plugin.getChestBankManager().getTownUUIDForChest(chestLoc);
        if (townUUID == null) return false;
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown()) return true;
            return !resident.getTown().getUUID().equals(townUUID);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Envoie un message a tous les membres en ligne de la ville.
     */
    private void notifyTownMembers(UUID townUUID, String messageKey) {
        try {
            Town town = TownyUniverse.getInstance().getTown(townUUID);
            if (town == null) return;
            for (Resident r : town.getResidents()) {
                Player p = Bukkit.getPlayer(r.getUUID());
                if (p != null && p.isOnline()) {
                    Messaging.sendInfo(p, messageKey);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur notification explosion coffre-banque: " + e.getMessage());
        }
    }

    public static boolean hasSwitchPermission(Player player, Town town) {
        if (AdminUtil.isAdminMode(player)) return true;
        if (town == null) return false;
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown()) return false;
            return resident.getTown().getUUID().equals(town.getUUID());
        } catch (Exception e) {
            return false;
        }
    }
}

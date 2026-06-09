package fr.chesiren.townychestbank.util;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class AdminUtil {

    /**
     * Retourne true si le joueur a la permission admin ET est en mode Créatif.
     * En survie, la permission admin n'accorde aucun passe-droit.
     */
    public static boolean isAdminMode(Player player) {
        return player.hasPermission("townychestbank.admin")
            && player.getGameMode() == GameMode.CREATIVE;
    }
}

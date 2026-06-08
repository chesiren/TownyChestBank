package fr.chesiren.townychestbank.listener;

import fr.chesiren.townychestbank.TownyChestBankPlugin;
import fr.chesiren.townychestbank.util.Messaging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;

public class CommandBlockListener implements Listener {

    private final TownyChestBankPlugin plugin;

    // Commandes Towny bloquees (sans /), en minuscule
    // Couvre: /t deposit, /town deposit, /t withdraw, /town withdraw
    private static final List<String> BLOCKED_EXACT_PREFIXES = Arrays.asList(
        "t deposit", "t withdraw",
        "town deposit", "town withdraw"
    );

    // Pour /ta town <ville> deposit|withdraw et /townyadmin town <ville> deposit|withdraw
    private static final List<String> ADMIN_PREFIXES = Arrays.asList(
        "ta town ", "townyadmin town "
    );

    private static final List<String> BANK_SUBCOMMANDS = Arrays.asList("deposit", "withdraw");

    public CommandBlockListener(TownyChestBankPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        String msg = event.getMessage().trim().toLowerCase();
        if (!msg.startsWith("/")) return;
        String cmd = msg.substring(1).trim();

        for (String prefix : BLOCKED_EXACT_PREFIXES) {
            if (cmd.equals(prefix) || cmd.startsWith(prefix + " ")) {
                block(event);
                return;
            }
        }

        for (String prefix : ADMIN_PREFIXES) {
            if (cmd.startsWith(prefix)) {
                String[] parts = cmd.split("\\s+");
                // parts: [ta, town, <ville>, <action>, ...]
                if (parts.length >= 4 && BANK_SUBCOMMANDS.contains(parts[3])) {
                    block(event);
                    return;
                }
            }
        }
    }

    private void block(PlayerCommandPreprocessEvent event) {
        event.setCancelled(true);
        Messaging.sendError(event.getPlayer(), "tcb_command_blocked");
    }
}

package fr.chesiren.townychestbank.util;

import com.palmergames.bukkit.towny.object.TownBlock;
import fr.chesiren.townychestbank.TownyChestBankPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

public class ChestPlacer {

    private static final Random RANDOM = new Random();

    /**
     * Tente de poser un coffre a la surface du homeblock donne.
     * Retourne la location du coffre pose, ou null si impossible.
     */
    public static Location placeChestAtSurface(TownBlock homeBlock, TownyChestBankPlugin plugin) {
        try {
            World world = plugin.getServer().getWorld(homeBlock.getWorld().getName());
            if (world == null) return null;

            int chunkX = homeBlock.getX();
            int chunkZ = homeBlock.getZ();

            for (int attempt = 0; attempt < 20; attempt++) {
                int bx = chunkX * 16 + RANDOM.nextInt(16);
                int bz = chunkZ * 16 + RANDOM.nextInt(16);

                int by = world.getHighestBlockYAt(bx, bz);
                Block surface = world.getBlockAt(bx, by, bz);
                Block above = world.getBlockAt(bx, by + 1, bz);

                if (!surface.getType().isSolid()) continue;
                if (surface.isLiquid()) continue;
                if (above.getType() != Material.AIR) continue;

                above.setType(Material.CHEST);
                return above.getLocation();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du placement du coffre: " + e.getMessage());
        }
        return null;
    }
}

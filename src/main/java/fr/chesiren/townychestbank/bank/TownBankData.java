package fr.chesiren.townychestbank.bank;

import org.bukkit.Location;

import java.util.UUID;

public class TownBankData {

    private final UUID townUUID;
    private Location chestLocation;

    public TownBankData(UUID townUUID) {
        this.townUUID = townUUID;
    }

    public TownBankData(UUID townUUID, Location chestLocation) {
        this.townUUID = townUUID;
        this.chestLocation = chestLocation;
    }

    public UUID getTownUUID() {
        return townUUID;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public void setChestLocation(Location chestLocation) {
        this.chestLocation = chestLocation;
    }

    public boolean hasChest() {
        return chestLocation != null;
    }
}

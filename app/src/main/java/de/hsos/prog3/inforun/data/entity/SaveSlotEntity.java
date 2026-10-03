package de.hsos.prog3.inforun.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "save_slots")
public class SaveSlotEntity {

    @PrimaryKey
    public int slotId; // 1..3

    public String playerName;

    public int timeLeftSec;
    public int lives;
    public int collectibles;

    // für später (Levelkarte/Coins)
    public int coins;
    public int unlockedLevel;

    public long savedAt;

    public SaveSlotEntity(int slotId, String playerName,
                          int timeLeftSec, int lives, int collectibles,
                          int coins, int unlockedLevel, long savedAt) {
        this.slotId = slotId;
        this.playerName = playerName;
        this.timeLeftSec = timeLeftSec;
        this.lives = lives;
        this.collectibles = collectibles;
        this.coins = coins;
        this.unlockedLevel = unlockedLevel;
        this.savedAt = savedAt;
    }
}

package de.hsos.prog3.inforun.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "highscores")
public class HighscoreEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String playerName;

    public int score;

    /** Unix timestamp in ms */
    public long createdAt;

    public HighscoreEntity(String playerName, int score, long createdAt) {
        this.playerName = playerName;
        this.score = score;
        this.createdAt = createdAt;
    }
}

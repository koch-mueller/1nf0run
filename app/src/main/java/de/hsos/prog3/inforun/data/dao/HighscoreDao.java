package de.hsos.prog3.inforun.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import de.hsos.prog3.inforun.data.entity.HighscoreEntity;

@Dao
public interface HighscoreDao {

    @Insert
    long insert(HighscoreEntity highscore);

    @Query("SELECT * FROM highscores ORDER BY score DESC, createdAt DESC LIMIT :limit")
    List<HighscoreEntity> getTop(int limit);

    @Query("DELETE FROM highscores")
    void deleteAll();
}

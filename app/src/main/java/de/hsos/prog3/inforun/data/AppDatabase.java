package de.hsos.prog3.inforun.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import de.hsos.prog3.inforun.data.dao.HighscoreDao;
import de.hsos.prog3.inforun.data.dao.SaveSlotDao;
import de.hsos.prog3.inforun.data.entity.HighscoreEntity;
import de.hsos.prog3.inforun.data.entity.SaveSlotEntity;

@Database(
        entities = {HighscoreEntity.class, SaveSlotEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract HighscoreDao highscoreDao();

    public abstract SaveSlotDao saveSlotDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "inforun_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

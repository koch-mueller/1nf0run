package de.hsos.prog3.inforun.data.repository;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hsos.prog3.inforun.data.AppDatabase;
import de.hsos.prog3.inforun.data.dao.HighscoreDao;
import de.hsos.prog3.inforun.data.entity.HighscoreEntity;

public class HighscoreRepository {

    public interface ResultCallback<T> {
        void onResult(T result);
    }

    private final HighscoreDao highscoreDao;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public HighscoreRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        highscoreDao = db.highscoreDao();
    }

    public void insertHighscore(String playerName, int score) {
        long now = System.currentTimeMillis();
        HighscoreEntity entity = new HighscoreEntity(playerName, score, now);

        dbExecutor.execute(() -> highscoreDao.insert(entity));
    }

    public void getTopHighscores(int limit, ResultCallback<List<HighscoreEntity>> callback) {
        dbExecutor.execute(() -> {
            List<HighscoreEntity> list = highscoreDao.getTop(limit);
            callback.onResult(list);
        });
    }

    public void deleteAll() {
        dbExecutor.execute(highscoreDao::deleteAll);
    }
}

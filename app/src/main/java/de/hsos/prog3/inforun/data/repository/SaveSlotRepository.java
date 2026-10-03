package de.hsos.prog3.inforun.data.repository;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hsos.prog3.inforun.data.AppDatabase;
import de.hsos.prog3.inforun.data.dao.SaveSlotDao;
import de.hsos.prog3.inforun.data.entity.SaveSlotEntity;

public class SaveSlotRepository {

    public interface ResultCallback<T> {
        void onResult(T result);
    }

    private final SaveSlotDao dao;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public SaveSlotRepository(Context context) {
        dao = AppDatabase.getInstance(context).saveSlotDao();
    }

    public void getAll(ResultCallback<List<SaveSlotEntity>> cb) {
        dbExecutor.execute(() -> cb.onResult(dao.getAll()));
    }

    public void getBySlot(int slotId, ResultCallback<SaveSlotEntity> cb) {
        dbExecutor.execute(() -> cb.onResult(dao.getBySlot(slotId)));
    }

    public void upsert(SaveSlotEntity slot) {
        dbExecutor.execute(() -> dao.upsert(slot));
    }

    public void deleteSlot(int slotId) {
        dbExecutor.execute(() -> dao.deleteSlot(slotId));
    }

    public void deleteAll() {
        dbExecutor.execute(dao::deleteAll);
    }
}

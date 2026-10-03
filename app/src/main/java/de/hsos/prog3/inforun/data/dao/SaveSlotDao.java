package de.hsos.prog3.inforun.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import de.hsos.prog3.inforun.data.entity.SaveSlotEntity;

@Dao
public interface SaveSlotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SaveSlotEntity slot);

    @Query("SELECT * FROM save_slots ORDER BY slotId ASC")
    List<SaveSlotEntity> getAll();

    @Query("SELECT * FROM save_slots WHERE slotId = :slotId LIMIT 1")
    SaveSlotEntity getBySlot(int slotId);

    @Query("DELETE FROM save_slots WHERE slotId = :slotId")
    void deleteSlot(int slotId);

    @Query("DELETE FROM save_slots")
    void deleteAll();
}

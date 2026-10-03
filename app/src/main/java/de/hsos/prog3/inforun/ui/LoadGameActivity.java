package de.hsos.prog3.inforun.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hsos.prog3.inforun.R;
import de.hsos.prog3.inforun.data.entity.SaveSlotEntity;
import de.hsos.prog3.inforun.data.repository.SaveSlotRepository;

public class LoadGameActivity extends AppCompatActivity {

    private LinearLayout savegameList;
    private SaveSlotRepository repo;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_game);

        savegameList = findViewById(R.id.savegameList);
        repo = new SaveSlotRepository(this);

        Button btnBack = findViewById(R.id.btnBackLoad);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSlots();
    }

    private void loadSlots() {
        repo.getAll(list -> ui.post(() -> renderSlots(list)));
    }

    private void renderSlots(List<SaveSlotEntity> slots) {
        savegameList.removeAllViews();

        savegameList.addView(makeSlotRow(1, find(slots, 1)));
        savegameList.addView(makeSlotRow(2, find(slots, 2)));
        savegameList.addView(makeSlotRow(3, find(slots, 3)));
    }

    private SaveSlotEntity find(List<SaveSlotEntity> slots, int id) {
        if (slots == null) return null;
        for (SaveSlotEntity s : slots) if (s.slotId == id) return s;
        return null;
    }

    private LinearLayout makeSlotRow(int slotId, SaveSlotEntity s) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        int padding = UiUnits.dp(this, 16);
        row.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText(getString(R.string.slot_title, slotId));
        title.setTextSize(20f);
        title.setTextColor(Color.BLACK);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        TextView info = new TextView(this);
        info.setTextSize(16f);
        info.setTextColor(Color.BLACK);
        if (s == null) {
            info.setText(R.string.slot_empty);
        } else {
            String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
                    .format(new Date(s.savedAt));
            info.setText(getString(
                    R.string.slot_summary,
                    s.playerName,
                    s.timeLeftSec,
                    s.lives,
                    s.collectibles,
                    s.coins,
                    s.unlockedLevel,
                    dateStr
            ));
        }

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button load = new Button(this);
        load.setText(R.string.load_game);
        load.setEnabled(s != null);
        load.setOnClickListener(v -> startFromSlot(s));

        Button del = new Button(this);
        del.setText(R.string.delete);
        del.setEnabled(s != null);
        del.setOnClickListener(v -> confirmDelete(slotId));

        btnRow.addView(load);
        btnRow.addView(del);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, 0, 0, UiUnits.dp(this, 20));
        row.setLayoutParams(p);

        row.addView(title);
        row.addView(info);
        row.addView(btnRow);

        return row;
    }

    private void confirmDelete(int slotId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.slot_delete_title)
                .setMessage(getString(R.string.slot_delete_message, slotId))
                .setPositiveButton(R.string.delete, (d, w) -> {
                    repo.deleteSlot(slotId);
                    loadSlots();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void startFromSlot(SaveSlotEntity s) {
        Intent intent = new Intent(this, SlotHubActivity.class);
        intent.putExtra(GameActivity.EXTRA_SLOT_ID, s.slotId);
        startActivity(intent);
        finish();
    }
}

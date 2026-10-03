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

public class SlotSelectActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NAME = "extra_player_name";

    private LinearLayout slotList;
    private SaveSlotRepository repo;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private String playerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slot_select);

        playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = getString(R.string.player_unknown);
        }

        slotList = findViewById(R.id.slotList);
        repo = new SaveSlotRepository(this);

        Button back = findViewById(R.id.btnBackSlot);
        back.setOnClickListener(v -> finish());

        loadSlots();
    }

    private void loadSlots() {
        repo.getAll(list -> ui.post(() -> renderSlots(list)));
    }

    private void renderSlots(List<SaveSlotEntity> slots) {
        slotList.removeAllViews();

        slotList.addView(makeSlotView(1, find(slots, 1)));
        slotList.addView(makeSlotView(2, find(slots, 2)));
        slotList.addView(makeSlotView(3, find(slots, 3)));
    }

    private SaveSlotEntity find(List<SaveSlotEntity> slots, int id) {
        if (slots == null) return null;
        for (SaveSlotEntity s : slots) if (s.slotId == id) return s;
        return null;
    }

    private LinearLayout makeSlotView(int slotId, SaveSlotEntity existing) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int padding = UiUnits.dp(this, 16);
        box.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText(getString(R.string.slot_title, slotId));
        title.setTextSize(20f);
        title.setTextColor(Color.BLACK);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        TextView info = new TextView(this);
        info.setTextSize(16f);
        info.setTextColor(Color.BLACK);
        if (existing == null) {
            info.setText(R.string.slot_empty);
        } else {
            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
                    .format(new Date(existing.savedAt));
            info.setText(getString(
                    R.string.slot_summary,
                    existing.playerName,
                    existing.timeLeftSec,
                    existing.lives,
                    existing.collectibles,
                    existing.coins,
                    existing.unlockedLevel,
                    date
            ));
        }

        Button use = new Button(this);
        use.setText(existing == null ? R.string.slot_use : R.string.slot_use_occupied);
        use.setOnClickListener(v -> onSelect(slotId, existing));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, 0, 0, UiUnits.dp(this, 20));

        box.addView(title);
        box.addView(info);
        box.addView(use);
        box.setLayoutParams(p);

        return box;
    }

    private void onSelect(int slotId, SaveSlotEntity existing) {
        if (existing == null) {
            createNew(slotId);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.slot_overwrite_title)
                .setMessage(getString(R.string.slot_overwrite_message, slotId))
                .setPositiveButton(R.string.overwrite, (d, w) -> createNew(slotId))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void createNew(int slotId) {
        SaveSlotEntity slot = new SaveSlotEntity(
                slotId,
                playerName,
                60, 3, 0,
                0, 1,
                System.currentTimeMillis()
        );
        repo.upsert(slot);

        Intent i = new Intent(this, SlotHubActivity.class);
        i.putExtra(GameActivity.EXTRA_SLOT_ID, slotId);
        startActivity(i);
        finish();
    }
}

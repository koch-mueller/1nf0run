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

import de.hsos.prog3.inforun.R;
import de.hsos.prog3.inforun.data.entity.SaveSlotEntity;
import de.hsos.prog3.inforun.data.repository.SaveSlotRepository;
import de.hsos.prog3.inforun.game.GameConstants;

public class SlotHubActivity extends AppCompatActivity {

    public static final String EXTRA_SLOT_ID = GameActivity.EXTRA_SLOT_ID;

    private int slotId;
    private SaveSlotRepository repo;

    private TextView tvInfo;
    private LinearLayout levelList;

    private final Handler ui = new Handler(Looper.getMainLooper());

    // Unlock costs for levels 2 through 5.
    private int unlockCost(int levelId) {
        return (levelId - 1) * 10;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slot_hub);

        slotId = getIntent().getIntExtra(EXTRA_SLOT_ID, -1);
        repo = new SaveSlotRepository(this);

        tvInfo = findViewById(R.id.tvHubInfo);
        levelList = findViewById(R.id.levelList);

        Button back = findViewById(R.id.btnHubBack);
        back.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSlot();
    }

    private void loadSlot() {
        repo.getBySlot(slotId, slot -> ui.post(() -> render(slot)));
    }

    private void render(SaveSlotEntity slot) {
        if (slot == null) {
            tvInfo.setText(R.string.slot_not_found);
            levelList.removeAllViews();
            return;
        }

        tvInfo.setText(getString(
                R.string.slot_hub_summary,
                slot.slotId,
                slot.playerName,
                slot.coins,
                slot.unlockedLevel
        ));
        tvInfo.setTextColor(Color.BLACK);
        levelList.removeAllViews();

        for (int levelId = 1; levelId <= GameConstants.MAX_LEVEL; levelId++) {
            addLevelButton(slot, levelId);
        }
    }

    private void addLevelButton(SaveSlotEntity slot, int levelId) {
        Button b = new Button(this);

        boolean unlocked = levelId <= Math.max(1, slot.unlockedLevel);
        if (levelId == 1) unlocked = true;

        if (unlocked) {
            b.setText(getString(R.string.level_start, levelId));
            b.setOnClickListener(v -> startLevel(slot, levelId));
        } else {
            int cost = unlockCost(levelId);
            b.setText(getString(R.string.level_locked, levelId, cost));
            b.setOnClickListener(v -> tryUnlock(slot, levelId));
        }

        levelList.addView(b);
    }

    private void startLevel(SaveSlotEntity slot, int levelId) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(NewGameActivity.EXTRA_PLAYER_NAME, slot.playerName);
        i.putExtra(GameActivity.EXTRA_TIME_LEFT, 60);
        i.putExtra(GameActivity.EXTRA_LIVES, 3);
        i.putExtra(GameActivity.EXTRA_COLLECTIBLES, 0);
        i.putExtra(GameActivity.EXTRA_SLOT_ID, slot.slotId);
        i.putExtra(GameActivity.EXTRA_LEVEL_ID, levelId);
        startActivity(i);
    }

    private void tryUnlock(SaveSlotEntity slot, int levelId) {
        int cost = unlockCost(levelId);

        if (slot.coins < cost) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.not_enough_coins_title)
                    .setMessage(getString(R.string.not_enough_coins_message, cost, slot.coins))
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.unlock_level_title)
                .setMessage(getString(R.string.unlock_level_message, levelId, cost))
                .setPositiveButton(R.string.unlock, (d, w) -> {
                    slot.coins -= cost;
                    slot.unlockedLevel = Math.max(slot.unlockedLevel, levelId);
                    slot.savedAt = System.currentTimeMillis();
                    repo.upsert(slot);
                    loadSlot();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

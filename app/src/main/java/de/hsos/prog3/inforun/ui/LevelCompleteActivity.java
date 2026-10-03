package de.hsos.prog3.inforun.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import de.hsos.prog3.inforun.R;

public class LevelCompleteActivity extends AppCompatActivity {

    public static final String EXTRA_SLOT_ID = GameActivity.EXTRA_SLOT_ID;
    public static final String EXTRA_LEVEL_ID = GameActivity.EXTRA_LEVEL_ID;
    public static final String EXTRA_COINS_GAINED = "extra_coins_gained";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_complete);

        int slotId = getIntent().getIntExtra(EXTRA_SLOT_ID, -1);
        int levelId = getIntent().getIntExtra(EXTRA_LEVEL_ID, 1);
        int gained = getIntent().getIntExtra(EXTRA_COINS_GAINED, 0);

        TextView info = findViewById(R.id.tvLevelCompleteInfo);
        info.setText(getString(R.string.level_complete_info, levelId, gained));

        Button back = findViewById(R.id.btnBackToHub);
        back.setOnClickListener(v -> {
            Intent i = new Intent(this, SlotHubActivity.class);
            i.putExtra(GameActivity.EXTRA_SLOT_ID, slotId);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }
}

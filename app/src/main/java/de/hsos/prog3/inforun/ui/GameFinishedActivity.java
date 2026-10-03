package de.hsos.prog3.inforun.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import de.hsos.prog3.inforun.R;
import de.hsos.prog3.inforun.game.GameConstants;

public class GameFinishedActivity extends AppCompatActivity {

    public static final String EXTRA_SLOT_ID = GameActivity.EXTRA_SLOT_ID;
    public static final String EXTRA_COINS_TOTAL = "extra_coins_total";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_finished);

        int coinsTotal = getIntent().getIntExtra(EXTRA_COINS_TOTAL, 0);

        TextView info = findViewById(R.id.tvFinishInfo);
        info.setText(getString(R.string.game_finished_info, GameConstants.MAX_LEVEL, coinsTotal));

        Button back = findViewById(R.id.btnBackToHome);
        back.setOnClickListener(v -> {
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }
}

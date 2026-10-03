package de.hsos.prog3.inforun.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import de.hsos.prog3.inforun.R;

public class GameOverActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER = "extra_player";
    public static final String EXTRA_SCORE = "extra_score";
    public static final String EXTRA_REASON = "extra_reason";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        String player = getIntent().getStringExtra(EXTRA_PLAYER);
        int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        String reason = getIntent().getStringExtra(EXTRA_REASON);

        TextView info = findViewById(R.id.tvGameOverInfo);
        info.setText(getString(R.string.game_over_info, player, score, reason));
        info.setTextColor(Color.WHITE);
        Button retry = findViewById(R.id.btnRetry);
        Button toHome = findViewById(R.id.btnToHome);
        Button toHighscores = findViewById(R.id.btnToHighscores);

        retry.setOnClickListener(v -> {
            Intent i = new Intent(this, NewGameActivity.class);
            // Preserve the name for a quick retry.
            i.putExtra(NewGameActivity.EXTRA_PLAYER_NAME, player);
            startActivity(i);
            finish();
        });

        toHighscores.setOnClickListener(v -> {
            Intent i = new Intent(this, HighscoreActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finish();
        });

        toHome.setOnClickListener(v -> {
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }
}

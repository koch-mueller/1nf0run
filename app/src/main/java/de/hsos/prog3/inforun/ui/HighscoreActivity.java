package de.hsos.prog3.inforun.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hsos.prog3.inforun.R;
import de.hsos.prog3.inforun.data.entity.HighscoreEntity;
import de.hsos.prog3.inforun.data.repository.HighscoreRepository;

public class HighscoreActivity extends AppCompatActivity {

    private LinearLayout highscoreList;
    private HighscoreRepository repository;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_highscore);

        highscoreList = findViewById(R.id.highscoreList);
        repository = new HighscoreRepository(this);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadHighscores();
    }

    private void loadHighscores() {
        repository.getTopHighscores(10, list -> {
            uiHandler.post(() -> renderHighscores(list));
        });
    }

    private void renderHighscores(List<HighscoreEntity> scores) {
        highscoreList.removeAllViews();

        if (scores == null || scores.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_highscores);
            empty.setTextSize(16f);
            empty.setTextColor(Color.BLACK);
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            highscoreList.addView(empty);
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY);

        int rank = 1;
        for (HighscoreEntity e : scores) {
            String dateStr = df.format(new Date(e.createdAt));

            TextView row = new TextView(this);
            row.setText(getString(R.string.highscore_row, rank, e.playerName, e.score, dateStr));
            row.setTextSize(16f);
            row.setTextColor(Color.BLACK);
            row.setPadding(
                    UiUnits.dp(this, 8),
                    UiUnits.dp(this, 12),
                    UiUnits.dp(this, 8),
                    UiUnits.dp(this, 12)
            );

            highscoreList.addView(row);
            rank++;
        }
    }
}

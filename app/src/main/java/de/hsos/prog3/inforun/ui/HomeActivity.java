package de.hsos.prog3.inforun.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import de.hsos.prog3.inforun.R;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button btnNewGame = findViewById(R.id.btnNewGame);
        Button btnLoadGame = findViewById(R.id.btnLoadGame);
        Button btnHighscores = findViewById(R.id.btnHighscores);
        Button btnSettings = findViewById(R.id.btnSettings);

        btnNewGame.setOnClickListener(v ->
                startActivity(new Intent(this, NewGameActivity.class)));

        btnLoadGame.setOnClickListener(v ->
                startActivity(new Intent(this, LoadGameActivity.class)));

        btnHighscores.setOnClickListener(v ->
                startActivity(new Intent(this, HighscoreActivity.class)));

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }
}

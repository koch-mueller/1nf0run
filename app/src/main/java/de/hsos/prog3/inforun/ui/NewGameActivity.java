package de.hsos.prog3.inforun.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import de.hsos.prog3.inforun.R;

public class NewGameActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NAME = "player_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        EditText editPlayerName = findViewById(R.id.editPlayerName);
        Button btnStartGame = findViewById(R.id.btnStartGame);

        String prefillName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        if (prefillName != null && !prefillName.trim().isEmpty()) {
            editPlayerName.setText(prefillName);
            editPlayerName.setSelection(prefillName.length());
        }

        btnStartGame.setOnClickListener(v -> {
            String name = editPlayerName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, R.string.enter_player_name, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, SlotSelectActivity.class);
            intent.putExtra(SlotSelectActivity.EXTRA_PLAYER_NAME, name);
            startActivity(intent);
            finish();
        });
    }
}

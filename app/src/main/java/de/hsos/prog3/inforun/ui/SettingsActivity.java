package de.hsos.prog3.inforun.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import de.hsos.prog3.inforun.R;
import de.hsos.prog3.inforun.data.repository.HighscoreRepository;
import de.hsos.prog3.inforun.data.repository.SaveSlotRepository;

public class SettingsActivity extends AppCompatActivity {

    private HighscoreRepository highscoreRepository;
    private SaveSlotRepository saveSlotRepository;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        highscoreRepository = new HighscoreRepository(this);
        saveSlotRepository = new SaveSlotRepository(this);

        Button btnReset = findViewById(R.id.btnResetData);
        Button btnBack = findViewById(R.id.btnBackSettings);

        btnBack.setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> showResetConfirmDialog());
    }

    private void showResetConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_data_title)
                .setMessage(R.string.reset_data_message)
                .setPositiveButton(R.string.yes, (d, w) -> resetAllData())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void resetAllData() {
        // Repository operations run off the UI thread.
        highscoreRepository.deleteAll();
        saveSlotRepository.deleteAll();

        uiHandler.post(() ->
                Toast.makeText(this, R.string.data_deleted, Toast.LENGTH_SHORT).show()
        );
    }
}

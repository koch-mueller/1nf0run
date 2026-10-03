package de.hsos.prog3.inforun.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import de.hsos.prog3.inforun.R;
import de.hsos.prog3.inforun.data.entity.SaveSlotEntity;
import de.hsos.prog3.inforun.data.repository.HighscoreRepository;
import de.hsos.prog3.inforun.data.repository.SaveSlotRepository;
import de.hsos.prog3.inforun.game.GameConstants;
import de.hsos.prog3.inforun.game.GameEventListener;
import de.hsos.prog3.inforun.game.GameView;
import de.hsos.prog3.inforun.game.LevelConfig;
import de.hsos.prog3.inforun.ui.dialog.PauseDialogFragment;

public class GameActivity extends AppCompatActivity
        implements GameEventListener, PauseDialogFragment.PauseActions {

    // ---- Intent Extras ----
    public static final String EXTRA_LEVEL_ID = "extra_level_id";
    public static final String EXTRA_SLOT_ID = "extra_slot_id";

    public static final String EXTRA_TIME_LEFT = "extra_time_left";
    public static final String EXTRA_LIVES = "extra_lives";
    public static final String EXTRA_COLLECTIBLES = "extra_collectibles";

    // ---- HUD ----
    private TextView tvLives;
    private TextView tvTime;
    private TextView tvCollectibles;

    // ---- State ----
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private String playerName;
    private int timeLeftSec = 60;
    private int lives = 3;
    private int collectibles = 0;

    private int levelId = 1;
    private int slotId = -1;

    private int slotCoins = 0;
    private int slotUnlockedLevel = 1;

    private boolean gameFinished = false;

    // ---- GameView ----
    private GameView gameView;

    // ---- Persistence ----
    private HighscoreRepository highscoreRepository;
    private SaveSlotRepository saveSlotRepository;

    // ---- Timer Runnable ----
    private final Runnable timeTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (gameFinished) return;

            timeLeftSec--;
            updateHud();

            if (timeLeftSec <= 0) {
                onLevelCompleted();
            } else {
                uiHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        playerName = getString(R.string.player_unknown);

        // ---- Bind HUD ----
        tvLives = findViewById(R.id.tvLives);
        tvTime = findViewById(R.id.tvTime);
        tvCollectibles = findViewById(R.id.tvCollectibles);

        if (tvLives == null || tvTime == null || tvCollectibles == null) {
            throw new IllegalStateException("HUD TextViews not found. Check activity_game.xml IDs: tvLives, tvTime, tvCollectibles");
        }

        // ---- Read Extras ----
        String nameExtra = getIntent().getStringExtra(NewGameActivity.EXTRA_PLAYER_NAME);
        if (nameExtra != null && !nameExtra.trim().isEmpty()) {
            playerName = nameExtra.trim();
        }

        levelId = getIntent().getIntExtra(EXTRA_LEVEL_ID, 1);
        slotId = getIntent().getIntExtra(EXTRA_SLOT_ID, -1);

        timeLeftSec = getIntent().getIntExtra(EXTRA_TIME_LEFT, 60);
        lives = getIntent().getIntExtra(EXTRA_LIVES, 3);
        collectibles = getIntent().getIntExtra(EXTRA_COLLECTIBLES, 0);

        // ---- Repositories ----
        highscoreRepository = new HighscoreRepository(this);
        saveSlotRepository = new SaveSlotRepository(this);

        // Load slot info (coins/unlocked)
        if (slotId != -1) {
            saveSlotRepository.getBySlot(slotId, slot -> uiHandler.post(() -> {
                if (slot != null) {
                    slotCoins = slot.coins;
                    slotUnlockedLevel = slot.unlockedLevel;
                }
            }));
        }

        // ---- Attach GameView ----
        FrameLayout container = findViewById(R.id.gameContainer);
        if (container == null) {
            throw new IllegalStateException("gameContainer not found in activity_game.xml");
        }

        container.removeAllViews();
        gameView = new GameView(this);
        gameView.setGameEventListener(this);
        container.addView(gameView);

        // ---- Apply Level ----
        // Theme (background + world offset) by levelId
        gameView.applyLevelId(levelId);

        // Difficulty/config by levelId
        LevelConfig cfg = LevelConfig.forLevel(levelId);
        gameView.applyLevelConfig(cfg);

        // ---- Buttons ----
        Button btnQuit = findViewById(R.id.btnQuit);
        if (btnQuit == null) {
            throw new IllegalStateException("btnQuit not found in activity_game.xml");
        }
        btnQuit.setOnClickListener(v -> quitToHome());

        Button btnPause = findViewById(R.id.btnPause);
        if (btnPause == null) {
            throw new IllegalStateException("btnPause not found in activity_game.xml");
        }
        btnPause.setOnClickListener(v -> showPauseDialog());

        updateHud();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameFinished) return;

        if (gameView != null) gameView.resume();

        uiHandler.removeCallbacks(timeTickRunnable);
        uiHandler.postDelayed(timeTickRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        uiHandler.removeCallbacks(timeTickRunnable);
        if (gameView != null) gameView.pause();
    }

    // ---- HUD ----
    private void updateHud() {
        tvLives.setText(getString(R.string.hud_lives, lives));
        tvTime.setText(getString(R.string.hud_time, timeLeftSec));
        tvCollectibles.setText(getString(R.string.hud_items, collectibles));
    }

    // ---- GameEventListener ----
    @Override
    public void onCollectiblePicked() {
        uiHandler.post(() -> {
            if (gameFinished) return;
            collectibles++;
            updateHud();
        });
    }

    @Override
    public void onHitObstacle() {
        uiHandler.post(() -> {
            if (gameFinished) return;

            lives--;
            updateHud();

            if (lives <= 0) {
                endGame(getString(R.string.reason_no_lives));
            }
        });
    }

    @Override
    public void onGameOver() {
        uiHandler.post(() -> {
            if (gameFinished) return;
            endGame(getString(R.string.reason_game_over));
        });
    }

    // ---- End / Navigation ----
    private void endGame(String message) {
        if (gameFinished) return;
        gameFinished = true;

        uiHandler.removeCallbacks(timeTickRunnable);
        if (gameView != null) gameView.pause();

        int score = calculateScore();
        highscoreRepository.insertHighscore(playerName, score);

        Intent intent = new Intent(this, GameOverActivity.class);
        intent.putExtra(GameOverActivity.EXTRA_PLAYER, playerName);
        intent.putExtra(GameOverActivity.EXTRA_SCORE, score);
        intent.putExtra(GameOverActivity.EXTRA_REASON, message);
        startActivity(intent);
        finish();
    }

    private void onLevelCompleted() {
        if (gameFinished) return;
        gameFinished = true;

        uiHandler.removeCallbacks(timeTickRunnable);
        if (gameView != null) gameView.pause();

        int score = calculateScore();
        highscoreRepository.insertHighscore(playerName, score);

        // Coins/Unlock in Slot speichern
        if (slotId != -1) {
            int newCoins = slotCoins + collectibles;
            int newUnlocked = Math.max(slotUnlockedLevel, levelId + 1);

            SaveSlotEntity updated = new SaveSlotEntity(
                    slotId,
                    playerName,
                    60, 3, 0,              // neuer Run startet frisch
                    newCoins,
                    newUnlocked,
                    System.currentTimeMillis()
            );
            saveSlotRepository.upsert(updated);

            if (levelId >= GameConstants.MAX_LEVEL) {
                Intent fin = new Intent(this, GameFinishedActivity.class);
                fin.putExtra(GameActivity.EXTRA_SLOT_ID, slotId);
                fin.putExtra(GameFinishedActivity.EXTRA_COINS_TOTAL, newCoins);
                startActivity(fin);
                finish();
                return;
            }
        }

        Intent i = new Intent(this, LevelCompleteActivity.class);
        i.putExtra(EXTRA_SLOT_ID, slotId);
        i.putExtra(EXTRA_LEVEL_ID, levelId);
        i.putExtra(LevelCompleteActivity.EXTRA_COINS_GAINED, collectibles);
        startActivity(i);
        finish();
    }

    private int calculateScore() {
        return collectibles * 10 + Math.max(timeLeftSec, 0);
    }

    private void quitToHome() {
        // Keep unlocked levels and collected coins when leaving a run.
        if (slotId != -1) {
            SaveSlotEntity updated = new SaveSlotEntity(
                    slotId,
                    playerName,
                    60, 3, 0,
                    slotCoins,              // Coins bleiben
                    slotUnlockedLevel,      // Unlock bleibt
                    System.currentTimeMillis()
            );
            saveSlotRepository.upsert(updated);
        }

        uiHandler.removeCallbacks(timeTickRunnable);
        if (gameView != null) gameView.pause();

        Intent i = new Intent(this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void showPauseDialog() {
        uiHandler.removeCallbacks(timeTickRunnable);
        if (gameView != null) gameView.pause();

        PauseDialogFragment dialog = new PauseDialogFragment();

        dialog.setCancelable(true);
        dialog.show(getSupportFragmentManager(), "pause_dialog");
    }

    private void resumeGame() {
        if (gameFinished) return;

        uiHandler.removeCallbacks(timeTickRunnable);
        uiHandler.postDelayed(timeTickRunnable, 1000);

        if (gameView != null) gameView.resume();
    }

    private void restartGame() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    public void onResumeGame() {
        resumeGame();
    }

    @Override
    public void onRestartGame() {
        restartGame();
    }

    @Override
    public void onQuitGame() {
        quitToHome();
    }
}

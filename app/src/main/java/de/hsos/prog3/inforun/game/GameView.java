package de.hsos.prog3.inforun.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.hsos.prog3.inforun.R;
import de.hsos.prog3.inforun.game.entities.Player;
import de.hsos.prog3.inforun.game.entities.collectibles.Coin;
import de.hsos.prog3.inforun.game.entities.enemies.CockroachEnemy;
import de.hsos.prog3.inforun.game.entities.enemies.Enemy;
import de.hsos.prog3.inforun.game.entities.enemies.RobotEnemy;
import de.hsos.prog3.inforun.game.entities.powerups.ActivePowerUps;
import de.hsos.prog3.inforun.game.entities.powerups.PowerUpEntity;
import de.hsos.prog3.inforun.game.entities.powerups.PowerUpType;
import de.hsos.prog3.inforun.game.entities.projectiles.CDProjectile;
import de.hsos.prog3.inforun.game.world.Platform;
import de.hsos.prog3.inforun.game.world.Spike;

/**
 * GameView:
 * - Player bewegt sich nach rechts (Auto-Run), Welt bleibt in World-Koordinaten.
 * - Kamera folgt dem Player (Canvas translate).
 * - Prozedural generierte Plattformen, Gefahren, Gegner und Collectibles.
 * - Levelabhängige Muster und Schwierigkeitsparameter sorgen für Abwechslung.
 * - Collectibles werden nach der Weltgenerierung in einem separaten Loot-Layer platziert.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread;
    private GameEventListener listener;

    // Config can be applied before surfaceCreated (from Activity).
    private LevelConfig pendingConfig;
    private boolean surfaceReady = false;

    // --- Level / Run ---
    private float runSpeedPxPerSec = 380f;
    private float levelLengthPx = 28000f; // wird bei surfaceCreated gesetzt (60–90s)

    // --- Camera ---
    private float cameraX;
    private float groundY;

    // === One knob to move whole world up/down ===
    private float worldYOffsetPx = 0f;

    // Ground position factor (relative to screen height)
    private float groundFactor = 0.80f;

    // --- Assets / Rendering ---
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Movie playerMovie;
    private Bitmap platformBitmap;   // desktop.png
    private Bitmap spikeBitmap;      // headphonejack.png
    private Bitmap cockroachBitmap;  // bug.png
    private Bitmap robotBitmap;      // robot.png
    private Bitmap cdBitmap;         // cd.png

    // --- Background (looping/parallax) ---
    private Bitmap bgBitmap;
    private Bitmap bgScaled;
    private int screenW, screenH;
    private int bgW; // scaled width
    private float bgParallax = 0.35f;

    // Level selection (works before surface exists)
    private int pendingLevelId = 1;

    // --- World objects ---
    private Player player;
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Spike> spikes = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<CDProjectile> projectiles = new ArrayList<>();

    // Game state
    private boolean gameOver = false;

    // --- Collectibles / PowerUps ---
    private final List<Coin> coins = new ArrayList<>();
    private final List<PowerUpEntity> powerUps = new ArrayList<>();
    private final ActivePowerUps activePowerUps = new ActivePowerUps();

    // Bitmaps
    private Bitmap coinBitmap;       // money.png
    private Bitmap powerBatteryBmp;  // battery.png
    private Bitmap powerDisketteBmp; // diskette.png

    // --- Level flavor knobs (set per level) ---
    private float gapMin = 240f;
    private float gapMax = 340f;
    private float robotIntervalBase = 1.45f;
    private float ceilingChance = 0.12f;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        // Player animation (optional)
        try (InputStream is = getResources().openRawResource(R.raw.char_run_mini)) {
            playerMovie = Movie.decodeStream(is);
        } catch (Exception e) {
            playerMovie = null;
        }

        platformBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.desktop);
        spikeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.headphonejack);

        cockroachBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bug);
        robotBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.robot);
        cdBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cd);

        coinBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.data_token);
        powerBatteryBmp = BitmapFactory.decodeResource(getResources(), R.drawable.battery);
        powerDisketteBmp = BitmapFactory.decodeResource(getResources(), R.drawable.diskette);

        // default background
        bgBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gamescreen);
    }

    public void setGameEventListener(GameEventListener listener) {
        this.listener = listener;
    }

    // ----------------------------
    // WORLD HEIGHT / GROUND CONTROL
    // ----------------------------

    public void setWorldYOffsetPx(float px) {
        this.worldYOffsetPx = px;

        if (!surfaceReady) return;

        recomputeGroundY();

        // If already running, rebuild so ALL generated objects use new groundY.
        if (player != null) {
            rebuildWorldAfterThemeChange();
        }
    }

    private void recomputeGroundY() {
        groundY = (getHeight() * groundFactor) + worldYOffsetPx;

        // clamp: prevent extreme offsets from breaking everything
        if (groundY < getHeight() * 0.35f) groundY = getHeight() * 0.35f;
        if (groundY > getHeight() * 0.92f) groundY = getHeight() * 0.92f;
    }

    // ----------------------------
    // LEVEL THEME (BACKGROUND + OFFSET)
    // ----------------------------

    /**
     * Called from Activity. Safe to call BEFORE surfaceCreated.
     */
    public void applyLevelId(int levelId) {
        pendingLevelId = levelId;
        if (!surfaceReady) return;
        applyLevelIdInternal(levelId);
    }

    /**
     * Must only be called when surfaceReady==true (screen size known).
     */
    private void applyLevelIdInternal(int levelId) {
        int bgRes;
        float yOffset;

        switch (levelId) {
            case 1:
            case 2:
                bgRes = R.drawable.gamescreen;
                yOffset = -560f;
                break;

            case 3:
            case 4:
                bgRes = R.drawable.gamescreen_lvl_2;
                yOffset = -460f;
                break;

            case 5:
                bgRes = R.drawable.gamescreen_lvl_3;
                yOffset = -310f;
                break;

            default:
                bgRes = R.drawable.gamescreen;
                yOffset = -560f;
                break;
        }

        // Background
        bgBitmap = BitmapFactory.decodeResource(getResources(), bgRes);
        prepareBackground();

        // Offset / ground
        worldYOffsetPx = yOffset;
        recomputeGroundY();

        // Level knobs
        applyLevelKnobs(levelId);

        // If game already running: rebuild so spawns match new ground
        if (player != null) {
            rebuildWorldAfterThemeChange();
        }
    }

    private void applyLevelKnobs(int levelId) {
        // Diese Werte verändern Silhouette/Rhythmus massiv ohne neue Assets:
        if (levelId == 1 || levelId == 2) {
            gapMin = 250f;
            gapMax = 340f;
            robotIntervalBase = 1.55f; // langsamer schießen -> entspannter
            ceilingChance = 0.08f;
        } else if (levelId == 3 || levelId == 4) {
            gapMin = 230f;
            gapMax = 360f;
            robotIntervalBase = 1.40f;
            ceilingChance = 0.20f;
        } else {
            gapMin = 215f;
            gapMax = 385f;
            robotIntervalBase = 1.28f; // aggressiver
            ceilingChance = 0.35f;
        }
    }

    /** Rebuilds the generated world while retaining the freshly placed loot layer. */
    private void rebuildWorldAfterThemeChange() {
        // erstmal State resetten
        projectiles.clear();
        activePowerUps.clear();

        // generateRun() cleared coins/powerUps already and spawns new ones
        generateRun();

        player.reset(getWidth() * 0.2f, groundY);
        cameraX = 0f;
        gameOver = false;
    }

    // ----------------------------
    // SURFACE CALLBACKS / THREAD
    // ----------------------------

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;

        screenW = getWidth();
        screenH = getHeight();

        // apply level theme now (background + offset) because now sizes are known
        applyLevelIdInternal(pendingLevelId);

        // Ground
        recomputeGroundY();

        // Apply pending config (speed etc.)
        if (pendingConfig != null) {
            applyLevelConfigInternal(pendingConfig);
        } else {
            levelLengthPx = runSpeedPxPerSec * 75f;
        }

        // Build world
        generateRun();

        // Player initial
        player = new Player(getWidth() * 0.2f, groundY, playerMovie);
        cameraX = 0f;

        thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        if (thread == null) return;
        thread.setRunning(false);
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException ignored) { }
        }
        thread = null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenW = width;
        screenH = height;

        prepareBackground();
        recomputeGroundY();

        if (player != null) {
            rebuildWorldAfterThemeChange();
        }
    }

    public void pause() {
        if (thread == null) return;
        thread.setRunning(false);
        if (Thread.currentThread() == thread) return;
        try {
            thread.join();
        } catch (InterruptedException ignored) { }
        thread = null;
    }

    public void resume() {
        if (!surfaceReady) return;
        if (thread != null) return;

        thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    // ----------------------------
    // LEVEL CONFIG (SPEED etc.)
    // ----------------------------

    public void applyLevelConfig(LevelConfig cfg) {
        if (cfg == null) return;
        pendingConfig = cfg;
        if (surfaceReady) {
            applyLevelConfigInternal(cfg);
        }
    }

    /** Applies difficulty settings and rebuilds the world when the surface is ready. */
    private void applyLevelConfigInternal(LevelConfig cfg) {
        runSpeedPxPerSec = cfg.moveSpeed;
        levelLengthPx = runSpeedPxPerSec * 75f;

        if (player != null) {
            projectiles.clear();
            activePowerUps.clear();

            // generateRun() clears + spawns coins/powerUps
            generateRun();

            player.reset(getWidth() * 0.2f, groundY);
            cameraX = 0f;
            gameOver = false;
        }
    }

    // ----------------------------
    // BACKGROUND
    // ----------------------------

    private void prepareBackground() {
        if (bgBitmap == null || screenH <= 0) return;

        float scale = (float) screenH / (float) bgBitmap.getHeight();
        bgW = Math.max(1, Math.round(bgBitmap.getWidth() * scale));
        bgScaled = Bitmap.createScaledBitmap(bgBitmap, bgW, screenH, true);
    }

    private void drawLoopingBackground(Canvas canvas) {
        if (bgScaled == null) return;

        float scrollX = cameraX * bgParallax;
        float x = - (scrollX % bgW);
        if (x > 0) x -= bgW;

        while (x < screenW) {
            canvas.drawBitmap(bgScaled, x, 0f, null);
            x += bgW;
        }
    }

    // ----------------------------
    // INPUT
    // ----------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (player == null) return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                player.onJumpPressed();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                player.onJumpReleased();
                return true;
        }
        return super.onTouchEvent(event);
    }

    // ----------------------------
    // UPDATE LOOP
    // ----------------------------

    public void update(float dt) {
        if (player == null) return;
        if (gameOver) return;

        player.update(
                dt,
                runSpeedPxPerSec,
                groundY,
                platforms,
                spikes,
                enemies,
                () -> { if (listener != null) listener.onHitObstacle(); },
                () -> {
                    gameOver = true;
                    if (listener != null) listener.onGameOver();
                    pause();
                }
        );

        // enemies update
        float px = player.getX();
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (!e.isAlive()) {
                enemies.remove(i);
                continue;
            }
            RectF b = e.getBounds();
            if (b.right < px - 500f || b.left > px + 1400f) continue;
            e.update(dt, platforms);
        }

        // PowerUps timers
        activePowerUps.update(dt);

        // Coins collisions (magnet)
        RectF prCoin = player.getRect();
        boolean magnet = activePowerUps.isActive(PowerUpType.MAGNET);
        float pcx = prCoin.centerX();
        float pcy = prCoin.centerY();
        float magnetRadius = 520f;

        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin c = coins.get(i);

            if (magnet) {
                RectF cb = c.getBounds();
                float dx = cb.centerX() - pcx;
                float dy = cb.centerY() - pcy;
                if (dx * dx + dy * dy < magnetRadius * magnetRadius) {
                    c.pullTowards(pcx, pcy, 180000f, dt);
                }
            }

            RectF cb = c.getBounds();
            if (cb.right < cameraX - 250f) {
                coins.remove(i);
                continue;
            }

            if (RectF.intersects(prCoin, cb)) {
                coins.remove(i);
                int add = activePowerUps.isActive(PowerUpType.DOUBLE_COINS) ? 2 : 1;
                if (listener != null) {
                    for (int n = 0; n < add; n++) listener.onCollectiblePicked();
                }
            }
        }

        // PowerUps collisions
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUpEntity pu = powerUps.get(i);

            RectF pb = pu.getBounds();
            if (pb.right < cameraX - 250f) {
                powerUps.remove(i);
                continue;
            }

            if (RectF.intersects(prCoin, pb)) {
                PowerUpType t = pu.getType();
                float duration;
                switch (t) {
                    case INVINCIBLE: duration = 3.0f; break;
                    case MAGNET: duration = 7.0f; break;
                    case DOUBLE_COINS: duration = 9.0f; break;
                    default: duration = 6.0f; break;
                }
                activePowerUps.activate(t, duration);

                if (t == PowerUpType.INVINCIBLE) {
                    player.grantShield(duration);
                }

                powerUps.remove(i);
                if (listener != null) listener.onCollectiblePicked();
            }
        }

        // camera follow
        float followOffset = getWidth() * 0.28f;
        cameraX = player.getX() - followOffset;
        if (cameraX < 0f) cameraX = 0f;

        // robot shooting
        final float cdSpeed = 620f;
        for (Enemy e : enemies) {
            if (e instanceof RobotEnemy) {
                ((RobotEnemy) e).tryShootIfVisible(cameraX, (float) screenW, projectiles, cdSpeed);
            }
        }

        // projectiles update + collision
        RectF pr = player.getRect();
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            CDProjectile p = projectiles.get(i);
            p.update(dt);

            if (p.bounds.right < cameraX - 250f || p.bounds.left > cameraX + screenW + 250f) {
                projectiles.remove(i);
                continue;
            }

            if (RectF.intersects(pr, p.bounds)) {
                player.tryDamage(
                        () -> { if (listener != null) listener.onHitObstacle(); },
                        () -> {
                            gameOver = true;
                            if (listener != null) listener.onGameOver();
                            pause();
                        }
                );

                if (i >= 0 && i < projectiles.size()) {
                    projectiles.remove(i);
                }
            }
        }
    }

    // ----------------------------
    // DRAW
    // ----------------------------

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Background
        drawLoopingBackground(canvas);

        // World
        canvas.save();
        canvas.translate(-cameraX, 0);

        float leftBound = cameraX - 250f;
        float rightBound = cameraX + getWidth() + 250f;

        for (Platform p : platforms) {
            RectF plat = p.bounds;
            if (plat.right < leftBound || plat.left > rightBound) continue;
            p.draw(canvas, platformBitmap, paint);
        }

        for (Spike s : spikes) {
            RectF sp = s.bounds;
            if (sp.right < leftBound || sp.left > rightBound) continue;
            s.draw(canvas, spikeBitmap, paint);
        }

        for (Enemy e : enemies) {
            RectF b = e.getBounds();
            if (b.right < leftBound || b.left > rightBound) continue;
            e.draw(canvas, paint);
        }

        for (Coin c : coins) {
            RectF b = c.getBounds();
            if (b.right < leftBound || b.left > rightBound) continue;
            c.draw(canvas, coinBitmap, paint);
        }

        for (PowerUpEntity pu : powerUps) {
            RectF b = pu.getBounds();
            if (b.right < leftBound || b.left > rightBound) continue;
            Bitmap bmp = null;
            if (pu.getType() == PowerUpType.INVINCIBLE) bmp = powerBatteryBmp;
            else if (pu.getType() == PowerUpType.MAGNET) bmp = powerDisketteBmp;
            else if (pu.getType() == PowerUpType.DOUBLE_COINS) bmp = coinBitmap;
            pu.draw(canvas, bmp, paint);
        }

        paint.setColor(Color.WHITE);
        if (player != null) player.draw(canvas, paint);

        for (CDProjectile p : projectiles) {
            p.draw(canvas, cdBitmap, paint);
        }

        canvas.restore();
    }

    // --------------------------
    // Pattern-based Mapbuilding
    // --------------------------

    private enum PatternType {
        LOW_HIGH,
        SPIKE_TIMING,
        NARROW_PATROL,
        FAKE_SAFE,
        MULTI_HOP,
        COIN_RAIL,
        SINGLE_GAP,
        SAFE_REWARD,
        STAIRCASE_UP,
        STAIRCASE_DOWN,
        SPLIT_CHOICE,
        ROBOT_GAUNTLET_LIGHT,
        LOW_CEILING,
        ZIGZAG_HOP,
        TIMED_COMMIT
    }

    private void generateRun() {
        platforms.clear();
        spikes.clear();
        enemies.clear();
        projectiles.clear();
        coins.clear();
        powerUps.clear();

        // Scale enemy sprites once
        if (cockroachBitmap != null && cockroachBitmap.getWidth() > 0) {
            if (cockroachBitmap.getWidth() > 200) {
                int newW = Math.max(1, Math.round(cockroachBitmap.getWidth() * 0.55f));
                int newH = Math.max(1, Math.round(cockroachBitmap.getHeight() * 0.55f));
                cockroachBitmap = Bitmap.createScaledBitmap(cockroachBitmap, newW, newH, true);
            }
        }
        if (robotBitmap != null && robotBitmap.getWidth() > 0) {
            if (robotBitmap.getWidth() > 220) {
                int newW = Math.max(1, Math.round(robotBitmap.getWidth() * 0.55f));
                int newH = Math.max(1, Math.round(robotBitmap.getHeight() * 0.55f));
                robotBitmap = Bitmap.createScaledBitmap(robotBitmap, newW, newH, true);
            }
        }

        final float tileW = 180f;
        final float tileH = 55f;

        final float spikeW = 120f;
        final float spikeH = 90f;

        final float laneLow  = groundY - 180f;
        final float laneMid  = groundY - 240f;
        final float laneHigh = groundY - 320f;

        float x = getWidth() * 1.2f;

        PatternType last1 = null;
        PatternType last2 = null;

        while (x < levelLengthPx - 1200f) {
            float progress = x / Math.max(1f, levelLengthPx);

            PatternType p = pickPattern(this.pendingLevelId, progress, last1, last2);

            float end = addPattern(p, x, tileW, tileH, spikeW, spikeH, laneLow, laneMid, laneHigh);

            maybeAddGroundHazard(end, progress, spikeW, spikeH, tileW, p);

            // shift history
            last2 = last1;
            last1 = p;

            x = end + randRange(260f, 520f);
        }

        // EINMALIGES Loot-Layer
        placeStaticCollectibles();
    }

    // -----------------------------------
    // Hazards: level + pattern aware
    // -----------------------------------

    private void maybeAddGroundHazard(float patternEndX,
                                      float progress,
                                      float spikeW, float spikeH,
                                      float tileW,
                                      PatternType lastPattern) {

        // Level base pressure
        float base;
        if (pendingLevelId <= 2) base = 0.20f;
        else if (pendingLevelId <= 4) base = 0.36f;
        else base = 0.50f;

        // Reward/Flow-Patterns nicht sofort kaputt machen
        if (lastPattern == PatternType.SAFE_REWARD || lastPattern == PatternType.COIN_RAIL) {
            base *= 0.35f;
        }

        // late gets slightly more
        float prob = base + progress * 0.15f;
        if (Math.random() > prob) return;

        float desiredLeft = patternEndX + randRange(240f, 420f);

        // Robots: sehr wenig in L1/2, mehr später
        float robotChance;
        if (pendingLevelId <= 2) robotChance = 0.05f;
        else if (pendingLevelId <= 4) robotChance = 0.10f;
        else robotChance = 0.14f;

        boolean spawnRobot = (progress > 0.35f) && (Math.random() < robotChance);

        if (spawnRobot) {
            float rw = 85f;
            float rh = 85f;

            // Robot interval scales with progress a bit
            float interval = robotIntervalBase - (progress > 0.70f ? 0.10f : 0.00f);

            float left = findSafeGroundSpikeX(desiredLeft, desiredLeft + rw, 180f);
            if (left > 0f && left < levelLengthPx - 1200f) {
                spawnRobotOnPlatform(left, groundY, rw, rh, interval);
            }
            return;
        }

        // spike clusters
        int count = 1;
        double r = Math.random();
        if (r < 0.40) count = 2;
        if (r < 0.16) count = 3;

        float clearance = 170f;
        addGroundSpikeClusterSafe(desiredLeft, count, spikeW, spikeH, clearance);
    }

    // -----------------------------------
    // Pattern selection with history + knobs
    // -----------------------------------

    private PatternType pickPattern(int levelId, float progress, PatternType last1, PatternType last2) {
        PatternType[] early;
        PatternType[] mid;
        PatternType[] late;

        if (levelId == 1 || levelId == 2) {
            early = new PatternType[]{
                    PatternType.LOW_HIGH, PatternType.SINGLE_GAP, PatternType.COIN_RAIL, PatternType.NARROW_PATROL
            };
            mid = new PatternType[]{
                    PatternType.SPIKE_TIMING, PatternType.LOW_HIGH, PatternType.SAFE_REWARD, PatternType.MULTI_HOP
            };
            late = new PatternType[]{
                    PatternType.MULTI_HOP, PatternType.FAKE_SAFE, PatternType.STAIRCASE_UP, PatternType.SPIKE_TIMING
            };

        } else if (levelId == 3 || levelId == 4) {
            early = new PatternType[]{
                    PatternType.STAIRCASE_UP, PatternType.SPLIT_CHOICE, PatternType.LOW_HIGH, PatternType.COIN_RAIL
            };
            mid = new PatternType[]{
                    PatternType.SPIKE_TIMING, PatternType.ROBOT_GAUNTLET_LIGHT, PatternType.MULTI_HOP, PatternType.FAKE_SAFE
            };
            late = new PatternType[]{
                    PatternType.ZIGZAG_HOP, PatternType.TIMED_COMMIT, PatternType.FAKE_SAFE, PatternType.MULTI_HOP
            };

        } else {
            early = new PatternType[]{
                    PatternType.LOW_CEILING, PatternType.STAIRCASE_DOWN, PatternType.SPLIT_CHOICE, PatternType.SPIKE_TIMING
            };
            mid = new PatternType[]{
                    PatternType.LOW_CEILING, PatternType.TIMED_COMMIT, PatternType.ROBOT_GAUNTLET_LIGHT, PatternType.ZIGZAG_HOP
            };
            late = new PatternType[]{
                    PatternType.LOW_CEILING, PatternType.TIMED_COMMIT, PatternType.ZIGZAG_HOP, PatternType.MULTI_HOP
            };
        }

        PatternType[] pool = (progress < 0.33f) ? early : (progress < 0.72f ? mid : late);

        // Try up to N times to avoid repeats and respect ceilingChance
        for (int attempt = 0; attempt < 10; attempt++) {
            PatternType chosen = pool[(int) (Math.random() * pool.length)];

            // hard avoid immediate repeat
            if (last1 != null && chosen == last1) continue;

            // soft avoid last2
            if (last2 != null && chosen == last2 && Math.random() < 0.65) continue;

            // ceiling gating (so nicht dauernd low_ceiling überall dominiert)
            if (chosen == PatternType.LOW_CEILING && Math.random() > ceilingChance) continue;

            // in Level 1/2: Robot-Gauntlet extrem selten
            if (pendingLevelId <= 2 && chosen == PatternType.ROBOT_GAUNTLET_LIGHT && Math.random() < 0.70) continue;

            return chosen;
        }

        // fallback
        PatternType fallback = pool[(int) (Math.random() * pool.length)];
        if (fallback == PatternType.LOW_CEILING && Math.random() > ceilingChance) {
            fallback = PatternType.LOW_HIGH;
        }
        return fallback;
    }

    private float addPattern(PatternType type,
                             float startX,
                             float tileW, float tileH,
                             float spikeW, float spikeH,
                             float laneLow, float laneMid, float laneHigh) {

        switch (type) {
            case LOW_HIGH:
                return patternLowHigh(startX, tileW, tileH, spikeW, spikeH, laneLow, laneMid);

            case SPIKE_TIMING:
                return patternSpikeTiming(startX, tileW, tileH, spikeW, spikeH, laneMid);

            case NARROW_PATROL:
                return patternNarrowPatrol(startX, tileW, tileH, laneHigh);

            case FAKE_SAFE:
                return patternFakeSafe(startX, tileW, tileH, spikeW, spikeH, laneMid);

            case COIN_RAIL:
                return patternCoinRail(startX, tileW, tileH, laneLow, laneMid, laneHigh);

            case SINGLE_GAP:
                return patternSingleGap(startX, tileW, tileH, spikeW, spikeH, laneMid);

            case SAFE_REWARD:
                return patternSafeReward(startX, tileW, tileH, laneMid);

            case STAIRCASE_UP:
                return patternStaircase(startX, tileW, tileH, laneLow, laneMid, laneHigh, true);

            case STAIRCASE_DOWN:
                return patternStaircase(startX, tileW, tileH, laneLow, laneMid, laneHigh, false);

            case SPLIT_CHOICE:
                return patternSplitChoice(startX, tileW, tileH, laneLow, laneMid, laneHigh);

            case ROBOT_GAUNTLET_LIGHT:
                return patternRobotGauntletLight(startX, tileW, tileH, spikeW, spikeH, laneMid);

            case LOW_CEILING:
                return patternLowCeiling(startX, tileW, tileH, spikeW, spikeH, laneMid);

            case ZIGZAG_HOP:
                return patternZigZagHop(startX, tileW, tileH, spikeW, spikeH, laneLow, laneMid, laneHigh);

            case TIMED_COMMIT:
                return patternTimedCommit(startX, tileW, tileH, spikeW, spikeH, laneMid);

            case MULTI_HOP:
            default:
                return patternMultiHop(startX, tileW, tileH, spikeW, spikeH, laneLow, laneMid, laneHigh);
        }
    }

    // --- Pattern implementations ---

    private float patternLowHigh(float x, float tileW, float tileH, float spikeW, float spikeH, float laneLow, float laneMid) {
        int tiles = (int) randRange(4f, 7f);
        float platW = tiles * tileW;
        float platTop = laneMid;
        float platLeft = x + randRange(220f, 320f);
        float platRight = platLeft + platW;

        platforms.add(new Platform(new RectF(platLeft, platTop, platRight, platTop + tileH), tileW, tileH));

        float spikeClusterLeft = x + randRange(40f, 120f);
        int count = (Math.random() < 0.35) ? 2 : 1;
        addGroundSpikeClusterSafe(spikeClusterLeft, count, spikeW, spikeH, 170f);

        if (Math.random() < 0.55) {
            spawnCockroachOnPlatform(platLeft + randRange(40f, Math.max(60f, platW - 110f)), platTop, 70f, 40f, 190f);
        }
        return Math.max(platRight, spikeClusterLeft + count * (spikeW + 6f)) + randRange(220f, 360f);
    }

    private float patternSpikeTiming(float x, float tileW, float tileH, float spikeW, float spikeH, float laneMid) {
        int spikeCount = 2 + (Math.random() < 0.25 ? 1 : 0);
        float clusterLeft = x + randRange(90f, 140f);
        addGroundSpikeClusterSafe(clusterLeft, spikeCount, spikeW, spikeH, 200f);

        int tiles = (int) randRange(3f, 5f);
        float platW = tiles * tileW;
        float platTop = laneMid;
        float platLeft = clusterLeft + spikeCount * (spikeW + 6f) + randRange(260f, 340f);
        float platRight = platLeft + platW;
        platforms.add(new Platform(new RectF(platLeft, platTop, platRight, platTop + tileH), tileW, tileH));

        float interval = robotIntervalBase + (pendingLevelId <= 2 ? 0.08f : 0.00f);
        spawnRobotOnPlatform(platRight - 85f - 16f, platTop, 85f, 85f, interval);

        return platRight + randRange(240f, 420f);
    }

    private float patternNarrowPatrol(float x, float tileW, float tileH, float laneHigh) {
        int tiles = (int) randRange(3f, 4f);
        float platW = tiles * tileW;
        float platTop = laneHigh;
        float platLeft = x + randRange(240f, 360f);
        float platRight = platLeft + platW;

        platforms.add(new Platform(new RectF(platLeft, platTop, platRight, platTop + tileH), tileW, tileH));
        spawnCockroachOnPlatform(platLeft + randRange(20f, Math.max(40f, platW - 90f)), platTop, 70f, 40f, 210f);

        if (Math.random() < 0.45) {
            addGroundSpikeClusterSafe(platRight + randRange(240f, 340f), 1, 120f, 90f, 180f);
        }

        return platRight + randRange(260f, 420f);
    }

    private float patternFakeSafe(float x, float tileW, float tileH, float spikeW, float spikeH, float laneMid) {
        int tiles = (int) randRange(6f, 9f);
        float platW = tiles * tileW;
        float platTop = laneMid;
        float platLeft = x + randRange(240f, 340f);
        float platRight = platLeft + platW;
        platforms.add(new Platform(new RectF(platLeft, platTop, platRight, platTop + tileH), tileW, tileH));

        int spCount = (Math.random() < 0.35) ? 2 : 1;
        float spTop = platTop - spikeH * 0.75f;
        float spLeft = platLeft + platW * 0.45f;
        float gap = 8f;
        for (int i = 0; i < spCount; i++) {
            float sx = spLeft + i * (spikeW + gap);
            RectF db = new RectF(sx, spTop, sx + spikeW, spTop + spikeH);
            spikes.add(new Spike(db));
        }

        if (Math.random() < 0.55) {
            float interval = robotIntervalBase + 0.05f;
            spawnRobotOnPlatform(platRight - 85f - 18f, platTop, 85f, 85f, interval);
        } else {
            spawnCockroachOnPlatform(platLeft + platW * 0.2f, platTop, 70f, 40f, 185f);
        }

        return platRight + randRange(240f, 420f);
    }

    private float patternMultiHop(float x, float tileW, float tileH, float spikeW, float spikeH,
                                  float laneLow, float laneMid, float laneHigh) {
        int hops = (Math.random() < 0.45) ? 3 : 2;

        float cur = x + randRange(220f, 320f);
        float lastRight = cur;

        float[] lanes = new float[]{laneLow, laneMid, laneHigh};

        for (int i = 0; i < hops; i++) {
            int tiles = (i == 0) ? (int) randRange(3f, 5f) : (int) randRange(2f, 4f);
            float w = tiles * tileW;
            float top = lanes[(int) (Math.random() * lanes.length)];

            float gap = (i == 0) ? randRange(gapMin, gapMax) : randRange(gapMin - 20f, gapMax - 10f);
            if (i == 0) cur = cur;
            else cur = lastRight + gap;

            float left = cur;
            float right = left + w;

            platforms.add(new Platform(new RectF(left, top, right, top + tileH), tileW, tileH));

            if (i == hops - 1) {
                if (Math.random() < 0.55) {
                    float interval = robotIntervalBase - (pendingLevelId >= 5 ? 0.08f : 0.00f);
                    spawnRobotOnPlatform(right - 85f - 16f, top, 85f, 85f, interval);
                } else {
                    float sTop = top - spikeH * 0.75f;
                    float sx = left + w * 0.55f;
                    RectF db = new RectF(sx, sTop, sx + spikeW, sTop + spikeH);
                    spikes.add(new Spike(db));
                }
            }

            lastRight = right;
        }

        return lastRight + randRange(260f, 460f);
    }

    // ============================================================
    // ADDITIONAL PATTERNS
    // ============================================================

    private float patternCoinRail(float x, float tileW, float tileH,
                                  float laneLow, float laneMid, float laneHigh) {
        int tiles = (int) randRange(5f, 8f);
        float platW = tiles * tileW;

        float platTop = (Math.random() < 0.7) ? laneMid : laneHigh;
        float platLeft = x + randRange(220f, 340f);
        float platRight = platLeft + platW;

        platforms.add(new Platform(new RectF(platLeft, platTop, platRight, platTop + tileH), tileW, tileH));

        final float coinSize = 52f;
        float y = platTop - coinSize - 18f;

        int n = (platW > 700f) ? 7 : (platW > 520f ? 6 : 5);
        float spacing = coinSize * 1.35f;

        float startX = platLeft + randRange(50f, Math.max(50f, platW - (n * spacing) - 50f));
        for (int i = 0; i < n; i++) {
            float cx = startX + i * spacing;
            tryAddCoinNoOverlap(cx, y, coinSize);
        }

        if (Math.random() < (pendingLevelId <= 2 ? 0.18 : 0.30)) {
            addGroundSpikeClusterSafe(x + randRange(60f, 150f), 1, 120f, 90f, 170f);
        }

        return platRight + randRange(260f, 440f);
    }

    private float patternSingleGap(float x, float tileW, float tileH,
                                   float spikeW, float spikeH,
                                   float laneMid) {
        float start = x + randRange(220f, 320f);

        int tilesA = (int) randRange(3f, 5f);
        int tilesB = (int) randRange(3f, 5f);

        float wA = tilesA * tileW;
        float wB = tilesB * tileW;

        float gap = randRange(gapMin, gapMax);

        float aL = start;
        float aR = aL + wA;

        float bL = aR + gap;
        float bR = bL + wB;

        platforms.add(new Platform(new RectF(aL, laneMid, aR, laneMid + tileH), tileW, tileH));
        platforms.add(new Platform(new RectF(bL, laneMid, bR, laneMid + tileH), tileW, tileH));

        if (Math.random() < 0.45) {
            addGroundSpikeClusterSafe(aR - randRange(220f, 300f), 1, spikeW, spikeH, 170f);
        }

        if (Math.random() < 0.55) {
            spawnCockroachOnPlatform(bL + randRange(30f, Math.max(60f, wB - 110f)), laneMid, 70f, 40f, 205f);
        }

        return bR + randRange(260f, 420f);
    }

    private float patternSafeReward(float x, float tileW, float tileH, float laneMid) {
        int tiles = (int) randRange(7f, 11f);
        float platW = tiles * tileW;

        float platLeft = x + randRange(220f, 340f);
        float platRight = platLeft + platW;

        platforms.add(new Platform(new RectF(platLeft, laneMid, platRight, laneMid + tileH), tileW, tileH));

        final float coinSize = 52f;
        int n = 6;
        float spacing = coinSize * 1.35f;
        float startX = platLeft + randRange(60f, Math.max(60f, platW - (n * spacing) - 60f));

        for (int i = 0; i < n; i++) {
            float cx = startX + i * spacing;
            float t = (n <= 1) ? 0f : (i / (float) (n - 1));
            float arch = (float) Math.sin(t * Math.PI);
            float y = laneMid - coinSize - (18f + arch * 22f);
            tryAddCoinNoOverlap(cx, y, coinSize);
        }

        if (Math.random() < (pendingLevelId <= 2 ? 0.45f : 0.55f)) {
            final float powerSize = 64f;
            float px = platLeft + randRange(100f, Math.max(120f, platW - powerSize - 100f));
            float py = laneMid - powerSize - 22f;

            double pr = Math.random();
            PowerUpType type = (pr < 0.40) ? PowerUpType.MAGNET
                    : (pr < 0.72 ? PowerUpType.DOUBLE_COINS : PowerUpType.INVINCIBLE);

            powerUps.add(new PowerUpEntity(type, px, py, powerSize, 0f));
        }

        return platRight + randRange(280f, 520f);
    }

    private float patternStaircase(float x, float tileW, float tileH,
                                   float laneLow, float laneMid, float laneHigh,
                                   boolean up) {
        float start = x + randRange(220f, 320f);

        float[] topsUp   = new float[]{laneLow, laneMid, laneHigh};
        float[] topsDown = new float[]{laneHigh, laneMid, laneLow};
        float[] tops = up ? topsUp : topsDown;

        float curL = start;
        float lastR = start;

        for (int i = 0; i < 3; i++) {
            int tiles = (i == 0) ? (int) randRange(4f, 6f) : (int) randRange(3f, 5f);
            float w = tiles * tileW;

            float gap = (i == 0) ? randRange(gapMin - 10f, gapMax - 40f)
                    : randRange(gapMin - 25f, gapMax - 55f);

            if (i == 0) curL = start;
            else curL = lastR + gap;

            float top = tops[i];
            float curR = curL + w;

            platforms.add(new Platform(new RectF(curL, top, curR, top + tileH), tileW, tileH));

            if (Math.random() < 0.55) {
                final float coinSize = 52f;
                float cy = top - coinSize - 18f;
                int n = 3;
                float spacing = coinSize * 1.30f;
                float startX = curL + randRange(40f, Math.max(40f, w - (n * spacing) - 40f));
                for (int k = 0; k < n; k++) {
                    tryAddCoinNoOverlap(startX + k * spacing, cy, coinSize);
                }
            }

            lastR = curR;
        }

        if (Math.random() < 0.35) {
            addGroundSpikeClusterSafe(lastR + randRange(260f, 340f), 1, 120f, 90f, 170f);
        }

        return lastR + randRange(260f, 460f);
    }

    private float patternSplitChoice(float x, float tileW, float tileH,
                                     float laneLow, float laneMid, float laneHigh) {
        float start = x + randRange(220f, 340f);

        int lowTiles = (int) randRange(7f, 10f);
        float lowW = lowTiles * tileW;

        int highTiles = (int) randRange(4f, 6f);
        float highW = highTiles * tileW;

        float lowL = start;
        float lowR = lowL + lowW;

        float highL = lowL + randRange(260f, 360f);
        float highR = highL + highW;

        platforms.add(new Platform(new RectF(lowL, laneLow, lowR, laneLow + tileH), tileW, tileH));
        platforms.add(new Platform(new RectF(highL, laneHigh, highR, laneHigh + tileH), tileW, tileH));

        final float coinSize = 52f;
        float yHigh = laneHigh - coinSize - 18f;
        int n = 5;
        float spacing = coinSize * 1.35f;
        float railStart = highL + randRange(40f, Math.max(40f, highW - (n * spacing) - 40f));
        for (int i = 0; i < n; i++) {
            tryAddCoinNoOverlap(railStart + i * spacing, yHigh, coinSize);
        }

        if (Math.random() < 0.45) {
            final float powerSize = 64f;
            float px = highL + randRange(80f, Math.max(90f, highW - powerSize - 80f));
            float py = laneHigh - powerSize - 22f;
            PowerUpType type = (Math.random() < 0.50) ? PowerUpType.MAGNET : PowerUpType.DOUBLE_COINS;
            powerUps.add(new PowerUpEntity(type, px, py, powerSize, 0f));
        }

        if (Math.random() < 0.50) {
            spawnCockroachOnPlatform(highL + randRange(25f, Math.max(60f, highW - 110f)), laneHigh, 70f, 40f, 220f);
        }

        return Math.max(lowR, highR) + randRange(260f, 460f);
    }

    private float patternRobotGauntletLight(float x, float tileW, float tileH,
                                            float spikeW, float spikeH,
                                            float laneMid) {
        float spikeLeft = x + randRange(90f, 160f);
        int count = (Math.random() < 0.25) ? 2 : 1;
        addGroundSpikeClusterSafe(spikeLeft, count, spikeW, spikeH, 200f);

        int tiles = (int) randRange(3f, 5f);
        float w = tiles * tileW;

        float platLeft = spikeLeft + count * (spikeW + 6f) + randRange(260f, 340f);
        float platRight = platLeft + w;

        platforms.add(new Platform(new RectF(platLeft, laneMid, platRight, laneMid + tileH), tileW, tileH));

        float interval = robotIntervalBase - (pendingLevelId >= 5 ? 0.10f : 0.00f);
        spawnRobotOnPlatform(platRight - 85f - 16f, laneMid, 85f, 85f, interval);

        if (Math.random() < 0.55) {
            final float coinSize = 52f;
            float y = laneMid - coinSize - 18f;
            int n = 4;
            float spacing = coinSize * 1.35f;
            float startX = platLeft + randRange(40f, Math.max(40f, w - (n * spacing) - 40f));
            for (int i = 0; i < n; i++) {
                tryAddCoinNoOverlap(startX + i * spacing, y, coinSize);
            }
        }

        return platRight + randRange(260f, 420f);
    }

    private float patternLowCeiling(float x, float tileW, float tileH,
                                    float spikeW, float spikeH,
                                    float laneMid) {
        int tiles = (int) randRange(6f, 9f);
        float w = tiles * tileW;

        float platLeft = x + randRange(240f, 340f);
        float platRight = platLeft + w;

        platforms.add(new Platform(new RectF(platLeft, laneMid, platRight, laneMid + tileH), tileW, tileH));

        int spCount = (Math.random() < 0.40) ? 3 : 2;
        float sTop = laneMid - spikeH * 0.75f;
        float gap = 8f;

        float anchor = platLeft + w * randRange(0.35f, 0.55f);
        for (int i = 0; i < spCount; i++) {
            float sx = anchor + i * (spikeW + gap);
            spikes.add(new Spike(new RectF(sx, sTop, sx + spikeW, sTop + spikeH)));
        }

        if (Math.random() < 0.50) {
            float interval = robotIntervalBase - 0.05f;
            spawnRobotOnPlatform(platRight - 85f - 18f, laneMid, 85f, 85f, interval);
        } else if (Math.random() < 0.55) {
            spawnCockroachOnPlatform(platLeft + w * 0.2f, laneMid, 70f, 40f, 210f);
        }

        return platRight + randRange(260f, 420f);
    }

    private float patternZigZagHop(float x, float tileW, float tileH,
                                   float spikeW, float spikeH,
                                   float laneLow, float laneMid, float laneHigh) {
        int hops = (Math.random() < 0.55) ? 3 : 2;

        float[] lanes = new float[]{laneLow, laneMid, laneHigh};
        float curL = x + randRange(220f, 320f);
        float lastR = curL;

        float prevTop = -9999f;

        for (int i = 0; i < hops; i++) {
            int tiles = (i == 0) ? (int) randRange(3f, 5f) : (int) randRange(2f, 4f);
            float w = tiles * tileW;

            float gap = (i == 0) ? randRange(gapMin, gapMax) : randRange(gapMin - 25f, gapMax - 25f);
            if (i == 0) curL = curL;
            else curL = lastR + gap;

            float top = lanes[(int) (Math.random() * lanes.length)];
            if (Math.abs(top - prevTop) < 1f) {
                top = lanes[(int) (Math.random() * lanes.length)];
            }
            prevTop = top;

            float curR = curL + w;
            platforms.add(new Platform(new RectF(curL, top, curR, top + tileH), tileW, tileH));

            if (Math.random() < 0.55) {
                final float coinSize = 52f;
                float y = top - coinSize - 18f;
                int n = (w > 520f) ? 4 : 3;
                float spacing = coinSize * 1.35f;
                float startX = curL + randRange(35f, Math.max(35f, w - (n * spacing) - 35f));
                for (int k = 0; k < n; k++) {
                    tryAddCoinNoOverlap(startX + k * spacing, y, coinSize);
                }
            }

            if (i == hops - 1) {
                if (Math.random() < 0.55) {
                    addGroundSpikeClusterSafe(curR + randRange(240f, 340f), 1, spikeW, spikeH, 180f);
                } else if (Math.random() < 0.55) {
                    spawnCockroachOnPlatform(curL + w * 0.55f, top, 70f, 40f, 220f);
                }
            }

            lastR = curR;
        }

        return lastR + randRange(260f, 460f);
    }

    private float patternTimedCommit(float x, float tileW, float tileH,
                                     float spikeW, float spikeH,
                                     float laneMid) {
        int spikeCount = 2 + (Math.random() < 0.35 ? 1 : 0);
        float clusterLeft = x + randRange(90f, 140f);
        addGroundSpikeClusterSafe(clusterLeft, spikeCount, spikeW, spikeH, 210f);

        int tiles = (int) randRange(2f, 4f);
        float w = tiles * tileW;

        float platLeft = clusterLeft + spikeCount * (spikeW + 6f) + randRange(220f, 300f);
        float platRight = platLeft + w;

        platforms.add(new Platform(new RectF(platLeft, laneMid, platRight, laneMid + tileH), tileW, tileH));

        if (Math.random() < 0.55) {
            float interval = robotIntervalBase - 0.10f;
            spawnRobotOnPlatform(platRight - 85f - 16f, laneMid, 85f, 85f, interval);
        } else {
            float sTop = laneMid - spikeH * 0.75f;
            float sx = platLeft + w * 0.55f;
            spikes.add(new Spike(new RectF(sx, sTop, sx + spikeW, sTop + spikeH)));
        }

        return platRight + randRange(240f, 380f);
    }

    // ============================================================
    // COIN ANTI-OVERLAP HELPERS
    // ============================================================

    private boolean tryAddCoinNoOverlap(float x, float y, float size) {
        RectF nb = new RectF(x, y, x + size, y + size);

        for (Spike s : spikes) {
            if (RectF.intersects(nb, s.bounds)) return false;
        }

        for (Platform p : platforms) {
            if (RectF.intersects(nb, p.bounds)) return false;
        }

        for (Coin c : coins) {
            if (RectF.intersects(nb, c.getBounds())) return false;
        }

        coins.add(new Coin(x, y, size, 0f));
        return true;
    }

    // ============================================================
    // SINGLE "STATIC LOOT LAYER" (once per run)
    // ============================================================

    private void placeStaticCollectibles() {
        final float coinSize = 52f;
        final float powerSize = 64f;

        // Level-dependent densities
        float coinRailChance;
        float powerChance;
        int freeCoins;

        if (pendingLevelId <= 2) {
            coinRailChance = 0.18f;
            powerChance = 0.26f;
            freeCoins = 4;
        } else if (pendingLevelId <= 4) {
            coinRailChance = 0.22f;
            powerChance = 0.32f;
            freeCoins = 6;
        } else {
            coinRailChance = 0.20f; // nicht zu überladen, sonst fühlt sich's wieder gleich an
            powerChance = 0.34f;
            freeCoins = 6;
        }

        for (Platform p : platforms) {
            float pw = p.bounds.width();
            if (pw < 220f) continue;

            if (Math.random() < coinRailChance) {
                int n = (pw > 520f) ? 5 : (pw > 360f ? 4 : 3);
                float spacing = coinSize * 1.35f;
                float startX = p.bounds.left + randRange(40f, Math.max(40f, pw - (n * spacing) - 40f));
                float yTop = p.bounds.top - coinSize - 18f;

                for (int j = 0; j < n; j++) {
                    float cx = startX + j * spacing;
                    if (cx + coinSize < p.bounds.right - 20f) {
                        tryAddCoinNoOverlap(cx, yTop, coinSize);
                    }
                }
            }

            if (pw > 420f && Math.random() < powerChance) {
                float px = p.bounds.left + randRange(80f, pw - powerSize - 80f);
                float py = p.bounds.top - powerSize - 22f;

                double pr = Math.random();
                PowerUpType type = (pr < 0.40) ? PowerUpType.MAGNET
                        : (pr < 0.72 ? PowerUpType.DOUBLE_COINS : PowerUpType.INVINCIBLE);

                // simple: over platform is safe enough
                powerUps.add(new PowerUpEntity(type, px, py, powerSize, 0f));
            }
        }

        // A few free coins to break rigidity (but small number!)
        for (int k = 0; k < freeCoins; k++) {
            float px = randRange(700f, levelLengthPx - 700f);
            float y = groundY - coinSize * randRange(2.0f, 4.2f);
            tryAddCoinNoOverlap(px, y, coinSize);
        }
    }

    // --- Helpers used by patterns ---

    private void spawnCockroachOnPlatform(float x, float platformTop, float w, float h, float speed) {
        CockroachEnemy c = new CockroachEnemy(x, platformTop, w, h, speed);
        c.setSprite(cockroachBitmap);
        enemies.add(c);
    }

    /** Spawns a robot without modifying the separately managed loot layer. */
    private void spawnRobotOnPlatform(float x, float platformTop, float w, float h, float intervalSec) {
        RobotEnemy r = new RobotEnemy(x, platformTop, w, h, intervalSec);
        r.setSprite(robotBitmap);
        enemies.add(r);
    }

    private void addGroundSpikeClusterSafe(float desiredLeft, int count, float spikeW, float spikeH, float clearance) {
        float gap = 6f;
        float clusterW = count * spikeW + (count - 1) * gap;

        float left = findSafeGroundSpikeX(desiredLeft, desiredLeft + clusterW, clearance);
        if (left <= 0f) return;

        float top = groundY - spikeH * 0.75f;
        for (int i = 0; i < count; i++) {
            float sx = left + i * (spikeW + gap);
            RectF db = new RectF(sx, top, sx + spikeW, top + spikeH);
            spikes.add(new Spike(db));
        }
    }

    private float findSafeGroundSpikeX(float startX, float endX, float clearance) {
        float x = startX;
        float e = endX;
        int guard = 0;

        while (guard < 80) {
            boolean ok = true;
            for (Platform p : platforms) {
                RectF b = p.bounds;
                if (e > (b.left - clearance) && x < (b.right + clearance)) {
                    float shift = (b.right + clearance) - x + 40f;
                    x += shift;
                    e += shift;
                    ok = false;
                }
            }
            if (ok) return x;
            guard++;
        }
        return -1f;
    }

    private float randRange(float a, float b) {
        return a + (float) Math.random() * (b - a);
    }
}

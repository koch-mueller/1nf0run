package de.hsos.prog3.inforun.game.entities.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.List;

import de.hsos.prog3.inforun.game.entities.projectiles.CDProjectile;
import de.hsos.prog3.inforun.game.world.Platform;

/**
 * Robot:
 * - spawns on platforms
 * - stationary
 * - shoots CDs to the left
 *   - first shot: as soon as it becomes visible (with a short telegraph)
 *   - then every shootIntervalSec (each shot is telegraphed)
 *
 * Visibility-dependent shooting is triggered from GameView via tryShootIfVisible(...)
 * so we can use camera/screen parameters without coupling into the main update().
 */
public class RobotEnemy extends Enemy {

    /** Wind-up time before each shot (telegraph). */
    private static final float TELEGRAPH_SEC = 0.32f;

    private final float shootIntervalSec;

    /** Time until next shot is allowed (cooldown after a shot). */
    private float cooldownTimer = 0f;

    /** Telegraph countdown. While > 0, robot is "charging" a shot. */
    private float telegraphTimer = 0f;

    /** Whether we've been activated at least once (ever visible). */
    private boolean activated = false;

    /** If true, a shot is pending and will be fired when telegraphTimer reaches 0. */
    private boolean pendingShot = false;

    public RobotEnemy(float x, float yBottom, float w, float h, float shootIntervalSec) {
        this.shootIntervalSec = shootIntervalSec;
        bounds.set(x, yBottom - h, x + w, yBottom);
    }

    @Override
    public boolean isStompKillable() {
        return true; // requested
    }

    @Override
    public void update(float dt, List<Platform> platforms) {
        // stationary
        if (cooldownTimer > 0f) cooldownTimer -= dt;
        if (telegraphTimer > 0f) telegraphTimer -= dt;
    }

    public boolean isTelegraphing() {
        return pendingShot || telegraphTimer > 0f;
    }

    /** Call from GameView so we can know camera/screen visibility and spawn projectiles. */
    public void tryShootIfVisible(float cameraX, float screenW, List<CDProjectile> outProjectiles, float cdSpeedPxPerSec) {
        if (!alive) return;

        boolean visible = bounds.right >= cameraX && bounds.left <= cameraX + screenW;
        if (!visible) return;

        // First time we become visible: start a telegraph immediately.
        if (!activated) {
            activated = true;
            startTelegraph();
        }

        // If a shot is pending and telegraph finished -> fire.
        if (pendingShot && telegraphTimer <= 0f) {
            spawnCD(outProjectiles, cdSpeedPxPerSec);
            pendingShot = false;
            cooldownTimer = shootIntervalSec;
        }

        // If no shot pending and cooldown is done -> start next telegraph.
        if (!pendingShot && cooldownTimer <= 0f && telegraphTimer <= 0f) {
            startTelegraph();
        }
    }

    private void startTelegraph() {
        telegraphTimer = TELEGRAPH_SEC;
        pendingShot = true;
    }

    private void spawnCD(List<CDProjectile> out, float cdSpeedPxPerSec) {
        // Spawn CD from robot "hand" area (roughly middle-left)
        float h = bounds.height();
        float cdSize = Math.min(48f, h * 0.55f);

        float x = bounds.left - cdSize * 0.25f;
        float y = bounds.top + h * 0.45f - cdSize * 0.5f;

        out.add(new CDProjectile(x, y, cdSize, cdSize, -Math.abs(cdSpeedPxPerSec)));
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (sprite != null) {
            canvas.drawBitmap(sprite, null, bounds, null);

            // Telegraph indicator: subtle red overlay
            if (isTelegraphing() && paint != null) {
                int prevColor = paint.getColor();
                int prevAlpha = paint.getAlpha();

                paint.setColor(Color.RED);
                paint.setAlpha(80);
                canvas.drawRect(bounds, paint);

                paint.setAlpha(prevAlpha);
                paint.setColor(prevColor);
            }
        } else if (paint != null) {
            canvas.drawRect(bounds, paint);
        }
    }
}

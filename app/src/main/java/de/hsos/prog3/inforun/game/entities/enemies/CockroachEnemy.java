package de.hsos.prog3.inforun.game.entities.enemies;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.List;

import de.hsos.prog3.inforun.game.world.Platform;

/**
 * Kakerlake:
 * - spawnt auf Plattformen
 * - patrouilliert links/rechts
 * - fällt nicht runter: Edge-Check -> Richtung wechseln
 */
public class CockroachEnemy extends Enemy {

    private float vx;
    private boolean facingRight = true;

    public CockroachEnemy(float x, float platformTopY, float w, float h, float speedPxPerSec) {
        bounds.set(x, platformTopY - h, x + w, platformTopY);
        vx = speedPxPerSec;
        facingRight = vx >= 0f;
    }

    @Override
    public void update(float dt, List<Platform> platforms) {
        float prevLeft = bounds.left;
        float prevRight = bounds.right;

        bounds.offset(vx * dt, 0f);

        // Edge check: probe point slightly ahead of feet
        float probeX = (vx > 0f) ? (bounds.right + 4f) : (bounds.left - 4f);
        float probeY = bounds.bottom + 2f;

        if (!isSupportedAt(probeX, probeY, platforms)) {
            bounds.offset(prevLeft - bounds.left, 0f);
            vx = -vx;
            facingRight = vx >= 0f;
            return;
        }

        // Side collision with platform bodies (rare but keeps it stable)
        for (Platform p : platforms) {
            RectF b = p.bounds;
            if (!RectF.intersects(bounds, b)) continue;

            boolean hitFromLeft = prevRight <= b.left && bounds.right >= b.left;
            boolean hitFromRight = prevLeft >= b.right && bounds.left <= b.right;

            if (hitFromLeft || hitFromRight) {
                bounds.offset(prevLeft - bounds.left, 0f);
                vx = -vx;
                facingRight = vx >= 0f;
                return;
            }
        }
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (sprite == null) {
            canvas.drawRect(bounds, paint);
            return;
        }

        // Spiegeln, wenn nach links läuft
        if (!facingRight) {
            canvas.drawBitmap(sprite, null, bounds, null);
        } else {
            canvas.save();
            canvas.scale(-1f, 1f, bounds.centerX(), bounds.centerY());
            canvas.drawBitmap(sprite, null, bounds, null);
            canvas.restore();
        }
    }

    private boolean isSupportedAt(float x, float y, List<Platform> platforms) {
        for (Platform p : platforms) {
            RectF b = p.bounds;
            if (x >= b.left && x <= b.right) {
                // "Support" if probe is close to platform top
                if (y >= b.top - 8f && y <= b.top + 14f) return true;
            }
        }
        return false;
    }
}

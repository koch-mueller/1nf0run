package de.hsos.prog3.inforun.game.entities.collectibles;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Simple coin collectible that scrolls with the world.
 */
public class Coin {

    private final RectF bounds = new RectF();
    private float vx;

    public Coin(float x, float yTop, float size, float vx) {
        this.vx = vx;
        bounds.set(x, yTop, x + size, yTop + size);
    }

    public RectF getBounds() {
        return bounds;
    }

    public void setVx(float vx) {
        this.vx = vx;
    }

    public void update(float dt) {
        bounds.offset(vx * dt, 0f);
    }

    /**
     * Magnet helper: pulls coin towards (tx, ty) with a cheap inverse-square-ish falloff.
     * This adjusts both x and y.
     */
    public void pullTowards(float tx, float ty, float strength, float dt) {
        float cx = bounds.centerX();
        float cy = bounds.centerY();
        float dx = tx - cx;
        float dy = ty - cy;

        float len2 = dx * dx + dy * dy + 1f;
        float ax = (dx / len2) * strength;
        float ay = (dy / len2) * strength;

        bounds.offset(ax * dt, ay * dt);
    }

    public void draw(Canvas canvas, Bitmap coinBmp, Paint paint) {
        if (coinBmp != null) {
            canvas.drawBitmap(coinBmp, null, bounds, paint);
        } else {
            // fallback
            canvas.drawOval(bounds, paint);
        }
    }
}

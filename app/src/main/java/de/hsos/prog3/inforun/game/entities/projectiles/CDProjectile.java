package de.hsos.prog3.inforun.game.entities.projectiles;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Simple CD projectile in world coordinates. */
public class CDProjectile {

    public final RectF bounds = new RectF();
    private final float vx; // px/s

    // rotation
    private float angleDeg = 0f;
    private final float omegaDegPerSec = 720f; // 2 spins per second

    public CDProjectile(float x, float y, float w, float h, float vx) {
        bounds.set(x, y, x + w, y + h);
        this.vx = vx;
    }

    public void update(float dt) {
        bounds.offset(vx * dt, 0f);
        angleDeg += omegaDegPerSec * dt;
        if (angleDeg >= 360f) angleDeg -= 360f;
    }

    public void draw(Canvas canvas, Bitmap cdBitmap, Paint paint) {
        if (cdBitmap != null) {
            float cx = bounds.centerX();
            float cy = bounds.centerY();

            canvas.save();
            canvas.rotate(angleDeg, cx, cy);
            canvas.drawBitmap(cdBitmap, null, bounds, null);
            canvas.restore();
        } else if (paint != null) {
            canvas.drawRect(bounds, paint);
        }
    }
}

package de.hsos.prog3.inforun.game.entities.powerups;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * World entity that can be collected by the player to activate a timed effect.
 */
public class PowerUpEntity {

    private final RectF bounds = new RectF();
    private final PowerUpType type;
    private float vx;

    public PowerUpEntity(PowerUpType type, float x, float yTop, float size, float vx) {
        this.type = type;
        this.vx = vx;
        bounds.set(x, yTop, x + size, yTop + size);
    }

    public PowerUpType getType() { return type; }
    public RectF getBounds() { return bounds; }

    public void setVx(float vx) { this.vx = vx; }

    public void update(float dt) {
        bounds.offset(vx * dt, 0f);
    }

    public void draw(Canvas canvas, Bitmap bmp, Paint paint) {
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, bounds, paint);
        } else {
            canvas.drawRect(bounds, paint);
        }
    }
}

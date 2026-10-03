package de.hsos.prog3.inforun.game.entities.enemies;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.List;

import de.hsos.prog3.inforun.game.world.Platform;

/**
 * Basisklasse für Gegner (World-Koordinaten).
 */
public abstract class Enemy {

    protected final RectF bounds = new RectF();
    protected Bitmap sprite;
    protected boolean alive = true;

    public RectF getBounds() { return bounds; }

    public boolean isAlive() { return alive; }

    public void kill() { alive = false; }

    public void setSprite(Bitmap sprite) { this.sprite = sprite; }

    /** If true, player can kill this enemy by stomping from above. */
    public boolean isStompKillable() { return true; }

    public abstract void update(float dt, List<Platform> platforms);

    public void draw(Canvas canvas, Paint paint) {
        if (sprite != null) {
            canvas.drawBitmap(sprite, null, bounds, null);
        } else {
            canvas.drawRect(bounds, paint);
        }
    }
}

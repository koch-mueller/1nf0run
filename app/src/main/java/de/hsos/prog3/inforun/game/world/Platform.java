package de.hsos.prog3.inforun.game.world;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Plattform in World-Koordinaten.
 *
 * Collision ist ein einziges Rect (wichtig: keine Lücken).
 * Rendering kann als gekacheltes Bitmap erfolgen.
 */
public class Platform {

    public final RectF bounds;
    private final float tileW;
    private final float tileH;

    public Platform(RectF bounds) {
        this(bounds, 90f, 55f);
    }

    public Platform(RectF bounds, float tileW, float tileH) {
        this.bounds = bounds;
        this.tileW = tileW;
        this.tileH = tileH;
    }

    public void draw(Canvas canvas, Bitmap platformBitmap, Paint paint) {
        if (platformBitmap != null) {
            for (float tx = bounds.left; tx < bounds.right - 0.1f; tx += tileW) {
                RectF dst = new RectF(tx, bounds.top, tx + tileW, bounds.top + tileH);
                canvas.drawBitmap(platformBitmap, null, dst, null);
            }
        } else {
            int prev = paint.getColor();
            paint.setColor(Color.rgb(140, 140, 140));
            canvas.drawRect(bounds, paint);
            paint.setColor(prev);
        }
    }
}

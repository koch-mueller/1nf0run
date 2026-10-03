package de.hsos.prog3.inforun.game.world;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Spike (headphonejack) in World-Koordinaten.
 *
 * drawBounds: Rechteck zum Zeichnen (Sprite-Größe)
 * bounds:     Hitbox (bewusst etwas kleiner, damit es fair bleibt)
 */
public class Spike {

    /** Treffer-Hitbox */
    public final RectF bounds;

    /** Zeichen-Rechteck (Sprite) */
    public final RectF drawBounds;

    // --- Hitbox-Tuning ---
    // je größer, desto schmaler wird die Hitbox (links+rechts)
    private static final float SHRINK_W = 0.26f;   // 26% links/rechts weg
    private static final float SHRINK_TOP = 0.06f; // 6% oben weg

    public Spike(RectF drawBounds) {
        this.drawBounds = new RectF(drawBounds);

        float w = drawBounds.width();
        float h = drawBounds.height();

        float shrinkW = w * SHRINK_W;
        float shrinkTop = h * SHRINK_TOP;

        this.bounds = new RectF(
                drawBounds.left + shrinkW,
                drawBounds.top + shrinkTop,
                drawBounds.right - shrinkW,
                drawBounds.bottom
        );
    }

    public void draw(Canvas canvas, Bitmap spikeBitmap, Paint paint) {
        if (spikeBitmap != null) {
            canvas.drawBitmap(spikeBitmap, null, drawBounds, null);
        } else {
            int prev = paint.getColor();
            paint.setColor(Color.RED);
            canvas.drawRect(drawBounds, paint);
            paint.setColor(prev);
        }
    }
}

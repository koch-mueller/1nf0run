package de.hsos.prog3.inforun.game.entities;

import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;

import java.util.List;

import de.hsos.prog3.inforun.game.world.Platform;
import de.hsos.prog3.inforun.game.world.Spike;
import de.hsos.prog3.inforun.game.entities.enemies.Enemy;

/**
 * Player in World-Koordinaten:
 * - Auto-Run nach rechts (runSpeed kommt von außen)
 * - Jump + Gravity
 * - Kollisionen: Boden, Plattformen (oben landen, unten abprallen, seitlich blocken)
 * - Spikes: solid + Damage (Lives + iFrames/Blink)
 */
public class Player {

    // Physics
    private static final float GRAVITY = 2000f;
    private static final float JUMP_VELOCITY = -1100f;

    // Variable Jump (tap = short, hold = higher)
    private static final float MAX_HOLD_TIME = 0.18f; // seconds
    private static final float HOLD_BOOST = -3200f;   // vy add per second while held (negative = up)
    private static final float JUMP_CUT_MULT = 0.45f; // on release while going up

    // Jump Quality
    private static final float COYOTE_TIME = 0.12f; // 120ms

    // Dimensions (World)
    private final float w = 80f;
    private final float h = 120f;

    // State
    private float x;
    private float y;   // bottom Y
    private float vy;
    private boolean grounded;

    private float coyoteTimer = 0f;

    // Variable jump state
    private boolean jumpHeld = false;
    private float jumpHoldTimer = 0f;

    // Hit/Lives
    private int lives = 3;
    private float invulnTimer = 0f; // seconds

    // Shield visual timer (used for the invincible powerup)
    private float shieldTimer = 0f; // seconds
    private final Paint shieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Render
    private final RectF rect = new RectF();
    private final Movie runMovie;
    private long movieStartMs = 0;

    public Player(float startX, float groundY, Movie runMovie) {
        this.x = startX;
        this.y = groundY;
        this.vy = 0f;
        this.grounded = true;
        this.runMovie = runMovie;
        // translucent blue shield
        shieldPaint.setStyle(Paint.Style.STROKE);
        shieldPaint.setStrokeWidth(10f);
        shieldPaint.setColor(android.graphics.Color.argb(170, 70, 160, 255));
        syncRect();
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public RectF getRect() { return rect; }
    public int getLives() { return lives; }
    public float getInvulnTimer() { return invulnTimer; }

    public void reset(float startX, float groundY) {
        x = startX;
        y = groundY;
        vy = 0f;
        grounded = true;
        coyoteTimer = COYOTE_TIME;
        invulnTimer = 0f;
        lives = 3;
        jumpHeld = false;
        jumpHoldTimer = 0f;
        syncRect();
    }

    public void update(float dt,
                       float runSpeedPxPerSec,
                       float groundY,
                       List<Platform> platforms,
                       List<Spike> spikes,
                       List<Enemy> enemies,
                       Runnable onDamaged,   // called when a hit reduces lives
                       Runnable onDeath      // called when lives drop to 0
    ) {
        // --- Auto-Run ---
        float prevX = x;
        float prevY = y;
        x += runSpeedPxPerSec * dt;

        // timers
        if (coyoteTimer > 0f) coyoteTimer -= dt;
        if (invulnTimer > 0f) invulnTimer -= dt;
        if (shieldTimer > 0f) shieldTimer -= dt;

        // variable jump hold boost (applied before gravity)
        if (jumpHeld && vy < 0f && jumpHoldTimer < MAX_HOLD_TIME) {
            vy += HOLD_BOOST * dt;
            jumpHoldTimer += dt;
        }

        // physics
        vy += GRAVITY * dt;
        y += vy * dt;

        RectF prevRect = new RectF(prevX, prevY - h, prevX + w, prevY);
        syncRect();
        grounded = false;

        // 1) Ground collision
        if (y >= groundY) {
            y = groundY;
            vy = 0f;
            grounded = true;
            syncRect();
        }

        // 2) Platforms collision
        float prevBottom = prevRect.bottom;
        float prevTop = prevRect.top;
        float prevLeft = prevRect.left;
        float prevRight = prevRect.right;

        for (Platform p : platforms) {
            RectF plat = p.bounds;
            // coarse culling near player
            if (plat.right < x - 300f || plat.left > x + 600f) continue;
            if (!RectF.intersects(rect, plat)) continue;

            boolean fromAbove = prevBottom <= plat.top && rect.bottom >= plat.top;
            if (fromAbove) {
                y = plat.top;
                vy = 0f;
                grounded = true;
                syncRect();
                break;
            }

            boolean fromBelow = prevTop >= plat.bottom && rect.top <= plat.bottom;
            if (fromBelow) {
                y = plat.bottom + h;
                float bounceFactor = 0.35f;
                vy = Math.abs(vy) * bounceFactor;
                syncRect();
                break;
            }

            // side block (anti-clip)
            boolean hitFromLeft = prevRight <= plat.left && rect.right >= plat.left;
            boolean hitFromRight = prevLeft >= plat.right && rect.left <= plat.right;

            if (hitFromLeft) {
                x = plat.left - w - 0.01f;
                syncRect();
                break;
            } else if (hitFromRight) {
                x = plat.right + 0.01f;
                syncRect();
                break;
            }
        }

        // 3) Spikes collision (solid + damage)
        for (Spike s : spikes) {
            RectF sp = s.bounds;
            if (sp.right < x - 300f || sp.left > x + 650f) continue;
            if (!RectF.intersects(rect, sp)) continue;

            if (invulnTimer <= 0f) {
                lives--;
                invulnTimer = 1.4f;
                if (onDamaged != null) onDamaged.run();
                if (lives <= 0) {
                    if (onDeath != null) onDeath.run();
                    return;
                }
            }

            // solid-block + little bounce when landing on spike
            float prevBottom2 = prevRect.bottom;
            float prevTop2 = prevRect.top;
            float prevLeft2 = prevRect.left;
            float prevRight2 = prevRect.right;

            boolean fromAbove = prevBottom2 <= sp.top && rect.bottom >= sp.top;
            boolean fromBelow = prevTop2 >= sp.bottom && rect.top <= sp.bottom;
            boolean hitFromLeft = prevRight2 <= sp.left && rect.right >= sp.left;
            boolean hitFromRight = prevLeft2 >= sp.right && rect.left <= sp.right;

            if (fromAbove) {
                y = sp.top;
                vy = JUMP_VELOCITY * 0.55f;
                grounded = false;
                syncRect();
            } else if (fromBelow) {
                y = sp.bottom + h;
                vy = Math.abs(vy) * 0.25f;
                syncRect();
            } else if (hitFromLeft) {
                x = sp.left - w - 0.01f;
                syncRect();
            } else if (hitFromRight) {
                x = sp.right + 0.01f;
                syncRect();
            }
            break;
        }

        // 4) Enemies collision
        if (enemies != null) {
            for (Enemy e : enemies) {
                if (e == null || !e.isAlive()) continue;

                RectF eb = e.getBounds();
                if (eb.right < x - 320f || eb.left > x + 750f) continue;
                if (!RectF.intersects(rect, eb)) continue;

                // Stomp: falling and coming from above onto enemy top
                boolean stomp = (vy > 0f) && (prevRect.bottom <= eb.top + 8f) && (rect.bottom >= eb.top);

                if (stomp && e.isStompKillable()) {
                    e.kill();
                    // bounce up
                    vy = JUMP_VELOCITY * 0.70f;
                    y = eb.top; // stand on top momentarily
                    grounded = false;
                    syncRect();
                    continue;
                }

                // Otherwise: take damage (with iFrames) and treat enemy as solid blocker
                if (invulnTimer <= 0f) {
                    lives--;
                    invulnTimer = 1.4f;
                    if (onDamaged != null) onDamaged.run();
                    if (lives <= 0) {
                        if (onDeath != null) onDeath.run();
                        return;
                    }
                }

                // Solid response similar to spikes (hang on it)
                float prevBottomE = prevRect.bottom;
                float prevTopE = prevRect.top;
                float prevLeftE = prevRect.left;
                float prevRightE = prevRect.right;

                boolean fromAboveE = prevBottomE <= eb.top && rect.bottom >= eb.top;
                boolean fromBelowE = prevTopE >= eb.bottom && rect.top <= eb.bottom;
                boolean hitFromLeftE = prevRightE <= eb.left && rect.right >= eb.left;
                boolean hitFromRightE = prevLeftE >= eb.right && rect.left <= eb.right;

                if (fromAboveE) {
                    y = eb.top;
                    vy = 0f;
                    grounded = true;
                    syncRect();
                } else if (fromBelowE) {
                    y = eb.bottom + h;
                    vy = Math.abs(vy) * 0.25f;
                    syncRect();
                } else if (hitFromLeftE) {
                    x = eb.left - w - 0.01f;
                    syncRect();
                } else if (hitFromRightE) {
                    x = eb.right + 0.01f;
                    syncRect();
                } else {
                    // Fallback resolve: if we intersect but couldn't classify direction (e.g. big dt),
                    // push player out horizontally by the smallest overlap to prevent "clipping through".
                    float overlapL = rect.right - eb.left;
                    float overlapR = eb.right - rect.left;
                    if (overlapL < overlapR) {
                        x = eb.left - w - 0.01f;
                    } else {
                        x = eb.right + 0.01f;
                    }
                    syncRect();
                }

                // only handle one enemy per frame for stability
                break;
            }
        }

        if (grounded) coyoteTimer = COYOTE_TIME;
    }

    public void requestJump() {
        if (coyoteTimer > 0f) {
            vy = JUMP_VELOCITY;
            grounded = false;
            coyoteTimer = 0f;
            jumpHoldTimer = 0f;
            syncRect();
        }
    }

    /** Called on ACTION_DOWN: starts/holds jump for variable height. */
    public void onJumpPressed() {
        jumpHeld = true;
        requestJump();
    }

    /** Called on ACTION_UP/CANCEL: cuts jump short if still ascending. */
    public void onJumpReleased() {
        jumpHeld = false;
        if (vy < 0f) {
            vy *= JUMP_CUT_MULT;
        }
    }


    /** Grants invulnerability for at least the given duration (seconds). */
    public void grantInvulnerability(float seconds) {
        if (seconds <= 0f) return;
        if (invulnTimer < seconds) invulnTimer = seconds;
    }

    /**
     * Grants an "invincible" shield: ensures invulnerability AND enables the blue ring visual.
     * This is used by the INVINCIBLE powerup.
     */
    public void grantShield(float seconds) {
        if (seconds <= 0f) return;
        grantInvulnerability(seconds);
        if (shieldTimer < seconds) shieldTimer = seconds;
    }


    /**
     * Apply damage if not invulnerable; returns true if damage was taken.
     * Use onDamaged to update UI lives, and onDeath to end the run.
     */
    public boolean tryDamage(Runnable onDamaged, Runnable onDeath) {
        if (invulnTimer > 0f) return false;
        lives--;
        invulnTimer = 1.4f;
        if (onDamaged != null) onDamaged.run();
        if (lives <= 0) {
            if (onDeath != null) onDeath.run();
        }
        return true;
    }

    public void draw(Canvas canvas, Paint paint) {
        // Blue shield ring (powerup)
        if (shieldTimer > 0f) {
            float cx = rect.centerX();
            float cy = rect.centerY();
            float radius = Math.max(w, h) * 0.62f;
            canvas.drawCircle(cx, cy, radius, shieldPaint);
        }

        // Blink only the player sprite during i-frames (shield should still be visible)
        boolean drawPlayerNow = (invulnTimer <= 0f) || ((SystemClock.uptimeMillis() / 100) % 2 == 0);
        if (!drawPlayerNow) return;

        if (runMovie != null) {
            if (movieStartMs == 0) movieStartMs = SystemClock.uptimeMillis();
            int duration = runMovie.duration();
            if (duration <= 0) duration = 1000;
            int relTime = (int) ((SystemClock.uptimeMillis() - movieStartMs) % duration);
            runMovie.setTime(relTime);

            float mw = runMovie.width();
            float mh = runMovie.height();
            if (mw <= 0 || mh <= 0) {
                // fallback rect
                canvas.drawRect(rect, paint);
                return;
            }

            canvas.save();
            float scaleX = w / mw;
            float scaleY = h / mh;
            canvas.translate(rect.left, rect.top);
            canvas.scale(scaleX, scaleY);
            runMovie.draw(canvas, 0, 0);
            canvas.restore();
        } else {
            canvas.drawRect(rect, paint);
        }
    }

    private void syncRect() {
        rect.set(x, y - h, x + w, y);
    }
}
package de.hsos.prog3.inforun.game.entities.powerups;

import java.util.EnumMap;

/**
 * Tracks currently active (timed) power-ups.
 */
public class ActivePowerUps {

    private final EnumMap<PowerUpType, Float> remaining = new EnumMap<>(PowerUpType.class);

    public void clear() {
        remaining.clear();
    }

    /** Activates a power-up for durationSec (refreshes to max of existing/new). */
    public void activate(PowerUpType type, float durationSec) {
        Float old = remaining.get(type);
        if (old == null) remaining.put(type, durationSec);
        else remaining.put(type, Math.max(old, durationSec));
    }

    public void update(float dt) {
        if (remaining.isEmpty()) return;
        for (PowerUpType t : PowerUpType.values()) {
            Float r = remaining.get(t);
            if (r == null) continue;
            r -= dt;
            if (r <= 0f) remaining.remove(t);
            else remaining.put(t, r);
        }
    }

    public boolean isActive(PowerUpType type) {
        return remaining.containsKey(type);
    }

    public float getRemaining(PowerUpType type) {
        Float r = remaining.get(type);
        return r == null ? 0f : r;
    }
}

package de.hsos.prog3.inforun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.hsos.prog3.inforun.game.entities.powerups.ActivePowerUps;
import de.hsos.prog3.inforun.game.entities.powerups.PowerUpType;

public class ActivePowerUpsTest {

    @Test
    public void activationUsesTheLongerRemainingDuration() {
        ActivePowerUps active = new ActivePowerUps();

        active.activate(PowerUpType.MAGNET, 5f);
        active.activate(PowerUpType.MAGNET, 2f);

        assertTrue(active.isActive(PowerUpType.MAGNET));
        assertEquals(5f, active.getRemaining(PowerUpType.MAGNET), 0.001f);
    }

    @Test
    public void updateExpiresFinishedPowerUps() {
        ActivePowerUps active = new ActivePowerUps();
        active.activate(PowerUpType.DOUBLE_COINS, 1f);

        active.update(1.1f);

        assertFalse(active.isActive(PowerUpType.DOUBLE_COINS));
        assertEquals(0f, active.getRemaining(PowerUpType.DOUBLE_COINS), 0.001f);
    }

    @Test
    public void clearRemovesAllPowerUps() {
        ActivePowerUps active = new ActivePowerUps();
        active.activate(PowerUpType.INVINCIBLE, 3f);

        active.clear();

        assertFalse(active.isActive(PowerUpType.INVINCIBLE));
    }
}

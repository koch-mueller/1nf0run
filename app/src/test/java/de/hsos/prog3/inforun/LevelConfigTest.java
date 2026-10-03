package de.hsos.prog3.inforun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.hsos.prog3.inforun.game.GameConstants;
import de.hsos.prog3.inforun.game.LevelConfig;

public class LevelConfigTest {

    @Test
    public void configuredLevelsKeepTheirIds() {
        for (int level = 1; level <= GameConstants.MAX_LEVEL; level++) {
            assertEquals(level, LevelConfig.forLevel(level).levelId);
        }
    }

    @Test
    public void difficultyIncreasesAcrossTheCampaign() {
        LevelConfig first = LevelConfig.forLevel(1);
        LevelConfig last = LevelConfig.forLevel(GameConstants.MAX_LEVEL);

        assertTrue(last.moveSpeed > first.moveSpeed);
        assertTrue(last.enemySpawnIntervalSec < first.enemySpawnIntervalSec);
        assertTrue(last.robotChance > first.robotChance);
        assertTrue(last.obstacleSpawnSec < first.obstacleSpawnSec);
    }
}

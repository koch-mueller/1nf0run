package de.hsos.prog3.inforun.game;

public final class LevelConfig {
    public final int levelId;
    public final float moveSpeed;              // px/s
    public final float enemySpawnIntervalSec;  // Sekunden
    public final float robotChance;            // 0..1
    public final float slowZoneChance;         // pro Frame (dt-unabhängig, simpel)
    public final float obstacleSpawnSec;
    public final float collectibleSpawnSec;

    public LevelConfig(int levelId,
                       float moveSpeed,
                       float enemySpawnIntervalSec,
                       float robotChance,
                       float slowZoneChance,
                       float obstacleSpawnSec,
                       float collectibleSpawnSec) {
        this.levelId = levelId;
        this.moveSpeed = moveSpeed;
        this.enemySpawnIntervalSec = enemySpawnIntervalSec;
        this.robotChance = robotChance;
        this.slowZoneChance = slowZoneChance;
        this.obstacleSpawnSec = obstacleSpawnSec;
        this.collectibleSpawnSec = collectibleSpawnSec;
    }

    public static LevelConfig forLevel(int levelId) {
        // Level 1 is the easiest configuration; level 5 is the hardest.
        switch (levelId) {
            case 1: return new LevelConfig(levelId, 380f, 2.6f, 0.25f, 0.0025f, 2.0f, 2.6f);
            case 2: return new LevelConfig(levelId, 420f, 2.3f, 0.35f, 0.0030f, 1.8f, 2.5f);
            case 3: return new LevelConfig(levelId, 470f, 2.0f, 0.45f, 0.0035f, 1.6f, 2.4f);
            case 4: return new LevelConfig(levelId, 520f, 1.8f, 0.55f, 0.0040f, 1.4f, 2.3f);
            default: return new LevelConfig(levelId, 580f, 1.6f, 0.65f, 0.0045f, 1.2f, 2.2f);
        }
    }
}

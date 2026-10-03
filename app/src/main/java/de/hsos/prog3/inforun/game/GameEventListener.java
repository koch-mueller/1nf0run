package de.hsos.prog3.inforun.game;

public interface GameEventListener {
    void onCollectiblePicked();
    void onHitObstacle();

    /** Called when the player has no lives left. The Activity should end the game. */
    void onGameOver();
}

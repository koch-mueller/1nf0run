package de.hsos.prog3.inforun.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private boolean running = false;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        // Fixed timestep update + frame pacing.
        // Wichtig: Ohne "sleep" rendert der Thread so schnell wie möglich und kann Emulator/Device
        // massiv auslasten -> Lag + "Skipped frames" auf dem UI-Thread.
        long last = System.nanoTime();
        float accumulator = 0f;
        final float STEP = 1f / 60f; // 60Hz
        final long TARGET_FRAME_NS = 1_000_000_000L / 60L; // ~60 FPS render pacing

        while (running) {
            final long frameStart = System.nanoTime();
            long now = System.nanoTime();
            float frameTime = (now - last) / 1_000_000_000f;
            last = now;

            // clamp: verhindert spiral of death
            if (frameTime > 0.1f) frameTime = 0.1f;

            accumulator += frameTime;

            while (accumulator >= STEP) {
                gameView.update(STEP);
                accumulator -= STEP;
            }

            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        gameView.draw(canvas);
                    }
                }
            } finally {
                if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas);
            }

            // Frame pacing: Render auf ~60 FPS begrenzen, damit CPU nicht 100% läuft.
            long frameElapsed = System.nanoTime() - frameStart;
            long sleepNs = TARGET_FRAME_NS - frameElapsed;
            if (sleepNs > 0) {
                try {
                    // Thread.sleep arbeitet in ms + ns (ns wird i.d.R. gerundet, aber reicht hier)
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {
                    // If interrupted, just continue.
                }
            } else {
                // Wenn wir hinterherhängen, wenigstens kooperativ abgeben.
                Thread.yield();
            }
        }
    }
}

package de.hsos.prog3.inforun.ui;

import android.content.Context;

public final class UiUnits {

    private UiUnits() {
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

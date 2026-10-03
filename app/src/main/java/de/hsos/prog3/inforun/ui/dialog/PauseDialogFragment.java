package de.hsos.prog3.inforun.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.hsos.prog3.inforun.R;

public class PauseDialogFragment extends DialogFragment {

    public interface PauseActions {
        void onResumeGame();
        void onRestartGame();
        void onQuitGame();
    }

    private PauseActions actions;

    public PauseDialogFragment() {
        // Required empty constructor for fragment recreation.
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof PauseActions)) {
            throw new IllegalStateException("Host activity must implement PauseActions");
        }
        actions = (PauseActions) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.pause)
                .setMessage(R.string.pause_question)
                .setPositiveButton(R.string.continue_game, (d, w) -> actions.onResumeGame())
                .setNeutralButton(R.string.restart_game, (d, w) -> actions.onRestartGame())
                .setNegativeButton(R.string.quit, (d, w) -> actions.onQuitGame())
                .create();
    }

    @Override
    public void onCancel(@NonNull android.content.DialogInterface dialog) {
        super.onCancel(dialog);
        actions.onResumeGame();
    }

    @Override
    public void onDetach() {
        actions = null;
        super.onDetach();
    }
}

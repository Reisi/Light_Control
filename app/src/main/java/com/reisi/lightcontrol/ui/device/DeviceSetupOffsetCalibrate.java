package com.reisi.lightcontrol.ui.device;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.reisi.lightcontrol.R;

import org.jetbrains.annotations.NotNull;

public class DeviceSetupOffsetCalibrate extends DialogFragment {
    public interface DeviceCalibrateListener {
        void onDialogCalibrateClick(DialogFragment dialog);
        void onDialogCancelClick(DialogFragment dialog);
    }

    DeviceCalibrateListener listener;

    DeviceSetupOffsetCalibrate(DeviceCalibrateListener listener) {
        this.listener = listener;
    }

    @Override
    public @NotNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dev_setup_offset_message)
                .setPositiveButton(R.string.dev_setup_offset_calib, (dialog, which) -> listener.onDialogCalibrateClick(DeviceSetupOffsetCalibrate.this))
                .setNegativeButton(R.string.dev_setup_offset_cancel, (dialog, which) -> listener.onDialogCancelClick(DeviceSetupOffsetCalibrate.this));
        return builder.create();
    }
}

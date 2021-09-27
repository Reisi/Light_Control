package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

public interface NordicUartManagerCallbacks {
    void onDataReceived(@NonNull final BluetoothDevice device, final String data);
    void onDataSent(@NonNull final BluetoothDevice device, final String data);
}

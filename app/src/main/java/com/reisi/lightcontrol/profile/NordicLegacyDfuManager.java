package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;

public class NordicLegacyDfuManager extends BleManager {
    public static UUID getDfuServiceUuid() {
        return DFU_SERVICE_UUID;
    }

    final static UUID DFU_SERVICE_UUID = UUID.fromString("00001530-1212-EFDE-1523-785FEABCD123");

    public NordicLegacyDfuManager(final Context context) {
        super(context);
    }

    @NonNull
    @NotNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return null;
    }

    public class NordicLegacyDfuGattCallback extends BleManagerGattCallback {

        @Override
        protected boolean isRequiredServiceSupported(@NonNull @NotNull BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(DFU_SERVICE_UUID);
            if (service != null)
                return true;
            else
                return false;
        }

        @Override
        protected void onDeviceDisconnected() {

        }
    }
}

package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import no.nordicsemi.android.ble.WriteRequest;

public class NordicUartManager extends NordicLegacyDfuManager {
    private final static UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    private final NordicUartManagerCallbacks callbacks;
    private BluetoothGattCharacteristic rxCharacteristic, txCharacteristic;
    private boolean useLongWrite = true;

    public NordicUartManager(final Context context, NordicUartManagerCallbacks callbacks) {
        super(context);
        this.callbacks = callbacks;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new NordicUartManagerGattCallback();
    }

    public class NordicUartManagerGattCallback extends NordicLegacyDfuGattCallback {

        @Override
        protected void initialize() {
            super.initialize();
            setNotificationCallback(txCharacteristic).with((device, data) -> {
                final String text = data.getStringValue(0);
                callbacks.onDataReceived(device, text);
            });
            enableNotifications(txCharacteristic).enqueue();
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull @NotNull BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(UART_SERVICE_UUID);
            if (service != null) {
                rxCharacteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID);
                txCharacteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);
            }

            boolean writeRequest = false;
            boolean writeCommand = false;
            if (rxCharacteristic != null) {
                final int rxProperties = rxCharacteristic.getProperties();
                writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
                writeCommand = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;

                if (writeRequest)
                    rxCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                else
                    useLongWrite = false;
            }

            return rxCharacteristic != null && txCharacteristic != null && (writeRequest || writeCommand);
        }

        @Override
        protected void onDeviceDisconnected() {
            rxCharacteristic = null;
            txCharacteristic = null;
            useLongWrite = true;
        }
    }

    public boolean hasUart() {
        if (rxCharacteristic == null || txCharacteristic == null)
            return false;
        else
            return true;
    }

    public void uartSend(final String text) {
        if (rxCharacteristic == null)
            return;

        if (!TextUtils.isEmpty(text)) {
            final WriteRequest request = writeCharacteristic(rxCharacteristic, text.getBytes()).with((device, data) -> {
                callbacks.onDataSent(device, data.getStringValue(0));
            });
            if (!useLongWrite)
                request.split();
            request.enqueue();
        }
    }
}

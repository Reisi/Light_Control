package com.reisi.lightcontrol.profile;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import no.nordicsemi.android.ble.observer.BondingObserver;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

public class LightControlService extends Service implements LightControlManagerCallbacks, ConnectionObserver {
    public static final String BROADCAST_DEVICELIST_CHANGED = "com.reisi.lightcontrol.BROADCAST_DEVICELIST_CHANGED";
    public static final String EXTRA_DEVICE_ADDRESS = "com.reisi.lightcontrol.EXTRA_DEVICE_ADDRESS";
    public static final String EXTRA_DEVICE_NAME = "com.reisi.lightcontrol.EXTRA_DEVICE_NAME";
    public static final String EXTRA_CONNECTION_STATE = "com.reisi.lightcontrol.EXTRA_CONNECTION_STATE";

    public static final String BROADCAST_SETUP_HELMET_RECEIVED = "com.reisi.lightcontrol.BRADCAST_SETUP_HELMET_RECEIVED";
    public static final String BROADCAST_SETUP_BIKE_RECEIVED = "com.reisi.lightcontrol.BRADCAST_SETUP_BIKE_RECEIVED";
    public static final String EXTRA_SETUP_HELMET = "com.reisi.lightcontrol.EXTRA_SETUP_HELMET";
    public static final String EXTRA_SETUP_BIKE = "com.reisi.lightcontrol.EXTRA_SETUP_HELMET";
    public static final String EXTRA_STATUS_OVERCURRENT = "com.reisi.lightcontrol.EXTRA_STATUS_OVERCURRENT";
    public static final String EXTRA_STATUS_VOLTAGE = "com.reisi.lightcontrol.EXTRA_STATUS_VOLTAGE_LIMITING";
    public static final String EXTRA_STATUS_TEMPERATURE = "com.reisi.lightcontrol.EXTRA_STATUS_TEMPERATURE_LIMITING";
    public static final String EXTRA_STATUS_DUTYCYCLE = "com.reisi.lightcontrol.EXTRA_STATUS_DUTYCYCLE_LIMIT";

    public static final String BROADCAST_MEASUREMENT_RECEIVED = "com.reisi.lightcontrol.BROADCAST_MEASUREMENT_RECEIVED";
    public static final String EXTRA_POWER = "com.reisi.lightcontrol.EXTRA_POWER";
    public static final String EXTRA_TEMPERATURE = "com.reisi.lightcontrol.EXTRA_TEMPERATURE";
    public static final String EXTRA_INPUT_VOLTAGE = "com.reisi.lightcontrol.EXTRA_INPUT_VOLTAGE";
    public static final String EXTRA_PITCH_ANGLE = "com.reisi.lightcontrol.EXTRA_PITCH_ANGLE";
    public static final String EXTRA_BATTERY_SOC = "com.reisi.lightcontrol.EXTRA_BATTERY_SOC";
    public static final String EXTRA_TAILLIGHT_POWER = "com.reisi.lightcontrol.EXTRA_TAILLIGHT_POWER";

    public static final String BROADCAST_CONTROLPOINT_COMMON = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_COMMON";
    public static final String EXTRA_OPCODE = "com.reisi.lightcontrol.EXTRA_OPCODE";
    public static final String EXTRA_RESPONSE = "com.reisi.lightcontrol.EXTRA_RESPONSE";

    public static final String BROADCAST_CONTROLPOINT_MODE_CNT = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_MODE_CNT";
    public static final String EXTRA_MODE_CNT = "com.reisi.lightcontrol.EXTRA_MODE_CNT";

    public static final String BROADCAST_CONTROLPOINT_GROUPS = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_GROUPS";
    public static final String EXTRA_GROUPS = "com.reisi.lightcontrol.EXTRA_GROUPS";

    public static final String BROADCAST_CONTROLPOINT_MODE_CFG = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_MODE_CFG";
    public static final String EXTRA_MODE_HELMET_LIST = "com.reisi.lightcontrol.EXTRA_MODE_HELMET_LIST";
    public static final String EXTRA_MODE_BIKE_LIST = "com.reisi.lightcontrol.EXTRA_MODE_BIKE_LIST";

    public static final String BROADCAST_CONTROLPOINT_LED_CFG = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_LED_CFG";
    public static final String EXTRA_FLOOD_LED_CNT = "com.reisi.lightcontrol.EXTRA_FLOOD_LED_CNT";
    public static final String EXTRA_SPOT_LED_CNT = "com.reisi.lightcontrol.EXTRA_SPOT_LED_CNT";

    public static final String BROADCAST_CONTROLPOINT_SENSOR_OFFSET = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_SENSOR_OFFSET";
    public static final String EXTRA_SENSOR_OFFSET_X = "com.reisi.lightcontrol.EXTRA_SENSOR_OFFSET_X";
    public static final String EXTRA_SENSOR_OFFSET_Y = "com.reisi.lightcontrol.EXTRA_SENSOR_OFFSET_Y";
    public static final String EXTRA_SENSOR_OFFSET_Z = "com.reisi.lightcontrol.EXTRA_SENSOR_OFFSET_Z";

    public static final String BROADCAST_CONTROLPOINT_CURRENT_LIMIT = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_CURRENT_LIMIT";
    public static final String EXTRA_FLOOD_LIMIT = "com.reisi.lightcontrol.EXTRA_FLOOD_LIMIT";
    public static final String EXTRA_SPOT_LIMIT = "com.reisi.lightcontrol.EXTRA_SPOT_LIMIT";

    public static final String BROADCAST_CONTROLPOINT_PREFERRED_MODE = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_PREFFERED_MODE";
    public static final String EXTRA_PREFERRED_MODE = "com.reisi.lightcontrol.EXTRA_PREFFERED_MODE";

    public static final String BROADCAST_CONTROLPOINT_TEMPORARY_MODE = "com.reisi.lightcontrol.BROADCAST_CONTROLPOINT_TEMPORARY_MODE";
    public static final String EXTRA_TEMPORARY_MODE = "com.reisi.lightcontrol.EXTRA_PREFFERED_MODE";

    public static final String BROADCAST_UART = "com.reisi.lightcontrol.BROADCAST_UART_RECEIVED";
    public static final String EXTRA_UART_RX = "com.reisi.lightcontrol.EXTRA_UART_RX";
    public static final String EXTRA_UART_TX = "com.reisi.lightcontrol.EXTRA_UART_TX";

    private HashMap<BluetoothDevice, LightControlManager> lightControlManagers;
    private List<BluetoothDevice> supportedDevices;
    private List<BluetoothDevice> unsupportedDevices;

    public class LCSBinder extends Binder {
        public void addDevice(BluetoothDevice device) {
            if (unsupportedDevices.contains(device))
                return;

            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null) {
                manager = new LightControlManager(LightControlService.this, LightControlService.this);
                lightControlManagers.put(device, manager);
                manager.setConnectionObserver(LightControlService.this);
            }
            if (manager.getConnectionState() != BluetoothProfile.STATE_CONNECTING)
                manager.connect(device).enqueue();
        }

        public void removeDevice(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager != null) {
                if (manager.isConnected()) {
                    manager.disconnect().enqueue();
                }
                lightControlManagers.remove(device);
            }
            supportedDevices.remove(device);

            final Intent broadcast = new Intent(BROADCAST_DEVICELIST_CHANGED);
            broadcast.putExtra(EXTRA_DEVICE_NAME, device.getName());
            broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
            broadcast.putExtra(EXTRA_CONNECTION_STATE,false);
            LocalBroadcastManager.getInstance(LightControlService.this).sendBroadcast(broadcast);
        }

        public List<BluetoothDevice> getDevices() {
            return supportedDevices;
        }

        public int getState(@NotNull BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return LightControlManager.STATE_UNKNOWN;
            else
                return manager.getState();
        }

        public String getFirmwareRev(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return null;
            return manager.getFirmwareRev();
        }

        public String getHardwareRev(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return null;
            return manager.getHardwareRev();
        }

        public LightControlFeatureDataCallback getFeatures(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return null;
            return manager.getFeatures();
        }

        public void setMode(BluetoothDevice device, byte modeToSet) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.setMode(modeToSet);
        }

        public void requestModeCnt(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.requestModeCount();
        }

        public void requestGroups(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.requestGroups();
        }

        public void setGroups(BluetoothDevice device, byte newGroups) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.setGroups(newGroups);
        }

        public void requestPreferredMode(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.requestPreferredMode();
        }

        public void setPreferredMode(BluetoothDevice device, byte newPreferredMode) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.setPreferredMode(newPreferredMode);
        }

        public void requestTemporaryMode(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.requestTemporaryMode();
        }

        public void setTemporaryMode(BluetoothDevice device, byte newTemporaryMode) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.setTemporaryMode(newTemporaryMode);
        }

        public void requestModes(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.requestModeList();
        }

        public void setModes(BluetoothDevice device, List modeList) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.setModeList(modeList);
        }

        public void requestOffset(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.requestSensorOffset();
        }

        public void calibrateOffset(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.startSensorOffsetCalibration();
        }

        public void requestCurrentLimit(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.requestCurrentLimit();
        }

        public void setCurrentLimit(BluetoothDevice device, byte floodLimit, byte spotLimit) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.setCurrentLimit(floodLimit, spotLimit);
        }

        public void requestLedConfiguration(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.requestLedConfiguration();
        }

        public void checkLedConfiguration(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.startLedConfigurationCheck();
        }

        public boolean hasUart(BluetoothDevice device) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return false;
            return manager.hasUart();
        }

        public void sendUartCommand(BluetoothDevice device, String command) {
            LightControlManager manager = lightControlManagers.get(device);
            if (manager == null)
                return;
            manager.uartSend(command);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null)
                    return;
                mBinder.addDevice(device);
            }
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            }
        }
    };

    @Override
    public void onCreate() {
        lightControlManagers = new HashMap<>();
        supportedDevices = new ArrayList<>();
        unsupportedDevices = new ArrayList<>();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        for (LightControlManager manager : lightControlManagers.values()) {
            manager.disconnect().enqueue();
        }
        lightControlManagers.clear();
        unsupportedDevices.clear();
        supportedDevices.clear();
        unsupportedDevices = null;
        supportedDevices = null;

        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDeviceConnecting(@NonNull @NotNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceConnected(@NonNull @NotNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceFailedToConnect(@NonNull @NotNull BluetoothDevice device, int reason) {

    }

    @Override
    public void onDeviceReady(@NonNull @NotNull BluetoothDevice device) {
        if (!supportedDevices.contains(device))
            supportedDevices.add(device);

        final Intent broadcast = new Intent(BROADCAST_DEVICELIST_CHANGED);
        broadcast.putExtra(EXTRA_DEVICE_NAME, device.getName());
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_CONNECTION_STATE,true);
        LocalBroadcastManager.getInstance(LightControlService.this).sendBroadcast(broadcast);
    }

    @Override
    public void onDeviceDisconnecting(@NonNull @NotNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(@NonNull @NotNull BluetoothDevice device, int reason) {
        if (reason == REASON_NOT_SUPPORTED && !unsupportedDevices.contains(device))
            unsupportedDevices.add(device);

        if (!supportedDevices.contains(device))
            return;

        final Intent broadcast = new Intent(BROADCAST_DEVICELIST_CHANGED);
        broadcast.putExtra(EXTRA_DEVICE_NAME, device.getName());
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_CONNECTION_STATE,false);
        LocalBroadcastManager.getInstance(LightControlService.this).sendBroadcast(broadcast);
    }

    private final LCSBinder mBinder = new LCSBinder();

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY; //super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDataReceived(@NonNull @NotNull BluetoothDevice device, String data) {
        final Intent broadcast = new Intent(BROADCAST_UART);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_UART_RX, data);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onDataSent(@NonNull @NotNull BluetoothDevice device, String data) {
        final Intent broadcast = new Intent(BROADCAST_UART);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_UART_TX, data);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onMeasurementSetupReceived(@NonNull BluetoothDevice device,
                                           @NonNull LightControlHelmetSetup setup, boolean overCurrent,
                                           boolean voltageLimiting, boolean temperatureLimiting,
                                           boolean dutyCycleLimit) {
        final Intent broadcast = new Intent(BROADCAST_SETUP_HELMET_RECEIVED);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_SETUP_HELMET, setup);
        broadcast.putExtra(EXTRA_STATUS_OVERCURRENT, overCurrent);
        broadcast.putExtra(EXTRA_STATUS_VOLTAGE, voltageLimiting);
        broadcast.putExtra(EXTRA_STATUS_TEMPERATURE, temperatureLimiting);
        broadcast.putExtra(EXTRA_STATUS_DUTYCYCLE, dutyCycleLimit);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onMeasurementSetupReceived(@NonNull BluetoothDevice device,
                                           @NonNull LightControlBikeSetup setup, boolean overCurrent,
                                           boolean voltageLimiting, boolean temperatureLimiting,
                                           boolean dutyCycleLimit) {
        final Intent broadcast = new Intent(BROADCAST_SETUP_BIKE_RECEIVED);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_SETUP_BIKE, setup);
        broadcast.putExtra(EXTRA_STATUS_OVERCURRENT, overCurrent);
        broadcast.putExtra(EXTRA_STATUS_VOLTAGE, voltageLimiting);
        broadcast.putExtra(EXTRA_STATUS_TEMPERATURE, temperatureLimiting);
        broadcast.putExtra(EXTRA_STATUS_DUTYCYCLE, dutyCycleLimit);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onMeasurementDataReceived(@NonNull BluetoothDevice device, Float power,
                                          Float temperature, Float voltage, Float pitch,
                                          Float batterySOC, Float taillightPower) {
        final Intent broadcast = new Intent(BROADCAST_MEASUREMENT_RECEIVED);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        if (power != null) {
            broadcast.putExtra(EXTRA_POWER, power);
        }
        if (temperature != null) {
            broadcast.putExtra(EXTRA_TEMPERATURE, temperature);
        }
        if (voltage != null) {
            broadcast.putExtra(EXTRA_INPUT_VOLTAGE, voltage);
        }
        if (pitch != null) {
            broadcast.putExtra(EXTRA_PITCH_ANGLE, pitch);
        }
        if (batterySOC != null) {
            broadcast.putExtra(EXTRA_BATTERY_SOC, batterySOC);
        }
        if (taillightPower != null) {
            broadcast.putExtra(EXTRA_TAILLIGHT_POWER, taillightPower);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptCommonResponse(@NonNull BluetoothDevice device, int responseValue, int opCode) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_COMMON);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_RESPONSE, responseValue);
        broadcast.putExtra(EXTRA_OPCODE, opCode);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptModeCntResponse(@NonNull BluetoothDevice device, int modeCnt) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_MODE_CNT);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_MODE_CNT, modeCnt);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptGroupCfgResponse(@NonNull BluetoothDevice device, int groups) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_GROUPS);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_GROUPS, groups);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptModeCfgResponse(@NonNull BluetoothDevice device, List<LightControlHelmetSetup> helmetList, List<LightControlBikeSetup> bikeList) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_MODE_CFG);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        if (helmetList != null && !helmetList.isEmpty())
            broadcast.putParcelableArrayListExtra(EXTRA_MODE_HELMET_LIST, (ArrayList<LightControlHelmetSetup>)helmetList);
        else if (bikeList != null && !bikeList.isEmpty())
            broadcast.putParcelableArrayListExtra(EXTRA_MODE_BIKE_LIST, (ArrayList<LightControlBikeSetup>)bikeList);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptLedResponse(@NonNull BluetoothDevice device, int floodCnt, int spotCnt) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_LED_CFG);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_FLOOD_LED_CNT, floodCnt == 0xFF ? -1 : floodCnt);
        broadcast.putExtra(EXTRA_SPOT_LED_CNT, spotCnt == 0xFF ? -1 : spotCnt);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptSensorOffsetResponse(@NonNull BluetoothDevice device, int[] offset) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_SENSOR_OFFSET);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_SENSOR_OFFSET_X, offset[0]);
        broadcast.putExtra(EXTRA_SENSOR_OFFSET_Y, offset[1]);
        broadcast.putExtra(EXTRA_SENSOR_OFFSET_Z, offset[2]);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptRqstCurrentLimitResponse(@NonNull BluetoothDevice device, int floodLimit, int spotLimit) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_CURRENT_LIMIT);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_FLOOD_LIMIT, floodLimit);
        broadcast.putExtra(EXTRA_SPOT_LIMIT, spotLimit);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptPrefModeResponse(@NonNull BluetoothDevice device, int prefMode) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_PREFERRED_MODE);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_PREFERRED_MODE, prefMode);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void onCtrlptTempModeResponse(@NonNull BluetoothDevice device, int tempMode) {
        final Intent broadcast = new Intent(BROADCAST_CONTROLPOINT_TEMPORARY_MODE);
        broadcast.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        broadcast.putExtra(EXTRA_TEMPORARY_MODE, tempMode);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }
}

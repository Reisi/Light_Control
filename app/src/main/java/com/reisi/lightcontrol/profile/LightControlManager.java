package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.callback.SuccessCallback;
import no.nordicsemi.android.ble.observer.BondingObserver;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

public class LightControlManager extends NordicUartManager {
    //public class LightControlManager<T extends LightControlManagerCallbacks> extends BleManager {
    public static final UUID DEVICE_INFORMATION_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    public static final UUID HARDWARE_REVISION_STRING_UUID = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB");
    public static final UUID FIRMWARE_REVISION_STRING_UUID = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");

    private static final UUID LIGHT_CONTROL_SERVICE_UUID = UUID.fromString("4f770101-ed7d-11e4-840e-0002a5d5c51b");
    public static final UUID LIGHT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("4f770102-ed7d-11e4-840e-0002a5d5c51b");
    public static final UUID LIGHT_FEATURE_CHARACTERISTIC_UUID = UUID.fromString("4f770103-ed7d-11e4-840e-0002a5d5c51b");
    public static final UUID LIGHT_CONTROL_POINT_CHARACTERISTIC_UUID = UUID.fromString("4f770104-ed7d-11e4-840e-0002a5d5c51b");

    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_DISCONNECTED = 1;
    public static final int STATE_READ_ONLY = 2;
    public static final int STATE_READ_WRITE = 3;

    private boolean isSupported;
    private BluetoothGattCharacteristic measChar, featureChar, controlPointChar, hardwareChar, firmwareChar;

    private boolean modesRequested = false; // indicating if the modelist has already been requested and waiting to receive
    private int numOfModes = 0;             // the number of modes supported by device

    private final List<LightControlHelmetSetup> bufferedHelmetList = new ArrayList<>();
    private final List<LightControlBikeSetup> bufferedBikeList = new ArrayList<>();

    private int state = STATE_UNKNOWN;

    private boolean indEnableTried = false;
    //private boolean bondingTried = false;   // if the light control indication requires an encrypted link this variable indicates
                                            // if there has already been a try to bond. If it is true and the device is not bonded
                                            // it is probably out of the bonding window and changing any settings is not possible

    private final LightControlManagerCallbacks callbacks;

    public LightControlFeatureDataCallback onFeaturesRead = new LightControlFeatureDataCallback() {
        @Override
        public void onLightControlFeatureReceived(@NonNull BluetoothDevice device, int lightType) {}

        @Override
        public void onLightControlFeatureReceived(@NonNull BluetoothDevice device, int lightType, @NonNull LCConfigurationFeatures configurationFeatures, @NonNull LCSetupFeatures setupFeatures, @NonNull LCHelmetLightFeatures helmetLightFeatures) { }

        @Override
        public void onLightControlFeatureReceived(@NonNull BluetoothDevice device, int lightType, @NonNull LCConfigurationFeatures configurationFeatures, @NonNull LCSetupFeatures setupFeatures, @NonNull LCBikeLightFeatures bikeLightFeatures) { }
    };

    public LightControlManager(@NonNull final Context context, LightControlManagerCallbacks callbacks) {
        super(context, callbacks);
        this.callbacks = callbacks;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        //return gattCallback;
        return new LightControlManagerGattCallback();
    }

    @Override
    protected boolean shouldClearCacheWhenDisconnected () {
        return !isSupported;
    }

    public static UUID getLightControlServiceUuid() {
        return LIGHT_CONTROL_SERVICE_UUID;
    }

    private class LightControlManagerGattCallback extends NordicUartManagerGattCallback implements BondingObserver {
    //private final BleManagerGattCallback gattCallback = new BleManagerGattCallback() {
        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            //boolean isUartSupported = super.isRequiredServiceSupported(gatt);
            BluetoothGattService service = gatt.getService(DEVICE_INFORMATION_SERVICE_UUID);
            if (service != null) {
                hardwareChar = service.getCharacteristic(HARDWARE_REVISION_STRING_UUID);
                firmwareChar = service.getCharacteristic(FIRMWARE_REVISION_STRING_UUID);
            }

            service = gatt.getService(LIGHT_CONTROL_SERVICE_UUID);
            if (service != null) {
                measChar = service.getCharacteristic(LIGHT_MEASUREMENT_CHARACTERISTIC_UUID);
                featureChar = service.getCharacteristic(LIGHT_FEATURE_CHARACTERISTIC_UUID);
                controlPointChar = service.getCharacteristic(LIGHT_CONTROL_POINT_CHARACTERISTIC_UUID);
            }
            boolean notify = false;
            if (measChar != null) {
                final int properties = measChar.getProperties();
                notify = (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
            }
            boolean read = false;
            if (featureChar != null) {
                final int properties = featureChar.getProperties();
                read = (properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
            }
            boolean write = false, indicate = false;
            if (controlPointChar != null) {
                final int properties = controlPointChar.getProperties();
                indicate = (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
                write = (properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
                controlPointChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            }
            isSupported =  measChar != null && featureChar != null && controlPointChar != null
                    && notify && read && write && indicate;

            return isSupported;
        }

        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            return super.isRequiredServiceSupported(gatt);  // uart service is optional
            //return super.isOptionalServiceSupported(gatt);
        }

        private final LightControlMeasurementDataCallback onMeasCharNotify = new LightControlMeasurementDataCallback() {
            @Override
            public void onMeasurementSetupReceived(@NonNull BluetoothDevice device, @NonNull LightControlHelmetSetup setup, boolean overCurrent, boolean voltageLimiting, boolean temperatureLimiting, boolean dutyCycleLimit) {
                callbacks.onMeasurementSetupReceived(device, setup, overCurrent, voltageLimiting, temperatureLimiting, dutyCycleLimit);
            }

            @Override
            public void onMeasurementSetupReceived(@NonNull BluetoothDevice device, @NonNull LightControlBikeSetup setup, boolean overCurrent, boolean voltageLimiting, boolean temperatureLimiting, boolean dutyCycleLimit) {
                callbacks.onMeasurementSetupReceived(device, setup, overCurrent, voltageLimiting, temperatureLimiting, dutyCycleLimit);
            }

            @Override
            public void onMeasurementDataReceived(@NonNull BluetoothDevice device, Float power, Float temperature, Float voltage, Float pitch, Float soc, Float tailPower) {
                callbacks.onMeasurementDataReceived(device, power, temperature, voltage, pitch, soc, tailPower);
            }
        };

        public final LightControlControlPointDataCallback onCtrlptIndication = new LightControlControlPointDataCallback() {
            @Override
            public void onLCCPCommonResponse(@NonNull BluetoothDevice device, int responseValue, int opCode) {
                callbacks.onCtrlptCommonResponse(device, responseValue, opCode);
            }

            @Override
            public void onLCCPModeCntResponse(@NonNull BluetoothDevice device, int modeCnt) {
                numOfModes = modeCnt;
                callbacks.onCtrlptModeCntResponse(device, modeCnt);
            }

            @Override
            public void onLCCPGroupCfgResponse(@NonNull BluetoothDevice device, int groups) {
                callbacks.onCtrlptGroupCfgResponse(device, groups);
            }

            @Override
            public void onLCCPModeCfgResponse(@NonNull BluetoothDevice device, byte[] modeList) {
                if (onFeaturesRead.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT) {
                    int numOfModesRx = modeList.length / 2;
                    for (int i = 0; i < numOfModesRx; i++) {
                        LightControlHelmetSetup setup = new LightControlHelmetSetup();
                        int rawSetup = modeList[i * 2];
                        setup.floodActive = (rawSetup & 0x01) != 0;
                        setup.spotActive = (rawSetup & 0x02) != 0;
                        setup.pitchComensation = (rawSetup & 0x04) != 0;
                        setup.cloned = (rawSetup & 0x08) != 0;
                        setup.taillight = (rawSetup & 0x10) != 0;
                        setup.brakeLight = (rawSetup & 0x20) != 0;
                        setup.intensity = modeList[i * 2 + 1];
                        bufferedHelmetList.add(setup);
                    }
                    if (bufferedHelmetList.size() == numOfModes) {
                        callbacks.onCtrlptModeCfgResponse(device, bufferedHelmetList, null);
                        modesRequested = false;
                    }
                    else {
                        byte[] cfg = {LightControlManagerCallbacks.opCode.rqstModeCfg, (byte)bufferedHelmetList.size()};
                        writeCharacteristic(controlPointChar, cfg).enqueue();
                    }
                }
                else if (onFeaturesRead.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT) {
                    int numOfModesRx = modeList.length / 3;
                    for (int i = 0; i < numOfModesRx; i++) {
                        LightControlBikeSetup setup = new LightControlBikeSetup();
                        int rawSetup = modeList[i * 3];
                        setup.mainBeamActive = (rawSetup & 0x01) != 0;
                        setup.extendedMainBeamActive = (rawSetup & 0x02) != 0;
                        setup.highBeamActive = (rawSetup & 0x04) != 0;
                        setup.daylightActive = (rawSetup & 0x08) != 0;
                        setup.taillight = (rawSetup & 0x10) != 0;
                        setup.brakeLight = (rawSetup & 0x20) != 0;
                        setup.mainBeamIntensity = modeList[i * 3 + 1];
                        setup.highBeamIntensity = modeList[i * 3 + 2];
                        bufferedBikeList.add(setup);
                    }
                    if (bufferedBikeList.size() == numOfModes) {
                        callbacks.onCtrlptModeCfgResponse(device, null, bufferedBikeList);
                        modesRequested = false;
                    }
                    else {
                        byte[] cfg = {LightControlManagerCallbacks.opCode.rqstModeCfg, (byte)bufferedBikeList.size()};
                        writeCharacteristic(controlPointChar, cfg).enqueue();
                    }

                }
            }

            @Override
            public void onLCCPLedResponse(@NonNull BluetoothDevice device, int floodCnt, int spotCnt) {
                callbacks.onCtrlptLedResponse(device, floodCnt, spotCnt);
            }

            @Override
            public void onLCCPSensorOffsetResponse(@NonNull BluetoothDevice device, int[] offset) {
                callbacks.onCtrlptSensorOffsetResponse(device, offset);
            }

            @Override
            public void onLCCPRqstCurrentLimitResonse(@NonNull BluetoothDevice device, int floodLimit, int spotLimit) {
                callbacks.onCtrlptRqstCurrentLimitResponse(device, floodLimit, spotLimit);
            }

            @Override
            public void onLCCPPrefModeResponse(@NonNull BluetoothDevice device, int prefMode) {
                callbacks.onCtrlptPrefModeResponse(device, prefMode);
            }

            @Override
            public void onLCCPTempModeResponse(@NonNull BluetoothDevice device, int prefMode) {
                callbacks.onCtrlptTempModeResponse(device, prefMode);
            }
        };

        @Override
        protected void initialize() {
            super.initialize();

            setBondingObserver(this);

            readCharacteristic(featureChar).with(onFeaturesRead).enqueue();

            setNotificationCallback(measChar).with(onMeasCharNotify);
            enableNotifications(measChar).enqueue();

            state = STATE_READ_ONLY;

            if (indEnableTried == false) {
                setIndicationCallback(controlPointChar).with(onCtrlptIndication);
                enableIndications(controlPointChar).done(device -> state = STATE_READ_WRITE).enqueue();
                indEnableTried = true;
            }
            /*if (controlPointChar.getPermissions() != BluetoothGattCharacteristic.PERMISSION_WRITE ||
                    !bondingTried || getBluetoothDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
                setIndicationCallback(controlPointChar).with(onCtrlptIndication);
                enableIndications(controlPointChar).fail().enqueue();

                // if characteristic is not open and device not bonded yet, this will trigger the bonding procedure
                if (controlPointChar.getPermissions() != BluetoothGattCharacteristic.PERMISSION_WRITE &&
                        getBluetoothDevice().getBondState() != BluetoothDevice.BOND_BONDED)
                    bondingTried = true;

                if (controlPointChar.getPermissions() != BluetoothGattCharacteristic.PERMISSION_WRITE ||
                        getBluetoothDevice().getBondState() == BluetoothDevice.BOND_BONDED)
                    state = STATE_READ_WRITE;
            }*/

            if (hardwareChar != null) {
                readCharacteristic(hardwareChar).enqueue();
            }
            if (firmwareChar != null) {
                readCharacteristic(firmwareChar).enqueue();
            }
        }

        @Override
        protected void onDeviceDisconnected() {
            super.onDeviceDisconnected();
            measChar = null;
            featureChar = null;
            controlPointChar = null;
            if (state == STATE_READ_WRITE)
                indEnableTried = false;
            state = STATE_DISCONNECTED;
        }

        @Override
        public void onBondingRequired(@NonNull @NotNull BluetoothDevice device) {
            createBondInsecure().enqueue();
        }

        @Override
        public void onBonded(@NonNull @NotNull BluetoothDevice device) {
            //state = STATE_READ_WRITE;
        }

        @Override
        public void onBondingFailed(@NonNull @NotNull BluetoothDevice device) { }
    }

    public String getHardwareRev() {
        if (state == STATE_UNKNOWN)
            return null;
        return hardwareChar.getStringValue(0);
    }

    public String getFirmwareRev() {
        if (state == STATE_UNKNOWN)
            return null;
        return firmwareChar.getStringValue(0);
    }

    public LightControlFeatureDataCallback getFeatures() {
        if (state == STATE_UNKNOWN)
            return null;
        return onFeaturesRead;
    }

    public int getState() { return state; }

    public void requestModeCount() {
        if (state != STATE_READ_WRITE)
            return;

        if (numOfModes == 0) {
            byte[] msg = {LightControlManagerCallbacks.opCode.rqstModeCnt};
            writeCharacteristic(controlPointChar, msg).enqueue();
        }
        else {
            BluetoothDevice device = getBluetoothDevice();
            if (device != null)
                callbacks.onCtrlptModeCntResponse(device, numOfModes);
        }
    }

    public void setMode(byte mode) {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.setMode, mode};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void requestGroups() {
        if (state != STATE_READ_WRITE)
            return;

        byte[] rqst = {LightControlManagerCallbacks.opCode.rqstGroupCfg};
        writeCharacteristic(controlPointChar, rqst).enqueue();
    }

    public void setGroups(byte groups) {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.setGroupCfg, groups};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void requestModeList() {
        if (state != STATE_READ_WRITE)
            return;

        // mode list might be too big to fit in one message, so it has to be sequenced.
        // first clear old values
        if (modesRequested)
            return; // procedure already in progress
        //numOfModes = 0;
        bufferedHelmetList.clear();
        bufferedBikeList.clear();
        modesRequested = true;
        // first read the number if available modes if not done yet
        if (numOfModes == 0) {
            byte[] cnt = {LightControlManagerCallbacks.opCode.rqstModeCnt};
            writeCharacteristic(controlPointChar, cnt).enqueue();
        }
        // than start to read the available modes beginning with the first
        byte[] cfg = {LightControlManagerCallbacks.opCode.rqstModeCfg, 0};
        writeCharacteristic(controlPointChar, cfg).enqueue();
        // the rest of the list is requested on reception
    }

    public void setModeList(List modeList) {
        if (state != STATE_READ_WRITE)
            return;

        final int maxMsgLength = 19;
        int modesEncoded = 0;

        byte[] msg = new byte[2];
        msg[0] = LightControlManagerCallbacks.opCode.setModeCfg;
        msg[1] = (byte)modesEncoded;

        if (onFeaturesRead.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT) {
            for (LightControlHelmetSetup setup : (List<LightControlHelmetSetup>)modeList) {
                if (msg.length + 2 > maxMsgLength) {
                    writeCharacteristic(controlPointChar, msg).enqueue();
                    byte[] nextMsg = new byte[2];
                    nextMsg[0] = LightControlManagerCallbacks.opCode.setModeCfg;
                    nextMsg[1] = (byte)modesEncoded;
                    msg = nextMsg;
                }

                int mode = (setup.floodActive ? 0x01 : 0 );
                mode +=    (setup.spotActive ? 0x02 : 0 );
                mode +=    (setup.pitchComensation ? 0x04 : 0 );
                mode +=    (setup.cloned ? 0x08 : 0 );
                mode +=    (setup.taillight ? 0x10 : 0 );
                mode +=    (setup.brakeLight ? 0x20 : 0 );

                byte[] ext = new byte[msg.length + 2];
                System.arraycopy(msg, 0, ext, 0, msg.length);
                ext[msg.length] = (byte)mode;
                ext[msg.length + 1] = (byte)setup.intensity;
                modesEncoded++;

                msg = ext;
            }
            if (msg.length > 2)
                writeCharacteristic(controlPointChar, msg).enqueue();
        } else if (onFeaturesRead.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT) {
            for (LightControlBikeSetup setup : (List<LightControlBikeSetup>)modeList) {
                if (msg.length + 3 > maxMsgLength) {
                    writeCharacteristic(controlPointChar, msg).enqueue();
                    byte[] nextMsg = new byte[2];
                    nextMsg[0] = LightControlManagerCallbacks.opCode.setModeCfg;
                    nextMsg[1] = (byte)modesEncoded;
                    msg = nextMsg;
                }

                int mode = (setup.mainBeamActive ? 0x01 : 0 );
                mode +=    (setup.extendedMainBeamActive ? 0x02 : 0 );
                mode +=    (setup.highBeamActive ? 0x04 : 0 );
                mode +=    (setup.daylightActive ? 0x08 : 0 );
                mode +=    (setup.taillight ? 0x10 : 0 );
                mode +=    (setup.brakeLight ? 0x20 : 0 );

                byte[] ext = new byte[msg.length + 3];
                System.arraycopy(msg, 0, ext, 0, msg.length);
                ext[msg.length] = (byte)mode;
                ext[msg.length + 1] = (byte)setup.mainBeamIntensity;
                ext[msg.length + 2] = (byte)setup.highBeamIntensity;
                modesEncoded++;

                msg = ext;
            }
            if (msg.length > 2)
                writeCharacteristic(controlPointChar, msg).enqueue();
        }
    }

    public void requestLedConfiguration() {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.rqstLedCfg};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void startLedConfigurationCheck() {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.chkLedCfg};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void requestSensorOffset() {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.rqstSensorOffset};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void startSensorOffsetCalibration() {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.calSensorOffset};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void requestCurrentLimit() {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.rqstCurrLimit};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void setCurrentLimit(byte floodLimit, byte spotLimit) {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.setCurrLimit, floodLimit, spotLimit};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void requestPreferredMode() {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.rqstPrefMode};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void setPreferredMode(byte mode) {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.setPrefMode, mode};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void requestTemporaryMode() {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.rqstTempMode};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }

    public void setTemporaryMode(byte mode) {
        if (state != STATE_READ_WRITE)
            return;

        byte[] msg = {LightControlManagerCallbacks.opCode.setTempMode, mode};
        writeCharacteristic(controlPointChar, msg).enqueue();
    }
}

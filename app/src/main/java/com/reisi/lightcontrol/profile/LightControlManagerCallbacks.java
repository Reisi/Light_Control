package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import no.nordicsemi.android.ble.BleManagerCallbacks;

//public interface LightControlManagerCallbacks extends BleManagerCallbacks {
public interface LightControlManagerCallbacks extends NordicUartManagerCallbacks {
    void onMeasurementSetupReceived(@NonNull final BluetoothDevice device,
                                    @NonNull final LightControlHelmetSetup setup,
                                    boolean overcurrent,
                                    boolean voltageLimiting,
                                    boolean temperatureLimiting,
                                    boolean dutyCycleLimit);
    void onMeasurementSetupReceived(@NonNull final BluetoothDevice device,
                                    @NonNull final LightControlBikeSetup setup,
                                    boolean overcurrent,
                                    boolean voltageLimiting,
                                    boolean temperatureLimiting,
                                    boolean dutyCycleLimit);
    void onMeasurementDataReceived(@NonNull final BluetoothDevice device,
                                   final Float power,
                                   final Float temperature,
                                   final Float voltage,
                                   final Float pitch,
                                   final Float soc,
                                   final Float tailPower);

    @IntDef({opCode.rqstModeCnt, opCode.setMode, opCode.rqstGroupCfg, opCode.setGroupCfg, opCode.rqstModeCfg,
             opCode.setModeCfg, opCode.rqstLedCfg, opCode.chkLedCfg, opCode.rqstSensorOffset,
             opCode.calSensorOffset, opCode.rqstCurrLimit, opCode.setCurrLimit, opCode.rqstPrefMode,
             opCode.setPrefMode,  opCode.rqstTempMode, opCode.setTempMode, opCode.responseCode})
    @Retention(RetentionPolicy.SOURCE)
    @interface opCode {
        int rqstModeCnt = 1;
        int setMode = 2;
        int rqstGroupCfg = 3;
        int setGroupCfg = 4;
        int rqstModeCfg = 5;
        int setModeCfg = 6;
        int rqstLedCfg = 7;
        int chkLedCfg = 8;
        int rqstSensorOffset = 9;
        int calSensorOffset = 10;
        int rqstCurrLimit = 11;
        int setCurrLimit = 12;
        int rqstPrefMode = 13;
        int setPrefMode = 14;
        int rqstTempMode = 15;
        int setTempMode = 16;
        int responseCode = 32;
    }

    @IntDef({responseValue.success, responseValue.notSupported, responseValue.invalidParam,
             responseValue.failed})
    @Retention(RetentionPolicy.SOURCE)
    @interface responseValue {
        int success = 1;
        int notSupported = 2;
        int invalidParam = 3;
        int failed = 4;
    }

    void onCtrlptCommonResponse(@NonNull final BluetoothDevice device, @responseValue int responseValue, @opCode int opCode);
    void onCtrlptModeCntResponse(@NonNull final BluetoothDevice device, int modeCnt);
    void onCtrlptGroupCfgResponse(@NonNull final BluetoothDevice device, int groups);
    void onCtrlptModeCfgResponse(@NonNull final BluetoothDevice device, List<LightControlHelmetSetup> hlmtList, List<LightControlBikeSetup> bikeList);
    void onCtrlptLedResponse(@NonNull final BluetoothDevice device, int floodCnt, int spotCnt);
    void onCtrlptSensorOffsetResponse(@NonNull final BluetoothDevice device, int[] offset);
    void onCtrlptRqstCurrentLimitResponse(@NonNull final BluetoothDevice device, int floodLimit, int spotLimit);
    void onCtrlptPrefModeResponse(@NonNull final BluetoothDevice device, int prefMode);
    void onCtrlptTempModeResponse(@NonNull final BluetoothDevice device, int tempMode);
}

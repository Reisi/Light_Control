package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface LightControlControlPointCallback {

    int LC_CP_OP_CODE_RQST_MODE_CNT = 1;
    int LC_CP_OP_CODE_SET_MODE = 2;
    int LC_CP_OP_CODE_RQST_GROUP_CNFG = 3;
    int LC_CP_OP_CODE_SET_GROUP_CNFG = 4;
    int LC_CP_OP_CODE_RQST_MODE_CNFG = 5;
    int LC_CP_OP_CODE_SET_MODE_CNFG = 6;
    int LC_CP_OP_CODE_RQST_LED_CNFG = 7;
    int LC_CP_OP_CODE_CHCK_LED_CNFG = 8;
    int LC_CP_OP_CODE_RQST_SENSOR_OFFSET = 9;
    int LC_CP_OP_CODE_CALIB_SENSOR_OFFSET = 10;
    int LC_CP_OP_CODE_RQST_CURRENT_LIMIT = 11;
    int LC_CP_OP_CODE_SET_CURRENT_LIMIT = 12;
    int LC_CP_OP_CODE_RQST_PREF_MODE = 13;
    int LC_CP_OP_CODE_SET_PREF_MODE = 14;
    int LC_CP_OP_CODE_RQST_TEMP_MODE = 15;
    int LC_CP_OP_CODE_SET_TEMP_MODE = 16;
    int LC_CP_OP_CODE_RESPONSE_CODE = 32;

    int LC_CP_RSPNS_VALUE_SUCCESS = 1;
    int LC_CP_RSPNS_VALUE_NOT_SUPPORTED = 2;
    int LC_CP_RSPNS_VALUE_INVALID_PARAM = 3;
    int LC_CP_RSPNS_VALUE_FAILED = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            LC_CP_OP_CODE_RQST_MODE_CNT,
            LC_CP_OP_CODE_SET_MODE,
            LC_CP_OP_CODE_RQST_GROUP_CNFG,
            LC_CP_OP_CODE_SET_GROUP_CNFG,
            LC_CP_OP_CODE_RQST_MODE_CNFG,
            LC_CP_OP_CODE_SET_MODE_CNFG,
            LC_CP_OP_CODE_RQST_LED_CNFG,
            LC_CP_OP_CODE_CHCK_LED_CNFG,
            LC_CP_OP_CODE_RQST_SENSOR_OFFSET,
            LC_CP_OP_CODE_CALIB_SENSOR_OFFSET,
            LC_CP_OP_CODE_RQST_CURRENT_LIMIT,
            LC_CP_OP_CODE_SET_CURRENT_LIMIT,
            LC_CP_OP_CODE_RQST_PREF_MODE,
            LC_CP_OP_CODE_SET_PREF_MODE,
            LC_CP_OP_CODE_RQST_TEMP_MODE,
            LC_CP_OP_CODE_SET_TEMP_MODE,
            LC_CP_OP_CODE_RESPONSE_CODE
    })
    @interface LCCPOpCode {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            LC_CP_RSPNS_VALUE_SUCCESS,
            LC_CP_RSPNS_VALUE_NOT_SUPPORTED,
            LC_CP_RSPNS_VALUE_INVALID_PARAM,
            LC_CP_RSPNS_VALUE_FAILED
    })
    @interface LCCPResponseValue {}

    /**
     * Method called when an Control Point operation has finished with an error or on successful
     * operations not covered by other methods.
     *
     * @param device        the target device
     * @param responseValue the received response value
     * @param opCode        the operation code that has completed
     */
    void onLCCPCommonResponse(@NonNull final BluetoothDevice device,
                              @LCCPResponseValue int responseValue, @LCCPOpCode int opCode);

    /**
     * Method called when a response to a LC_CP_OP_CODE_RQST_MODE_CNT request has been received.
     *
     * @param device    the target device
     * @param modeCnt   the number of modes supported by the device
     */
    void onLCCPModeCntResponse(@NonNull final BluetoothDevice device, int modeCnt);

    /**
     * Method called when a response to a LC_CP_OP_CODE_RQST_GROUP_CNFG request has been received.
     *
     * @param device    the target device
     * @param groups    the current number of groups of the device
     */
    void onLCCPGroupCfgResponse(@NonNull final BluetoothDevice device, int groups);

    /**
     * Method called when a response to a LC_CP_OP_CODE_RQST_MODE_CNFG request has been received.
     *
     * @param device    the target device
     * @param modeList  the unconverted data representing the current list
     */
    void onLCCPModeCfgResponse(@NonNull final BluetoothDevice device,
                               byte[] modeList);

    /**
     * Method called when a response to a LC_CP_OP_CODE_RQST_LED_CNFG or
     * LC_CP_OP_CODE_CHCK_LED_CNFG request/command has been received.
     *
     * @param device    the target device
     * @param floodCnt  the number of in series connected LEDs connected to the flood driver
     * @param spotCnt   the number of in series connected LEDs connected to the spot driver
     */
    void onLCCPLedResponse(@NonNull final BluetoothDevice device, int floodCnt, int spotCnt);

    /**
     * Method called when a response to a LC_CP_OP_CODE_RQST_SENSOR_OFFSET or
     * LC_CP_OP_CODE_CALIB_SENSOR_OFFSET request/command has been received.
     *
     * @param device    the target device
     * @param offset    an array with the offset for x, y and z-axis
     */
    void onLCCPSensorOffsetResponse(@NonNull final BluetoothDevice device, int[] offset);

    /**
     * Method called when a response to a LC_CP_OP_CODE_RQST_CURRENT_LIMIT request has been
     * received.
     *
     * @param device        the target device
     * @param floodLimit    the current limit for flood output (in %)
     * @param spotLimit     the current limit of spot output (in %)
     */
    void onLCCPRqstCurrentLimitResonse(@NonNull final BluetoothDevice device,
                                       int floodLimit, int spotLimit);

    /**
     * Method called when a response to a LC_CP_OP_CODE_RQST_PREF_MODE request has been received.
     *
     * @param device    the target device
     * @param prefMode  the current set preferred mode (an invalid number (>= mode count) if not
     *                  set)
     */
    void onLCCPPrefModeResponse(@NonNull final BluetoothDevice device, int prefMode);

    /**
     * Method called when a response to a LC_CP_OP_CODE_RQST_TEMP_MODE request has been received.
     *
     * @param device    the target device
     * @param tempMode  the current set temporary mode (an invalid number (>= mode count) if not
     *                  set)
     */
    void onLCCPTempModeResponse(@NonNull final BluetoothDevice device, int tempMode);
}

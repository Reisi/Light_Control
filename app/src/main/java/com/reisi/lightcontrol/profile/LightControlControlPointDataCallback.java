package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse;
import no.nordicsemi.android.ble.data.Data;

public abstract class LightControlControlPointDataCallback extends ProfileReadResponse implements LightControlControlPointCallback {
    public LightControlControlPointDataCallback() {
        // empty
    }

    protected LightControlControlPointDataCallback(Parcel in) {
        super(in);
    }

    @Override
    public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
        super.onDataReceived(device, data);

        if (data.size() < 3) {
            // TODO: invalid data handler?
            return; // invalid response
        }

        int opCode = data.getIntValue(Data.FORMAT_UINT8, 0);
        int requestOpCode = data.getIntValue(Data.FORMAT_UINT8, 1);
        int responseValue = data.getIntValue(Data.FORMAT_UINT8, 2);


        if (opCode != LC_CP_OP_CODE_RESPONSE_CODE) {
            // TODO: invalid response handler?
            return; // invalid response
        }

        // response with error code, call common handler
        if (responseValue != LC_CP_RSPNS_VALUE_SUCCESS) {
            onLCCPCommonResponse(device, responseValue, requestOpCode);
            return;
        }

        switch (requestOpCode) {
            case LC_CP_OP_CODE_RQST_MODE_CNT:
                if (data.size() == 4) {
                    int modeCnt = data.getIntValue(Data.FORMAT_UINT8, 3);
                    onLCCPModeCntResponse(device, modeCnt);
                } else {
                    // TODO: invalid data handler?
                }
                break;
            case LC_CP_OP_CODE_RQST_GROUP_CNFG:
                if (data.size() == 4) {
                    int groups = data.getIntValue(Data.FORMAT_UINT8, 3);
                    onLCCPGroupCfgResponse(device, groups);
                } else {
                    // TODO: invalid data handler?
                }
                break;
            case LC_CP_OP_CODE_RQST_MODE_CNFG:
                byte[] rawData = new byte[data.size() - 3];
                System.arraycopy(data.getValue(), 3, rawData, 0, rawData.length);
                onLCCPModeCfgResponse(device, rawData);
                break;
            case LC_CP_OP_CODE_RQST_LED_CNFG:
            case LC_CP_OP_CODE_CHCK_LED_CNFG:
                if (data.size() == 5) {
                    int floodCnt = data.getIntValue(Data.FORMAT_UINT8, 3);
                    int spotCnt = data.getIntValue(Data.FORMAT_UINT8, 4);
                    onLCCPLedResponse(device, floodCnt, spotCnt);
                } else {
                    // TODO invalid data handler
                }
                break;
            case LC_CP_OP_CODE_RQST_SENSOR_OFFSET:
            case LC_CP_OP_CODE_CALIB_SENSOR_OFFSET:
                if (data.size() == 9) {
                    int[] sensorOffset = new int[3];
                    sensorOffset[0] = data.getIntValue(Data.FORMAT_SINT16, 3);
                    sensorOffset[1] = data.getIntValue(Data.FORMAT_SINT16, 5);
                    sensorOffset[2] = data.getIntValue(Data.FORMAT_SINT16, 7);
                    onLCCPSensorOffsetResponse(device, sensorOffset);
                } else {
                    // TODO: invalid data handler
                }
                break;
            case LC_CP_OP_CODE_RQST_CURRENT_LIMIT:
                if (data.size() == 5) {
                    int floodlimit = data.getIntValue(Data.FORMAT_UINT8, 3);
                    int spotLimit = data.getIntValue(Data.FORMAT_UINT8, 4);
                    onLCCPRqstCurrentLimitResonse(device, floodlimit, spotLimit);
                } else {
                    // TODO: invlaid data handler
                }
                break;
            case LC_CP_OP_CODE_RQST_PREF_MODE:
                if (data.size() == 4) {
                    int prefMode = data.getIntValue(Data.FORMAT_UINT8, 3);
                    onLCCPPrefModeResponse(device, prefMode);
                } else {
                    // TODO: invalid data handler
                }
                break;
            case LC_CP_OP_CODE_RQST_TEMP_MODE:
                if (data.size() == 4) {
                    int tempMode = data.getIntValue(Data.FORMAT_UINT8, 3);
                    onLCCPTempModeResponse(device, tempMode);
                } else {
                    // TODO: invalid data handler
                }
                break;
            default:
                onLCCPCommonResponse(device, responseValue, requestOpCode);
        }
    }
}

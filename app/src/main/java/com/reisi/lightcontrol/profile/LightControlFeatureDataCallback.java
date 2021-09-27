package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse;
import no.nordicsemi.android.ble.data.Data;

public abstract class LightControlFeatureDataCallback extends ProfileReadResponse implements LightControlFeatureCallback {
    public int lightType;
    public LCConfigurationFeatures configurationFeatures;
    public LCSetupFeatures setupFeatures;
    public LCHelmetLightFeatures helmetLightFeatures;
    public LCBikeLightFeatures bikeLightFeatures;

    public LightControlFeatureDataCallback() {
        lightType = LC_LF_LT_UNKNOWN;
    }

    protected LightControlFeatureDataCallback(Parcel in) {
        super(in);
    }

    @Override
    public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
        super.onDataReceived(device, data);

        if (data.size() != 4) {
            lightType = LC_LF_LT_UNKNOWN;
            onLightControlFeatureReceived(device, lightType);
            // TODO: invalid data handler?
            return;
        }

        int type = data.getIntValue(Data.FORMAT_UINT8, 0);
        int configuration = data.getIntValue(Data.FORMAT_UINT8, 1);
        int setup = data.getIntValue(Data.FORMAT_UINT8, 2);
        int light = data.getIntValue(Data.FORMAT_UINT8, 3);

        configurationFeatures = new LCConfigurationFeatures(configuration);
        setupFeatures = new LCSetupFeatures(setup);

        switch (type) {
            case LC_LF_LT_HELMET_LIGHT:
                lightType = LC_LF_LT_HELMET_LIGHT;
                helmetLightFeatures = new LCHelmetLightFeatures(light);
                onLightControlFeatureReceived(device, lightType, configurationFeatures, setupFeatures, helmetLightFeatures);
                break;
            case LC_LF_LT_BIKE_LIGHT:
                lightType = LC_LF_LT_BIKE_LIGHT;
                bikeLightFeatures = new LCBikeLightFeatures(light);
                onLightControlFeatureReceived(device, lightType, configurationFeatures, setupFeatures, bikeLightFeatures);
                break;
            case LC_LF_LT_TAIL_LIGHT:
                //break;
            default:
                lightType = LC_LF_LT_UNKNOWN;
                onLightControlFeatureReceived(device, lightType);
        }
    }
}

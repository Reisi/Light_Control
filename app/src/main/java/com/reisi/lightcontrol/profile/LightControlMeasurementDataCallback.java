package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse;
import no.nordicsemi.android.ble.data.Data;

import static com.reisi.lightcontrol.profile.LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT;
import static com.reisi.lightcontrol.profile.LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT;

public abstract class LightControlMeasurementDataCallback extends ProfileReadResponse implements LightControlMeasurementCallback {

    public LightControlMeasurementDataCallback() {
        // empty
    }

    protected LightControlMeasurementDataCallback(Parcel in) {
        super(in);
    }

    @Override
    public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
        super.onDataReceived(device, data);

        int offset = 0, flags, type;

        if (data.size() < 4) {
            // TODO: invalid data handler?
            return;
        }

        // light type
        type = data.getIntValue(Data.FORMAT_UINT8, offset);
        offset += 1;

        // flags
        flags = data.getIntValue(Data.FORMAT_UINT16, offset);
        offset += 2;

        // helmet lights
        if (type == LC_LF_LT_HELMET_LIGHT) {
            // read light setup and status
            LightControlHelmetSetup setup = new LightControlHelmetSetup();
            boolean overCurrent = false, voltageLimiting = false, tempLimiting = false, dcLimit = false;

            // setup
            int i = data.getIntValue(Data.FORMAT_UINT8, offset);
            offset += 1;
            setup.floodActive = (i & 0x1) != 0;
            setup.spotActive = (i & 0x2) != 0;
            setup.pitchComensation = (i & 0x4) != 0;
            setup.cloned = (i & 0x08) != 0;
            setup.taillight = (i & 0x10) != 0;
            setup.brakeLight = (i & 0x20) != 0;
            // intensity
            if ((flags & 1) != 0) { // intensity present flag
                setup.intensity = data.getIntValue(Data.FORMAT_UINT8, offset);
                offset += 1;
            }
            // flood status
            if ((flags & 2) != 0) { // flood status present flag
                i = data.getIntValue(Data.FORMAT_UINT8, offset);
                offset += 1;
                overCurrent = (i & 1) != 0;
                voltageLimiting = (i & 2) != 0;
                tempLimiting = (i & 4) != 0;
                dcLimit = (i & 8) != 0;
            }
            // spot status
            if ((flags & 4) != 0) { // spot status present flag
                i = data.getIntValue(Data.FORMAT_UINT8, offset);
                offset += 1;
                overCurrent = overCurrent || (i & 1) != 0;
                voltageLimiting = voltageLimiting || (i & 2) != 0;
                tempLimiting = tempLimiting || (i & 4) != 0;
                dcLimit = dcLimit || (i & 8) != 0;
            }
            // call handler
            onMeasurementSetupReceived(device, setup, overCurrent, voltageLimiting, tempLimiting, dcLimit);
        }

        // bike lights
        if (type == LC_LF_LT_BIKE_LIGHT) {
            // read light setup and status
            LightControlBikeSetup setup = new LightControlBikeSetup();
            boolean overCurrent = false, voltageLimiting = false, tempLimiting = false, dcLimit = false;

            // setup
            int i = data.getIntValue(Data.FORMAT_UINT8, offset);
            offset += 1;
            setup.mainBeamActive = (i & 0x1) != 0;
            setup.extendedMainBeamActive = (i & 0x2) != 0;
            setup.highBeamActive = (i & 0x4) != 0;
            setup.daylightActive = (i & 0x08) != 0;
            setup.taillight = (i & 0x10) != 0;
            setup.brakeLight = (i & 0x20) != 0;
            // intensity
            if ((flags & 1) != 0) { // intensity present flag
                setup.mainBeamIntensity = data.getIntValue(Data.FORMAT_UINT8, offset);
                offset += 1;
                setup.highBeamIntensity = data.getIntValue(Data.FORMAT_UINT8, offset);
                offset += 1;
            }
            // main beam status
            if ((flags & 2) != 0) { // main beam status present flag
                i = data.getIntValue(Data.FORMAT_UINT8, offset);
                offset += 1;
                overCurrent = (i & 1) != 0;
                voltageLimiting = (i & 2) != 0;
                tempLimiting = (i & 4) != 0;
                dcLimit = (i & 8) != 0;
            }
            // high beam status
            if ((flags & 4) != 0) { // high beam status present flag
                i = data.getIntValue(Data.FORMAT_UINT8, offset);
                offset += 1;
                overCurrent = overCurrent || (i & 1) != 0;
                voltageLimiting = voltageLimiting || (i & 2) != 0;
                tempLimiting = tempLimiting || (i & 4) != 0;
                dcLimit = dcLimit || (i & 8) != 0;
            }
            // call handler
            onMeasurementSetupReceived(device, setup, overCurrent, voltageLimiting, tempLimiting, dcLimit);
        }

        // process measurement data
        Float power = null, temperature = null, voltage = null, pitchAngle = null, soc = null, tailPower = null;

        // power
        if ((flags & 0x0018) != 0) {
            float j = 0;
            if ((flags & 0x0008) != 0) {
                j += (float) data.getIntValue(Data.FORMAT_UINT16, offset) / 1000;
                offset += 2;
            }
            if ((flags & 0x0010) != 0) {
                j += (float) data.getIntValue(Data.FORMAT_UINT16, offset) / 1000;
                offset += 2;
            }
            power = j;
        }

        // temperature
        if ((flags & 0x0020) != 0) {
            float j = (float) data.getIntValue(Data.FORMAT_SINT8, offset);
            offset += 1;
            temperature = j;
        }

        // input voltage
        if ((flags & 0x0040) != 0) {
            float j = (float) data.getIntValue(Data.FORMAT_UINT16, offset) / 1000;
            offset += 2;
            voltage = j;
        }

        // pitch angle
        if ((flags & 0x0080) != 0) {
            float j = (float) data.getIntValue(Data.FORMAT_SINT8, offset);
            offset += 1;
            pitchAngle = j;
        }

        // battery soc
        if ((flags & 0x0100) != 0) {
            float j = (float) data.getIntValue(Data.FORMAT_UINT8, offset);
            offset += 1;
            soc = j;
        }

        // battery soc
        if ((flags & 0x0200) != 0) {
            float j = (float) data.getIntValue(Data.FORMAT_UINT16, offset) / 1000;
            offset += 2;
            tailPower = j;
        }

        if (power != null || temperature != null || voltage != null || pitchAngle != null || soc != null || tailPower != null) {
            onMeasurementDataReceived(device, power, temperature, voltage, pitchAngle, soc, tailPower);
        }
    }

   // public abstract void onMeasurementDataReceived(@NonNull BluetoothDevice device, Float power, Float temperature, Float voltage, Float pitch, Float soc, Float tailPower);
}

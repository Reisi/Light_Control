package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface LightControlFeatureCallback {

    int LC_LF_LT_HELMET_LIGHT = 0;
    int LC_LF_LT_BIKE_LIGHT = 1;
    int LC_LF_LT_TAIL_LIGHT = 2;
    int LC_LF_LT_UNKNOWN = 15;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            LC_LF_LT_HELMET_LIGHT,
            LC_LF_LT_BIKE_LIGHT,
            LC_LF_LT_TAIL_LIGHT,
            LC_LF_LT_UNKNOWN
    })
    @interface LCLightType {}

    class LCConfigurationFeatures {
        public final boolean modeChangeSupported;
        public final boolean modeConfigurationSupported;
        public final boolean modeGroupingSupported;
        public final boolean preferredModeSupported;
        public final boolean temporaryModeSupported;

        @SuppressWarnings("WeakerAccess")
        public LCConfigurationFeatures(final int configurationFeatures) {
            modeChangeSupported = (configurationFeatures & 0x0001) != 0;
            modeConfigurationSupported = (configurationFeatures & 0x0002) != 0;
            modeGroupingSupported = (configurationFeatures & 0x0004) != 0;
            preferredModeSupported = (configurationFeatures & 0x0008) != 0;
            temporaryModeSupported = (configurationFeatures & 0x0010) != 0;
        }
    }

    class LCSetupFeatures {
        public final boolean ledConfigurationCheckSupported;
        public final boolean sensorOffsetSupported;
        public final boolean currentLimitSupported;

        @SuppressWarnings("WeakerAccess")
        public LCSetupFeatures(final int setupFeatures) {
            ledConfigurationCheckSupported = (setupFeatures & 0x0001) != 0;
            sensorOffsetSupported = (setupFeatures & 0x0002) != 0;
            currentLimitSupported = (setupFeatures & 0x0004) != 0;
        }
    }

    class LCHelmetLightFeatures {
        public final boolean floodSupported;
        public final boolean spotSupported;
        public final boolean pitchCompensationSupported;
        public final boolean driverCloningSupported;
        public final boolean externalTaillightSupported;
        public final boolean externalBrakelightSupported;

        @SuppressWarnings("WeakerAccess")
        public LCHelmetLightFeatures(final int helmetLightFeatures) {
            floodSupported = (helmetLightFeatures & 0x0001) != 0;
            spotSupported = (helmetLightFeatures & 0x0002) != 0;
            pitchCompensationSupported = (helmetLightFeatures & 0x0004) != 0;
            driverCloningSupported = (helmetLightFeatures & 0x0008) != 0;
            externalTaillightSupported = (helmetLightFeatures & 0x0010) != 0;
            externalBrakelightSupported = (helmetLightFeatures & 0x0020) != 0;
        }
    }

    class LCBikeLightFeatures {
        public final boolean mainBeamSupported;
        public final boolean extendedMainBeamSupported;
        public final boolean highBeamCompensationSupported;
        public final boolean daylightSupported;
        public final boolean externalTaillightSupported;
        public final boolean externalBrakelightSupported;

        @SuppressWarnings("WeakerAccess")
        public LCBikeLightFeatures(final int bikeLightFeatures) {
            mainBeamSupported = (bikeLightFeatures & 0x0001) != 0;
            extendedMainBeamSupported = (bikeLightFeatures & 0x0002) != 0;
            highBeamCompensationSupported = (bikeLightFeatures & 0x0004) != 0;
            daylightSupported = (bikeLightFeatures & 0x0008) != 0;
            externalTaillightSupported = (bikeLightFeatures & 0x0010) != 0;
            externalBrakelightSupported = (bikeLightFeatures & 0x0020) != 0;
        }
    }

    /**
     * Method called when the LightControlService Feature Characteristic has been read for an unknown light type
     *
     * @param device    the target device
     * @param lightType the light type
     */
    void onLightControlFeatureReceived(@NonNull final BluetoothDevice device,
                                       @LCLightType final int lightType);

    /**
     * Method called when the LightControlService Feature Characteristic has been read for an unknown light type
     *
     * @param device                the target device
     * @param lightType             the light type
     * @param configurationFeatures the configuration features
     * @param setupFeatures         the setup features
     * @param helmetLightFeatures   the helmet light features
     */
    void onLightControlFeatureReceived(@NonNull final BluetoothDevice device,
                                       @LCLightType final int lightType,
                                       @NonNull final LCConfigurationFeatures configurationFeatures,
                                       @NonNull final LCSetupFeatures setupFeatures,
                                       @NonNull final LCHelmetLightFeatures helmetLightFeatures);

    /**
     * Method called when the LightControlService Feature Characteristic has been read for an unknown light type
     *
     * @param device                the target device
     * @param lightType             the light type
     * @param configurationFeatures the configuration features
     * @param setupFeatures         the setup features
     * @param bikeLightFeatures     the bike light features
     */
    void onLightControlFeatureReceived(@NonNull final BluetoothDevice device,
                                       @LCLightType final int lightType,
                                       @NonNull final LCConfigurationFeatures configurationFeatures,
                                       @NonNull final LCSetupFeatures setupFeatures,
                                       @NonNull final LCBikeLightFeatures bikeLightFeatures);
}

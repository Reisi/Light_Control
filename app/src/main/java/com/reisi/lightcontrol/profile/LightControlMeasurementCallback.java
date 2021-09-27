package com.reisi.lightcontrol.profile;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

public interface LightControlMeasurementCallback {
    /**
     * Method called when data received from the measurement characteristic
     *
     * @param device                the target device
     * @param setup                 the current light setup of the device
     * @param overcurrent           indicator if over current limiting is active
     * @param voltageLimiting       indicator if limiting due to  low input voltage is active
     * @param temperatureLimiting   indicator if limiting due to high temperature is active
     * @param dutyCycleLimit        indicator if duty cycle limits are reached
     */
    void onMeasurementSetupReceived(@NonNull final BluetoothDevice device,
                                    @NonNull final LightControlHelmetSetup setup,
                                    boolean overcurrent,
                                    boolean voltageLimiting,
                                    boolean temperatureLimiting,
                                    boolean dutyCycleLimit);

    /**
     * Method called when data received from the measurement characteristic
     *
     * @param device                the target device
     * @param setup                 the current light setup of the device
     * @param overcurrent           indicator if over current limiting is active
     * @param voltageLimiting       indicator if limiting due to  low input voltage is active
     * @param temperatureLimiting   indicator if limiting due to high temperature is active
     * @param dutyCycleLimit        indicator if duty cycle limits are reached
     */
    void onMeasurementSetupReceived(@NonNull final BluetoothDevice device,
                                    @NonNull final LightControlBikeSetup setup,
                                    boolean overcurrent,
                                    boolean voltageLimiting,
                                    boolean temperatureLimiting,
                                    boolean dutyCycleLimit);

    /**
     * Metohd called when the received measurement data includes one of the containing values
     *
     * @param device                the target device
     * @param power                 the calculated output power of the device
     * @param temperature           the current temperature of the device
     * @param voltage               the current input voltage of the device
     * @param pitch                 the current pitch angle of the device in °
     * @param soc                   the current state of charge of connected battery in %
     * @param tailPower             the current power of connected taillight in W
     */
    void onMeasurementDataReceived(@NonNull final BluetoothDevice device,
                                   final Float power,
                                   final Float temperature,
                                   final Float voltage,
                                   final Float pitch,
                                   final Float soc,
                                   final Float tailPower);
}

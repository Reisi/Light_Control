package com.reisi.lightcontrol.ui.device;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlBikeSetup;
import com.reisi.lightcontrol.profile.LightControlService;
import com.reisi.lightcontrol.profile.LightControlHelmetSetup;

import java.util.ArrayList;
import java.util.List;

import static com.reisi.lightcontrol.profile.LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT;
import static com.reisi.lightcontrol.profile.LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT;

public class DeviceActivityStatus<E extends LightControlService.LCSBinder> extends Fragment {
    private static class StatusDataItem {
        public String name;
        public String unit;
        public float value;
        public int age;
        public int position;
    }

    private static class StatusHelmetSetupItem extends LightControlHelmetSetup {
        public boolean overCurrent;
        public boolean voltageLimiting;
        public boolean temperatureLimiting;
        public boolean dutyCycleLimit;
        public boolean inUse;
    }

    private static class StatusBikeSetupItem extends LightControlBikeSetup {
        public boolean overCurrent;
        public boolean voltageLimiting;
        public boolean temperatureLimiting;
        public boolean dutyCycleLimit;
        public boolean inUse;
    }

    private static final int AGE_TIMEOUT = 5;

    private final BluetoothDevice device;
    private final E serviceBinder;
    private final Context mContext;
    private TextView revision;
    private RecyclerView statusItemList;

    private DeviceStatusAdapter statusItemAdapter;
    private final StatusHelmetSetupItem helmetSetupItem = new StatusHelmetSetupItem();
    private final StatusBikeSetupItem bikeSetupItem = new StatusBikeSetupItem();
    private final List<StatusDataItem> dataItems = new ArrayList<>();

    private final Handler handler = new Handler();

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            for (StatusDataItem item : dataItems) {
                String value;
                if (item.age < AGE_TIMEOUT) {
                    item.age++;
                    if (item.name.equals(mContext.getResources().getString(R.string.dev_status_power)) ||
                        item.name.equals(mContext.getResources().getString(R.string.dev_status_voltage))) {
                        value = String.format("%.2f", item.value);
                    } else if (item.name.equals(mContext.getResources().getString(R.string.dev_status_tail_power))) {
                        value = String.format("%.3f", item.value);
                    } else {
                        value = String.format("%.0f", item.value);
                    }
                } else {
                    value = getResources().getString(R.string.measurement_default);
                }
                statusItemAdapter.setDataValue(value, item.position);
            }
            if (bikeSetupItem.inUse)
                statusItemAdapter.setSetupValue(bikeSetupItem);
            else if (helmetSetupItem.inUse)
                statusItemAdapter.setSetupValue(helmetSetupItem);
            handler.postDelayed(this, 1000);
        }
    };

    public DeviceActivityStatus(Context context, BluetoothDevice device, E serviceBinder) {
        this.mContext = context;
        this.device = device;
        this.serviceBinder = serviceBinder;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String address = intent.getStringExtra(LightControlService.EXTRA_DEVICE_ADDRESS);
            if (address == null || !address.equals(device.getAddress())) {
                return;
            }
            final String action = intent.getAction();
            switch (action) {
                case LightControlService.BROADCAST_MEASUREMENT_RECEIVED:
                    if (intent.hasExtra(LightControlService.EXTRA_POWER)) {
                        for (StatusDataItem item : dataItems) {
                            if (item.name.equals(mContext.getResources().getString(R.string.dev_status_power))) {
                                item.age = 0;
                                item.value = intent.getFloatExtra(LightControlService.EXTRA_POWER, 0);
                            }
                        }
                    }
                    if (intent.hasExtra(LightControlService.EXTRA_TEMPERATURE)) {
                        for (StatusDataItem item : dataItems) {
                            if (item.name.equals(mContext.getResources().getString(R.string.dev_status_temperature))) {
                                item.age = 0;
                                item.value = intent.getFloatExtra(LightControlService.EXTRA_TEMPERATURE, 0);
                            }
                        }
                    }
                    if (intent.hasExtra(LightControlService.EXTRA_INPUT_VOLTAGE)) {
                        for (StatusDataItem item : dataItems) {
                            if (item.name.equals(mContext.getResources().getString(R.string.dev_status_voltage))) {
                                item.age = 0;
                                item.value = intent.getFloatExtra(LightControlService.EXTRA_INPUT_VOLTAGE, 0);
                            }
                        }
                    }
                    if (intent.hasExtra(LightControlService.EXTRA_PITCH_ANGLE)) {
                        for (StatusDataItem item : dataItems) {
                            if (item.name.equals(mContext.getResources().getString(R.string.dev_status_pitch))) {
                                item.age = 0;
                                item.value = intent.getFloatExtra(LightControlService.EXTRA_PITCH_ANGLE, 0);
                            }
                        }
                    }
                    if (intent.hasExtra(LightControlService.EXTRA_BATTERY_SOC)) {
                        for (StatusDataItem item : dataItems) {
                            if (item.name.equals(mContext.getResources().getString(R.string.dev_status_soc))) {
                                item.age = 0;
                                item.value = intent.getFloatExtra(LightControlService.EXTRA_BATTERY_SOC, 0);
                            }
                        }
                    }
                    if (intent.hasExtra(LightControlService.EXTRA_TAILLIGHT_POWER)) {
                        for (StatusDataItem item : dataItems) {
                            if (item.name.equals(mContext.getResources().getString(R.string.dev_status_tail_power))) {
                                item.age = 0;
                                item.value = intent.getFloatExtra(LightControlService.EXTRA_TAILLIGHT_POWER, 0);
                            }
                        }
                    }
                    break;
                case LightControlService.BROADCAST_SETUP_HELMET_RECEIVED:
                    LightControlHelmetSetup newHelmetSetup = intent.getParcelableExtra(LightControlService.EXTRA_SETUP_HELMET);
                    if (newHelmetSetup == null)
                        return;
                    newHelmetSetup.copy(helmetSetupItem);
                    helmetSetupItem.overCurrent = intent.getBooleanExtra(LightControlService.EXTRA_STATUS_OVERCURRENT, false);
                    helmetSetupItem.voltageLimiting = intent.getBooleanExtra(LightControlService.EXTRA_STATUS_VOLTAGE, false);
                    helmetSetupItem.temperatureLimiting = intent.getBooleanExtra(LightControlService.EXTRA_STATUS_TEMPERATURE, false);
                    helmetSetupItem.dutyCycleLimit = intent.getBooleanExtra(LightControlService.EXTRA_STATUS_DUTYCYCLE, false);
                    bikeSetupItem.inUse = false;
                    helmetSetupItem.inUse = true;
                    break;
                case LightControlService.BROADCAST_SETUP_BIKE_RECEIVED:
                    LightControlBikeSetup newBikeSetup = intent.getParcelableExtra(LightControlService.EXTRA_SETUP_BIKE);
                    if (newBikeSetup == null)
                        return;
                    newBikeSetup.copy(bikeSetupItem);
                    bikeSetupItem.overCurrent = intent.getBooleanExtra(LightControlService.EXTRA_STATUS_OVERCURRENT, false);
                    bikeSetupItem.voltageLimiting = intent.getBooleanExtra(LightControlService.EXTRA_STATUS_VOLTAGE, false);
                    bikeSetupItem.temperatureLimiting = intent.getBooleanExtra(LightControlService.EXTRA_STATUS_TEMPERATURE, false);
                    bikeSetupItem.dutyCycleLimit = intent.getBooleanExtra(LightControlService.EXTRA_STATUS_DUTYCYCLE, false);
                    helmetSetupItem.inUse = true;
                    bikeSetupItem.inUse = true;
                    break;
                case LightControlService.BROADCAST_CONTROLPOINT_MODE_CFG:
                    if (intent.hasExtra(LightControlService.EXTRA_MODE_HELMET_LIST)) {
                        List<LightControlHelmetSetup> modes = intent.getParcelableArrayListExtra(LightControlService.EXTRA_MODE_HELMET_LIST);
                        if (modes == null)
                            return;
                        statusItemAdapter.updateModeListHelmet(modes);
                    }
                    if (intent.hasExtra(LightControlService.EXTRA_MODE_BIKE_LIST)) {
                        List<LightControlBikeSetup> modes = intent.getParcelableArrayListExtra(LightControlService.EXTRA_MODE_BIKE_LIST);
                        if (modes == null)
                            return;
                        statusItemAdapter.updateModeListBike(modes);
                    }
                    break;
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(LightControlService.BROADCAST_MEASUREMENT_RECEIVED);
        filter.addAction(LightControlService.BROADCAST_SETUP_HELMET_RECEIVED);
        filter.addAction(LightControlService.BROADCAST_SETUP_BIKE_RECEIVED);
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_MODE_CFG);
        return filter;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StatusDataItem power = new StatusDataItem();
        power.name = mContext.getResources().getString(R.string.dev_status_power);
        power.unit = mContext.getResources().getString(R.string.dev_status_power_unit_metric);
        power.age = AGE_TIMEOUT;
        //power.position =  0;
        dataItems.add(power);
        StatusDataItem temperature = new StatusDataItem();
        temperature.name = mContext.getResources().getString(R.string.dev_status_temperature);
        temperature.unit = mContext.getResources().getString(R.string.dev_status_temperatue_unit_metric);
        temperature.age = AGE_TIMEOUT;
        //temperature.position = 1;
        dataItems.add(temperature);
        StatusDataItem voltage = new StatusDataItem();
        voltage.name = mContext.getResources().getString(R.string.dev_status_voltage);
        voltage.unit = mContext.getResources().getString(R.string.dev_status_voltage_unit_metric);
        voltage.age = AGE_TIMEOUT;
        //voltage.position = 2;
        dataItems.add(voltage);
        StatusDataItem pitch = new StatusDataItem();
        pitch.name = mContext.getResources().getString(R.string.dev_status_pitch);
        pitch.unit = mContext.getResources().getString(R.string.dev_status_pitch_unit_metric);
        pitch.age = AGE_TIMEOUT;
        //pitch.position = 3;
        dataItems.add(pitch);
        StatusDataItem soc = new StatusDataItem();
        soc.name = mContext.getResources().getString(R.string.dev_status_soc);
        soc.unit = mContext.getResources().getString(R.string.dev_unit_percent);
        soc.age = AGE_TIMEOUT;
        //pitch.position = 3;
        dataItems.add(soc);
        StatusDataItem taillightPower = new StatusDataItem();
        taillightPower.name = mContext.getResources().getString(R.string.dev_status_tail_power);
        taillightPower.unit = mContext.getResources().getString(R.string.dev_status_power_unit_metric);
        taillightPower.age = AGE_TIMEOUT;
        //pitch.position = 3;
        dataItems.add(taillightPower);

        statusItemAdapter = new DeviceStatusAdapter(mContext, serviceBinder.getFeatures(device));
        for (StatusDataItem item : dataItems) {
            DeviceStatusAdapter.DataItemInfo adapterDataItem = new DeviceStatusAdapter.DataItemInfo();
            adapterDataItem.header = item.name;
            adapterDataItem.unit = item.unit;
            adapterDataItem.value = getResources().getString(R.string.measurement_default);
            item.position = statusItemAdapter.addDataItem(adapterDataItem);
        }
        if (serviceBinder.getFeatures(device).lightType == LC_LF_LT_HELMET_LIGHT)
            statusItemAdapter.setSetupValue(helmetSetupItem);
        else if (serviceBinder.getFeatures(device).lightType == LC_LF_LT_BIKE_LIGHT)
            statusItemAdapter.setSetupValue(bikeSetupItem);
        statusItemAdapter.setOnItemClickListener(modeNo -> serviceBinder.setMode(device, (byte)(modeNo == -1 ? 0xFF : modeNo)));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        View root = inflator.inflate(R.layout.fragment_device_status, container, false);

        revision = root.findViewById(R.id.revision);
        serviceBinder.requestModes(device);
        String firmware = serviceBinder.getFirmwareRev(device);
        String hardware = serviceBinder.getHardwareRev(device);
        String rev = getResources().getString(R.string.dev_status_fw_rev) + " " +
                (firmware != null ? firmware : getResources().getString(R.string.dev_status_rev)) + "\n" +
                getResources().getString(R.string.dev_status_hw_rev) + " " +
                (hardware != null ? hardware : getResources().getString(R.string.dev_status_rev));
        revision.setText(rev);

        statusItemList = root.findViewById(R.id.measdata);
        GridLayoutManager layoutManager = new GridLayoutManager(mContext, 2);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int type = statusItemAdapter.getItemViewType(position);
                switch (type) {
                    case DeviceStatusAdapter.TYPE_MODELIST:
                    case DeviceStatusAdapter.TYPE_SETUP:
                        return 2;
                    case DeviceStatusAdapter.TYPE_DATA:
                    default:
                        return 1;
                }
            }
        });
        statusItemList.setLayoutManager(layoutManager);
        statusItemList.setAdapter(statusItemAdapter);
        ((SimpleItemAnimator) statusItemList.getItemAnimator()).setSupportsChangeAnimations(false);


        LocalBroadcastManager.getInstance(mContext).registerReceiver(broadcastReceiver, makeIntentFilter());
        handler.postDelayed(runnable, 1000);

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        /*getActivity().unbindService(serviceConnection);
        serviceBinder = null;*/

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(broadcastReceiver);
        handler.removeCallbacks(runnable);

        dataItems.clear();
    }
}

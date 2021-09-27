package com.reisi.lightcontrol.ui.device;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlFeatureDataCallback;
import com.reisi.lightcontrol.profile.LightControlService;

import java.util.Objects;

public class DeviceActivitySetup<E extends LightControlService.LCSBinder> extends Fragment implements DeviceSetupAdapter.OnItemClickListener, DeviceSetupOffsetCalibrate.DeviceCalibrateListener {
    private final Context mContext;
    private final BluetoothDevice device;
    private final E serviceBinder;
    private DeviceSetupAdapter setupAdapter;
    private RecyclerView setupList;
    private LightControlFeatureDataCallback features;

    public DeviceActivitySetup(Context context, BluetoothDevice device, E serviceBinder) {
        this.mContext = context;
        this.device = device;
        this.serviceBinder = serviceBinder;
    }

    private static IntentFilter makeIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_SENSOR_OFFSET);
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_CURRENT_LIMIT);
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_LED_CFG);
        return filter;
    }

    private void setAdapter() {
        if (setupAdapter == null && features != null) {
            setupAdapter = new DeviceSetupAdapter(mContext, features);
            setupAdapter.setClickListener(this);
        }
        if (setupList != null) {
            setupList.setAdapter(setupAdapter);
        }
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
                /*case LightControlService.BROADCAST_CONTROLPOINT_COMMON:
                    int response = intent.getIntExtra(LightControlService.EXTRA_RESPONSE, 0);
                    String message;
                    if (response == LightControlManagerCallbacks.responseValue.success){
                        message = "success";
                    } else {
                        message = "failed";
                    }
                    Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                    break;*/
                case LightControlService.BROADCAST_CONTROLPOINT_SENSOR_OFFSET:
                    int[] offset = new int[3];
                    offset[0] = intent.getIntExtra(LightControlService.EXTRA_SENSOR_OFFSET_X, 0);
                    offset[1] = intent.getIntExtra(LightControlService.EXTRA_SENSOR_OFFSET_Y, 0);
                    offset[2] = intent.getIntExtra(LightControlService.EXTRA_SENSOR_OFFSET_Z, 0);
                    if (setupAdapter != null) {
                        setupAdapter.setOffset(offset);
                    }
                    break;
                case LightControlService.BROADCAST_CONTROLPOINT_CURRENT_LIMIT:
                    int[] limit = new int[2];
                    limit[0] = intent.getIntExtra(LightControlService.EXTRA_FLOOD_LIMIT, 0);
                    limit[1] = intent.getIntExtra(LightControlService.EXTRA_SPOT_LIMIT, 0);
                    if (setupAdapter != null) {
                        setupAdapter.setLimit(limit);
                    }
                    break;
                case LightControlService.BROADCAST_CONTROLPOINT_LED_CFG:
                    int[] count = new int[2];
                    count[0] = intent.getIntExtra(LightControlService.EXTRA_FLOOD_LED_CNT, -1);
                    count[1] = intent.getIntExtra(LightControlService.EXTRA_SPOT_LED_CNT, -1);
                    if (setupAdapter != null) {
                        setupAdapter.setLedCnt(count);
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        features = serviceBinder.getFeatures(device);
        if (features != null) {
            // TODO: öhhm, features should be already available, but what if not?
            if (features.setupFeatures.sensorOffsetSupported) {
                serviceBinder.requestOffset(device);
            }
            if (features.setupFeatures.currentLimitSupported) {
                serviceBinder.requestCurrentLimit(device);
            }
            if (features.setupFeatures.ledConfigurationCheckSupported) {
                serviceBinder.requestLedConfiguration(device);
            }
        }

        LocalBroadcastManager.getInstance(mContext).registerReceiver(broadcastReceiver, makeIntentFilter());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        View root = inflator.inflate(R.layout.fragment_device_setup, container, false);

        setupList = root.findViewById(R.id.setupList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        setupList.setLayoutManager(layoutManager);
        ((SimpleItemAnimator) Objects.requireNonNull(setupList.getItemAnimator())).setSupportsChangeAnimations(false);
        //if (setupAdapter != null) {    // check if adapter is already set
            setAdapter();
        //}

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(broadcastReceiver);
    }

    /*@Override
    public void onOffsetReadClick() {
        serviceBinder.requestOffset(device);
    }*/

    @Override
    public void onOffsetCalibrateClick() {
        DeviceSetupOffsetCalibrate confirmation = new DeviceSetupOffsetCalibrate(this);
        confirmation.show(getFragmentManager(), "calibration confirmation");
        //serviceBinder.calibrateOffset(device);
    }

    @Override
    public void onLimitReadClick() {
        serviceBinder.requestCurrentLimit(device);
    }

    @Override
    public void onLimitUpdateClick(int[] limit) {
        serviceBinder.setCurrentLimit(device, (byte)limit[0], (byte)limit[1]);
    }

    /*@Override
    public void onLedConfigurationReadClick() {
        serviceBinder.requestLedConfiguration(device);
    }*/

    @Override
    public void onLedConfigurationCheckClick() {
        serviceBinder.checkLedConfiguration(device);
    }

    @Override
    public void onDialogCalibrateClick(DialogFragment dialog) {
        serviceBinder.calibrateOffset(device);
    }

    @Override
    public void onDialogCancelClick(DialogFragment dialog) {
        // nothing to do here
    }
}

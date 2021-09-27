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
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlBikeSetup;
import com.reisi.lightcontrol.profile.LightControlFeatureCallback;
import com.reisi.lightcontrol.profile.LightControlFeatureDataCallback;
import com.reisi.lightcontrol.profile.LightControlService;
import com.reisi.lightcontrol.profile.LightControlHelmetSetup;

import java.util.ArrayList;
import java.util.List;

public class DeviceActivityConfig<E extends LightControlService.LCSBinder> extends Fragment {
    private final Context mContext;
    private final BluetoothDevice device;
    private final E serviceBinder;

    private int groups, numberOfModes, preferredMode, temporaryMode;
    LightControlFeatureDataCallback features;
    private List<LightControlHelmetSetup> hlmtList = new ArrayList<>();
    private List<LightControlBikeSetup> bkList = new ArrayList<>();
    private DeviceConfigAdapter configAdapter;
    private RecyclerView configList;
    private Button read, write;

    public DeviceActivityConfig(Context context, BluetoothDevice device, E serviceBinder) {
        this.mContext = context;
        this.device = device;
        this.serviceBinder = serviceBinder;
        this.features = serviceBinder.getFeatures(device);
    }
    
    private void updateAdapter() {
        if (hlmtList == null && bkList == null) {
            return;     // modeList data might not be available yet
        }
        if (configList == null) {
            return;     // data might be here before onCreateView is called
        }

        if (features.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT)
            configAdapter = new DeviceConfigAdapter(mContext, features, groups, preferredMode, temporaryMode, hlmtList);
        else if (features.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT)
            configAdapter = new DeviceConfigAdapter(mContext, features, groups, preferredMode, temporaryMode, bkList);
        configList.setAdapter(configAdapter);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String address = intent.getStringExtra(LightControlService.EXTRA_DEVICE_ADDRESS);
            if (address != null && !address.equals(device.getAddress())) {
                return;
            }
            final String action = intent.getAction();
            switch (action) {
                case LightControlService.BROADCAST_CONTROLPOINT_GROUPS:
                    groups = intent.getIntExtra(LightControlService.EXTRA_GROUPS, 0);
                    updateAdapter();
                    break;
                case LightControlService.BROADCAST_CONTROLPOINT_MODE_CNT:
                    numberOfModes = intent.getIntExtra(LightControlService.EXTRA_MODE_CNT, 0);
                    updateAdapter();
                    break;
                case LightControlService.BROADCAST_CONTROLPOINT_MODE_CFG:
                    if (features.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT)
                        hlmtList = intent.getParcelableArrayListExtra(LightControlService.EXTRA_MODE_HELMET_LIST);
                    else if (features.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT)
                        bkList = intent.getParcelableArrayListExtra(LightControlService.EXTRA_MODE_BIKE_LIST);
                    updateAdapter();
                    break;
                case LightControlService.BROADCAST_CONTROLPOINT_PREFERRED_MODE:
                    int prefMode = intent.getIntExtra(LightControlService.EXTRA_PREFERRED_MODE, 255);
                    preferredMode = prefMode >= numberOfModes ? 0 : prefMode + 1;
                    updateAdapter();
                    break;
                case LightControlService.BROADCAST_CONTROLPOINT_TEMPORARY_MODE:
                    int tempMode = intent.getIntExtra(LightControlService.EXTRA_TEMPORARY_MODE, 255);
                    temporaryMode = tempMode >= numberOfModes ? 0 : tempMode + 1;
                    updateAdapter();
                    break;
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_GROUPS);
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_MODE_CNT);
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_MODE_CFG);
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_PREFERRED_MODE);
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_TEMPORARY_MODE);
        return filter;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        groups = 0;
        numberOfModes = 0;
        preferredMode = 0;
        temporaryMode = 0;
        hlmtList = null;
        bkList = null;
        configAdapter = null;

        LocalBroadcastManager.getInstance(mContext).registerReceiver(broadcastReceiver, makeIntentFilter());

        readConfig();
    }

    private void readConfig() {
        if (serviceBinder == null) {
            return;
        }
        serviceBinder.requestModes(device);
        serviceBinder.requestModeCnt(device); // not necessary, will be requested automatically with request modes
        serviceBinder.requestGroups(device);
        serviceBinder.requestPreferredMode(device);
        serviceBinder.requestTemporaryMode(device);
    }

    private void writeConfig() {
        if (serviceBinder == null || configAdapter == null) {
            return;
        }

        int prefMode = configAdapter.getPreferredMode();
        if (prefMode != preferredMode) {
            serviceBinder.setPreferredMode(device, (byte) (prefMode == 0 ? 255 : prefMode - 1));
            preferredMode = prefMode;
        }

        int tempMode = configAdapter.getTemporaryMode();
        if (tempMode != temporaryMode) {
            serviceBinder.setTemporaryMode(device, (byte) (tempMode == 0 ? 255 : tempMode - 1));
            temporaryMode = tempMode;
        }

        int group = configAdapter.getGroups();
        if (group != groups) {
            serviceBinder.setGroups(device, (byte)configAdapter.getGroups());
            groups = group;
        }

        if (features.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT) {
            List<LightControlHelmetSetup> list = configAdapter.getModeListHelmet();
            if (!list.equals(hlmtList)) {
                serviceBinder.setModes(device, list);
                hlmtList = new ArrayList<>();
                for (LightControlHelmetSetup src : list) {
                    LightControlHelmetSetup dst = new LightControlHelmetSetup();
                    src.copy(dst);
                    hlmtList.add(dst);
                }
            }
        }
        else if (features.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT) {
            List<LightControlBikeSetup> list = configAdapter.getModeListBike();
            if (!list.equals(bkList)) {
                serviceBinder.setModes(device, list);
                bkList = new ArrayList<>();
                for (LightControlBikeSetup src : list) {
                    LightControlBikeSetup dst = new LightControlBikeSetup();
                    src.copy(dst);
                    bkList.add(dst);
                }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        View root = inflator.inflate(R.layout.fragment_device_config, container, false);

        read = root.findViewById(R.id.read);
        read.setOnClickListener(v -> readConfig());
        write = root.findViewById(R.id.write);
        write.setOnClickListener(v -> writeConfig());
        configList = root.findViewById(R.id.configList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        configList.setLayoutManager(layoutManager);
        ((SimpleItemAnimator) configList.getItemAnimator()).setSupportsChangeAnimations(false);
        if (configAdapter != null) {    // check if adapter is already set
            configList.setAdapter(configAdapter);
        }

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        /*getActivity().unbindService(serviceConnection);
        serviceBinder = null;*/

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(broadcastReceiver);
    }
}

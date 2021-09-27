package com.reisi.lightcontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.reisi.lightcontrol.profile.LightControlFeatureDataCallback;
import com.reisi.lightcontrol.profile.LightControlManager;
import com.reisi.lightcontrol.profile.LightControlService;
import com.reisi.lightcontrol.profile.NordicLegacyDfuManager;
import com.reisi.lightcontrol.ui.device.DeviceActivity;

import java.util.List;
import java.util.UUID;

import static com.reisi.lightcontrol.profile.NordicLegacyDfuManager.getDfuServiceUuid;

public class MainActivity<E extends LightControlService.LCSBinder> extends AppCompatActivity {
    public static final String EXTRA_DEVICE_ADDRESS = "com.reisi.lightcontrol.EXTRA_DEVICE_ADDRESS";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 1022; // random number

    private BluetoothManager btManger;
    private BluetoothAdapter btAdapter;
    private Button searchButton;
    private RecyclerView deviceList;

    private boolean isBound;
    private E serviceBinder;

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (serviceBinder == null) {
                return;
            }

            runOnUiThread(() -> serviceBinder.addDevice(device));
        }
    };

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION && grantResults.equals(android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            if (isBleEnabled())
                searchButton.setEnabled(true);
        }
    }

    private void requestNavigation() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_ACCESS_FINE_LOCATION);
    }

    private boolean isNavigationGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_OK) {      // bluetooth is enabled, check if navigation is granted 
                if (isNavigationGranted())
                    searchButton.setEnabled(true);
                else
                    requestNavigation();
            }
            else
                searchButton.setEnabled(false);
        }
    }
    
    private void requestBle() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private boolean isBleEnabled() {
        return btAdapter != null && btAdapter.isEnabled();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBinder = (E)service;
            if (serviceBinder == null)
                isBound = false;
            // add already connected devices, devices without LCS will be ignored
            //List<BluetoothDevice> devices = btManger.getConnectedDevices(BluetoothProfile.GATT);
            int[] state = {BluetoothAdapter.STATE_CONNECTING, BluetoothAdapter.STATE_CONNECTED};
            List<BluetoothDevice> devices = btManger.getDevicesMatchingConnectionStates(BluetoothProfile.GATT, state);
            for (BluetoothDevice device : devices) {
                serviceBinder.addDevice(device);
            }

            updateAdapter();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBinder = null;
            isBound = false;
        }
    };

    private boolean isDeviceSupported(BluetoothDevice device) {
        // check if device is supported
        LightControlFeatureDataCallback features = serviceBinder.getFeatures(device);
        return features.lightType != LightControlFeatureDataCallback.LC_LF_LT_UNKNOWN;
    }

    private void updateAdapter() {
        if (serviceBinder == null) {
            return;
        }
        List<BluetoothDevice> bleDevices = serviceBinder.getDevices();
        DeviceListAdapter dlAdapter = new DeviceListAdapter(this, bleDevices, serviceBinder);
        dlAdapter.setOnItemClickListener(new DeviceListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BluetoothDevice device) {
                if (serviceBinder.getState(device) == LightControlManager.STATE_DISCONNECTED ||
                        serviceBinder.getState(device) == LightControlManager.STATE_UNKNOWN) {
                    String message = getResources().getString(R.string.not_connected);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                }
                else if (!isDeviceSupported(device)) {
                    String message = getResources().getString(R.string.fw_unsupported);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent intent = new Intent(getApplicationContext(), DeviceActivity.class);
                    intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
                    startActivity(intent);
                }
            }

            @Override
            public void onItemSwipe(BluetoothDevice device) {
                serviceBinder.removeDevice(device);
            }
        });
        dlAdapter.getItemTouchHelper().attachToRecyclerView(deviceList);
        deviceList.setAdapter(dlAdapter);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (LightControlService.BROADCAST_DEVICELIST_CHANGED.equals(action)) {
                updateAdapter();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //android.os.Debug.waitForDebugger();

        searchButton = findViewById(R.id.button);
        searchButton.setOnClickListener(v -> {
            // start or stop searching for Light Control service devices when button is clicked
            UUID[] uuid = {LightControlManager.getLightControlServiceUuid()/*, NordicLegacyDfuManager.getDfuServiceUuid()*/};
            if (searchButton.getText().equals(getResources().getString(R.string.main_search))) {
                btAdapter.startLeScan(uuid, leScanCallback);
                searchButton.setText(getResources().getString(R.string.main_searching));
            } else if (searchButton.getText().equals(getResources().getString(R.string.main_searching))) {
                btAdapter.stopLeScan(leScanCallback);
                searchButton.setText(getResources().getString(R.string.main_search));
            }
        });

        deviceList = findViewById(R.id.deviceList);
        deviceList.setLayoutManager(new LinearLayoutManager(this));

        btManger = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManger.getAdapter();

        if (!isBleEnabled()) {
            searchButton.setEnabled(false); // deactivate search button if bluetooth is of
            requestBle();                   // and request bluetooth
        }
        else if (!isNavigationGranted()) {
            searchButton.setEnabled(false); // deactivate search button if navigation rights are not granted
            requestNavigation();
        }
        else
            searchButton.setEnabled(true);

        final Intent service = new Intent(this, LightControlService.class);
        startService(service);
        isBound = bindService(service, serviceConnection, BIND_AUTO_CREATE | BIND_ABOVE_CLIENT);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(LightControlService.BROADCAST_DEVICELIST_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(serviceConnection);
        stopService(new Intent(this, LightControlService.class));
        serviceBinder = null;
        isBound = false;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (searchButton.getText().equals(getResources().getString(R.string.main_searching))) {
            btAdapter.stopLeScan(leScanCallback);
            searchButton.setText(getResources().getString(R.string.main_search));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isBound) {
            final Intent service = new Intent(this, LightControlService.class);
            isBound = bindService(service, serviceConnection, BIND_AUTO_CREATE | BIND_ABOVE_CLIENT);
        }
        else
            updateAdapter();

        if (BuildConfig.DEBUG) { // don't even consider it otherwise
            if (Debug.isDebuggerConnected()) {
                Log.d("SCREEN", "Keeping screen on for debugging, detach debugger and force an onResume to turn it off.");
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Log.d("SCREEN", "Keeping screen on for debugging is now deactivated.");
            }
        }
    }
}

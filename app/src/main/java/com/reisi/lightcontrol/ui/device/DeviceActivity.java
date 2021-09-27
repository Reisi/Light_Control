package com.reisi.lightcontrol.ui.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.IBinder;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.reisi.lightcontrol.MainActivity;
import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlService;

public class DeviceActivity<E extends LightControlService.LCSBinder> extends AppCompatActivity {
    private E serviceBinder;
    private boolean isBound;
    private BluetoothDevice device;

    private int getMipmapResId(String name) {
        String pkgName = getPackageName();
        return getResources().getIdentifier(name, "mipmap", pkgName);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBinder = (E)service;

            SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getApplicationContext(), getSupportFragmentManager(), device, serviceBinder);
            ViewPager viewPager = findViewById(R.id.view_pager);
            viewPager.setAdapter(sectionsPagerAdapter);
            TabLayout tabs = findViewById(R.id.tabs);
            tabs.setupWithViewPager(viewPager);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBinder = null;
            isBound = false;
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case LightControlService.BROADCAST_DEVICELIST_CHANGED:
                    boolean state = intent.getBooleanExtra(LightControlService.EXTRA_CONNECTION_STATE, false);
                    String address = intent.getStringExtra(LightControlService.EXTRA_DEVICE_ADDRESS);
                    if (!state && address != null && address.equals(device.getAddress())) {
                        finish();
                    }
                    break;
                case LightControlService.BROADCAST_CONTROLPOINT_COMMON:
                    final String[] opcodeString = getResources().getStringArray(R.array.op_code);
                    final String[] responseString = getResources().getStringArray(R.array.response_value);//{"successfull", "not supported", "invalid param", "failed"};
                    int response = intent.getIntExtra(LightControlService.EXTRA_RESPONSE, 0);
                    int opcode = intent.getIntExtra(LightControlService.EXTRA_OPCODE, 0);
                    if (opcode == 0 || response == 0) {
                        break;
                    }
                    if (opcode >= opcodeString.length || opcode < 0)
                        opcode = opcodeString.length;
                    if (response >= responseString.length || response < 0)
                        response = responseString.length;
                    String message = opcodeString[opcode - 1] + " " + responseString[response - 1];
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        Intent intent = getIntent();
        final String deviceAddress = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothManager btManger = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManger.getAdapter();
        device = btAdapter.getRemoteDevice(deviceAddress);

        TextView title = findViewById(R.id.title);
        title.setText(device.getName());
        if (device.getName().equals("Helena") || device.getName().equals("Billina")) {
            ImageView logo = findViewById(R.id.logo);
            logo.setImageResource(getMipmapResId("helena"));
        }

        final Intent service = new Intent(this, LightControlService.class);
        bindService(service, serviceConnection, 0);
        isBound = true;

        final IntentFilter filter = new IntentFilter();
        filter.addAction(LightControlService.BROADCAST_DEVICELIST_CHANGED);
        filter.addAction(LightControlService.BROADCAST_CONTROLPOINT_COMMON);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isBound)
            finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(serviceConnection);
        serviceBinder = null;
        isBound = false;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }
}
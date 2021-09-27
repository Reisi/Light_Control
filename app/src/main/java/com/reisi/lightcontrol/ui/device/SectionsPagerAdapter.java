package com.reisi.lightcontrol.ui.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlManager;
import com.reisi.lightcontrol.profile.LightControlService;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter<E extends LightControlService.LCSBinder> extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.tab_text_1, R.string.tab_text_2, R.string.tab_text_3, R.string.tab_text_4};
    private final Context mContext;
    private final BluetoothDevice device;
    private final E serviceBinder;

    public SectionsPagerAdapter(Context context, FragmentManager fm, BluetoothDevice device, E serviceBinder) {
        super(fm);
        this.mContext = context;
        this.device = device;
        this.serviceBinder = serviceBinder;
    }

    @Override
    public Fragment getItem(int position) {
        if (position > 0 && serviceBinder.getState(device) == LightControlManager.STATE_READ_ONLY)
            position += 2;
        if (position == 3 && !serviceBinder.hasUart(device))
            position++;

        switch (position) {
            case 0:
                return new DeviceActivityStatus(mContext, device, serviceBinder);

            case 1:
                return new DeviceActivityConfig(mContext, device, serviceBinder);

            case 2:
                return new DeviceActivitySetup(mContext, device, serviceBinder);

            case 3:
                return new DeviceActivityDebug(mContext, device, serviceBinder);

            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (position > 0 && serviceBinder.getState(device) == LightControlManager.STATE_READ_ONLY)
            position += 2;
        if (position == 3 && !serviceBinder.hasUart(device))
            return null;
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        int cnt = 1;    // status is always availabe
        if (serviceBinder.getState(device) == LightControlManager.STATE_READ_WRITE)
            cnt += 2;
        if (serviceBinder.hasUart(device))
            cnt += 1;
        return cnt;
    }
}
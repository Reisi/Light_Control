package com.reisi.lightcontrol;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.reisi.lightcontrol.profile.LightControlManager;
import com.reisi.lightcontrol.profile.LightControlService;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeviceListAdapter<E extends LightControlService.LCSBinder> extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private List<BluetoothDevice> devices;
    private OnItemClickListener listener;
    private final ItemTouchHelper itemTouchHelper;
    private final Context mContext;
    private final E binder;

    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
        void onItemSwipe(BluetoothDevice device);
    }

    public DeviceListAdapter(Context context, List<BluetoothDevice> deviceList, E serviceBinder) {
        this.mContext = context;
        this.binder = serviceBinder;
        this.devices = deviceList;

        this.itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
    }

    public ItemTouchHelper getItemTouchHelper() {
        return itemTouchHelper;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View modeCfg = layoutInflater.inflate(R.layout.main_device_list, parent, false);

        return new ViewHolder(modeCfg);
    }

    private int getMipmapResId(String name) {
        String pkgName = mContext.getPackageName();
        return mContext.getResources().getIdentifier(name, "mipmap", pkgName);
    }

    @Override
    //public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    public void onBindViewHolder(@NonNull @NotNull DeviceListAdapter.ViewHolder holder, int position) {
        final BluetoothDevice device = this.devices.get(position);
        if (device == null)
            return;
        holder.name.setText(device.getName());
        int state = binder.getState(device);
        if (state == LightControlManager.STATE_READ_WRITE)
            holder.name.setTextColor(mContext.getResources().getColor(R.color.colorAccent));
        else if (state == LightControlManager.STATE_READ_ONLY)
            holder.name.setTextColor(mContext.getResources().getColor(R.color.colorSelected));
        else if (state == LightControlManager.STATE_DISCONNECTED)
            holder.name.setTextColor(mContext.getResources().getColor(R.color.colorUnselected));
        holder.address.setText(device.getAddress());
        if (device.getName().equals("Helena") || device.getName().equals("Billina")) {
            holder.logo.setImageResource(getMipmapResId("helena"));
        }
        else {
            //holder.logo.setImageResource(getMipmapResId("light_control"));
            holder.logo.setImageResource(getMipmapResId("ic_launcher"));
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView name, address;
        public ImageView logo;

        public ViewHolder(View itemView) {
            super(itemView);
            this.name = itemView.findViewById(R.id.deviceName);
            this.address = itemView.findViewById(R.id.deviceAddress);
            this.logo = itemView.findViewById(R.id.deviceLogo);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            if (listener != null && position < devices.size()) {
                final BluetoothDevice device = devices.get(getAdapterPosition());
                listener.onItemClick(device);
            }
        }
    }

    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, @NonNull @NotNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (listener != null && position < devices.size()) {
                final BluetoothDevice device = devices.get(viewHolder.getAdapterPosition());
                listener.onItemSwipe(device);
            }
        }
    };
}

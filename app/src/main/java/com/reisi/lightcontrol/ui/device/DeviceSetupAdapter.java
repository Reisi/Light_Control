package com.reisi.lightcontrol.ui.device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlFeatureDataCallback;

public class DeviceSetupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_OFFSET = 0;
    private static final int TYPE_LIMIT = 1;
    private static final int TYPE_LEDCNT = 2;
    private static final int TYPE_INVALID = 3;

    private final Context mContext;
    private OnItemClickListener clickListener;
    LightControlFeatureDataCallback features;
    private final int[] accelOffset = {0, 0, 0};
    private final int[] driverLimit = {0, 0};
    private final int[] ledCnt = {-1, -1};

    public interface OnItemClickListener {
        void onOffsetCalibrateClick();
        void onLimitReadClick();
        void onLimitUpdateClick(int[] limit);
        void onLedConfigurationCheckClick();
    }

    public DeviceSetupAdapter(Context context, LightControlFeatureDataCallback features) {
        this.mContext = context;
        this.features = features;
    }

    public DeviceSetupAdapter(Context context, LightControlFeatureDataCallback features, int[] offset, int[] limit, int[] ledCnt) {
        this.mContext = context;
        this.features = features;
        this.accelOffset[0] = offset[0];
        this.accelOffset[1] = offset[1];
        this.accelOffset[2] = offset[2];
        this.driverLimit[0] = limit[0];
        this.driverLimit[1] = limit[1];
        this.ledCnt[0] = ledCnt[0];
        this.ledCnt[1] = ledCnt[1];
    }

    public void setOffset(int[] offset) {
        accelOffset[0] = offset[0];
        accelOffset[1] = offset[1];
        accelOffset[2] = offset[2];
        int position = getPositionOfItem(TYPE_OFFSET);
        if (position < 0)
            return;
        this.notifyItemChanged(position);
    }

    public void setLimit(int[] limit) {
        driverLimit[0] = limit[0];
        driverLimit[1] = limit[1];
        int position = getPositionOfItem(TYPE_LIMIT);
        if (position < 0)
            return;
        this.notifyItemChanged(position);
    }

    public void setLedCnt(int[] ledCnt) {
        this.ledCnt[0] = ledCnt[0];
        this.ledCnt[1] = ledCnt[1];
        int position = getPositionOfItem(TYPE_LEDCNT);
        if (position < 0)
            return;
        this.notifyItemChanged(position);
    }

    public void setClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    private int getPositionOfItem(int viewType) {
        int position = 0;
        if (features.setupFeatures.sensorOffsetSupported) {
            if (viewType == TYPE_OFFSET)
                return position;
            else
                position++;
        }
        if (features.setupFeatures.currentLimitSupported) {
            if (viewType == TYPE_LIMIT)
                return position;
            else
                position++;
        }
        if (features.setupFeatures.ledConfigurationCheckSupported) {
            if (viewType == TYPE_LEDCNT)
                return position;
            //else
            //    position++;
        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View modeCfg;

        switch (viewType) {
            case TYPE_OFFSET:
            case TYPE_LEDCNT:
                modeCfg = layoutInflater.inflate(R.layout.device_setup_item, parent, false);
                return new ConfigViewHolder(modeCfg);
            case TYPE_LIMIT:
                modeCfg = layoutInflater.inflate(R.layout.device_setup_limit_item, parent, false);
                return new ConfigLimitViewHolder(modeCfg);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_OFFSET:
                ConfigViewHolder offsetHolder = (ConfigViewHolder)holder;
                offsetHolder.heading.setText(mContext.getResources().getString(R.string.dev_setup_offset));
                if (accelOffset[0] == 0 && accelOffset[1] == 0 && accelOffset[2] == 0) {
                    offsetHolder.data.setText(mContext.getResources().getString(R.string.dev_setup_offset_default));
                } else {
                    String value =  mContext.getResources().getString(R.string.dev_setup_offset_default_x) + accelOffset[0] + "\n" +
                            mContext.getResources().getString(R.string.dev_setup_offset_default_y) + accelOffset[1] + "\n" +
                            mContext.getResources().getString(R.string.dev_setup_offset_default_z) + accelOffset[2];
                    offsetHolder.data.setText(value);
                }
                offsetHolder.left.setText(mContext.getResources().getString(R.string.dev_setup_offset_calib));
                offsetHolder.left.setOnClickListener(v -> {
                    if (clickListener != null)
                        clickListener.onOffsetCalibrateClick();
                });
                offsetHolder.right.setVisibility(View.GONE);
                break;
            case TYPE_LIMIT:
                ConfigLimitViewHolder limitHolder = (ConfigLimitViewHolder)holder;

                limitHolder.heading.setText(mContext.getResources().getString(R.string.dev_setup_limit));

                String value1 = "", value2 = "";
                if (features.lightType == LightControlFeatureDataCallback.LC_LF_LT_HELMET_LIGHT) {
                    value1 = mContext.getResources().getString(R.string.dev_setup_limit_flood);
                    value2 = mContext.getResources().getString(R.string.dev_setup_limit_spot);
                }
                else if (features.lightType == LightControlFeatureDataCallback.LC_LF_LT_BIKE_LIGHT) {
                    value1 = mContext.getResources().getString(R.string.dev_setup_limit_mainbeam);
                    value2 = mContext.getResources().getString(R.string.dev_setup_limit_highbeam);
                }
                value1 = value1.concat("\n" + driverLimit[0] + "%");
                value2 = value2.concat("\n" + driverLimit[1] + "%");
                limitHolder.text1.setText(value1);
                limitHolder.text2.setText(value2);

                limitHolder.seekbar1.setProgress((int)((driverLimit[0] + 2.5) / 5));
                limitHolder.seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (!fromUser)
                            return;
                        driverLimit[0] = progress * 5;
                        String newValue = "";
                        if (features.lightType == LightControlFeatureDataCallback.LC_LF_LT_HELMET_LIGHT)
                            newValue = mContext.getResources().getString(R.string.dev_setup_limit_flood);
                        else if (features.lightType == LightControlFeatureDataCallback.LC_LF_LT_BIKE_LIGHT)
                            newValue = mContext.getResources().getString(R.string.dev_setup_limit_mainbeam);
                        newValue = newValue.concat("\n" + driverLimit[0] + "%");
                        limitHolder.text1.setText(newValue);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                limitHolder.seekbar2.setProgress((int)((driverLimit[1] + 2.5) / 5));
                limitHolder.seekbar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (!fromUser)
                            return;
                        driverLimit[1] = progress * 5;
                        String newValue = "";
                        if (features.lightType == LightControlFeatureDataCallback.LC_LF_LT_HELMET_LIGHT)
                            newValue = mContext.getResources().getString(R.string.dev_setup_limit_spot);
                        else if (features.lightType == LightControlFeatureDataCallback.LC_LF_LT_BIKE_LIGHT)
                            newValue = mContext.getResources().getString(R.string.dev_setup_limit_highbeam);
                        newValue = newValue.concat("\n" + driverLimit[1] + "%");
                        limitHolder.text2.setText(newValue);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                limitHolder.read.setText(mContext.getResources().getString(R.string.dev_setup_limit_read));
                limitHolder.read.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onLimitReadClick();
                    }
                });
                limitHolder.write.setText(mContext.getResources().getString(R.string.dev_setup_limit_update));
                limitHolder.write.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onLimitUpdateClick(driverLimit);
                    }
                });
                break;
            case TYPE_LEDCNT:
                ConfigViewHolder ledCntHolder = (ConfigViewHolder)holder;
                ledCntHolder.heading.setText(mContext.getResources().getString(R.string.dev_setup_ledcnt));
                String value;
                if (features.lightType == LightControlFeatureDataCallback.LC_LF_LT_HELMET_LIGHT) {
                    value = mContext.getResources().getString(R.string.dev_setup_ledcnt_flood) +
                            (ledCnt[0] < 0 ? mContext.getResources().getString(R.string.dev_setup_ledcnt_default) : ledCnt[0]) + "\n" +
                            mContext.getResources().getString(R.string.dev_setup_ledcnt_spot) +
                            (ledCnt[1] < 0 ? mContext.getResources().getString(R.string.dev_setup_ledcnt_default) : ledCnt[1]);
                }
                else if (features.lightType == LightControlFeatureDataCallback.LC_LF_LT_BIKE_LIGHT) {
                    value = mContext.getResources().getString(R.string.dev_setup_ledcnt_mainbeam) +
                            (ledCnt[0] < 0 ? mContext.getResources().getString(R.string.dev_setup_ledcnt_default) : ledCnt[0]) + "\n" +
                            mContext.getResources().getString(R.string.dev_setup_ledcnt_highbeam) +
                            (ledCnt[1] < 0 ? mContext.getResources().getString(R.string.dev_setup_ledcnt_default) : ledCnt[1]);
                }
                else
                    value = mContext.getResources().getString(R.string.dev_setup_ledcnt_default);
                ledCntHolder.data.setText(value);
                ledCntHolder.left.setText(mContext.getResources().getString(R.string.dev_setup_ledcnt_check));
                ledCntHolder.left.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onLedConfigurationCheckClick();
                    }
                });
                ledCntHolder.right.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        int itemCount = 0;
        itemCount += features.setupFeatures.sensorOffsetSupported ? 1 : 0;
        itemCount += features.setupFeatures.currentLimitSupported ? 1 : 0;
        itemCount += features.setupFeatures.ledConfigurationCheckSupported ? 1 : 0;
        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        // offset is first
        if (!features.setupFeatures.sensorOffsetSupported)
            position++;
        else if (position == TYPE_OFFSET)
            return TYPE_OFFSET;
        // current limit second
        if (!features.setupFeatures.currentLimitSupported)
            position++;
        else if (position == TYPE_LIMIT)
            return TYPE_LIMIT;
        // led count is third
        if (!features.setupFeatures.ledConfigurationCheckSupported)
            position++;
        else if (position == TYPE_LEDCNT)
            return TYPE_LEDCNT;

        return TYPE_INVALID;
    }

    /*public class OffsetViewHolder extends RecyclerView.ViewHolder {
        TextView data;
        Button read, calib;

        OffsetViewHolder(View itemView) {
            super(itemView);
            this.data = (TextView)itemView.findViewById(R.id.data);
            this.read = (Button)itemView.findViewById(R.id.read);
            this.calib = (Button) itemView.findViewById(R.id.calib);
        }
    }*/

    public static class ConfigViewHolder extends RecyclerView.ViewHolder {
        TextView heading, data;
        Button left, right;

        ConfigViewHolder(View itemView) {
            super(itemView);
            this.heading = itemView.findViewById(R.id.heading);
            this.data = itemView.findViewById(R.id.data);
            this.left = itemView.findViewById(R.id.leftButton);
            this.right = itemView.findViewById(R.id.rightButton);
        }
    }

    public static class ConfigLimitViewHolder extends RecyclerView.ViewHolder {
        TextView heading, text1, text2;
        SeekBar seekbar1, seekbar2;
        Button read, write;

        ConfigLimitViewHolder(View itemView) {
            super(itemView);
            this.heading = itemView.findViewById(R.id.heading);
            this.text1 = itemView.findViewById(R.id.text1);
            this.text2 = itemView.findViewById(R.id.text2);
            this.seekbar1 = itemView.findViewById(R.id.seekbar1);
            this.seekbar2 = itemView.findViewById(R.id.seekbar2);
            this.read = itemView.findViewById(R.id.read);
            this.write = itemView.findViewById(R.id.write);
        }
    }
}


package com.reisi.lightcontrol.ui.device;

import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlBikeSetup;
import com.reisi.lightcontrol.profile.LightControlFeatureCallback;
import com.reisi.lightcontrol.profile.LightControlFeatureDataCallback;
import com.reisi.lightcontrol.profile.LightControlHelmetSetup;

import java.util.ArrayList;
import java.util.List;

public class DeviceStatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_MODELIST = 0;
    public static final int TYPE_SETUP = 1;
    public static final int TYPE_DATA = 2;
    public static final int TYPE_INVALID = 3;

    public static class DataItemInfo {
        public String header, unit, value;
    }

    private final Context mContext;
    private OnItemClickListener listener;
    LightControlFeatureDataCallback features;
    private List<LightControlHelmetSetup> modeListHelmet;
    private LightControlHelmetSetup setupItemHelmet;
    private List<LightControlBikeSetup> modeListBike;
    private LightControlBikeSetup setupItemBike;
    private final List<DataItemInfo> dataItemList = new ArrayList<>();

    public interface OnItemClickListener {
        void onModeClick(int modeNo);
    }

    public DeviceStatusAdapter(Context context, LightControlFeatureDataCallback features) {
        this.mContext = context;
        this.features = features;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void updateModeListHelmet(List<LightControlHelmetSetup> modes) {
        this.modeListBike = null;
        this.modeListHelmet = new ArrayList<>();
        for (LightControlHelmetSetup src : modes) {
            LightControlHelmetSetup dst = new LightControlHelmetSetup();
            src.copy(dst);
            this.modeListHelmet.add(dst);
        }
        this.notifyDataSetChanged();
    }

    public void setSetupValue(LightControlHelmetSetup setup) {
        boolean dataSetChanged = setupItemHelmet == null ^ setup == null;

        this.setupItemBike = null;
        this.setupItemHelmet = setup;

        if (dataSetChanged) {
            this.notifyDataSetChanged();            // setup has been added or removed
        } else {
            this.notifyItemChanged((modeListHelmet == null && modeListBike == null) ? 0 : 1);// setup has just been updated
        }
    }

    public void updateModeListBike(List<LightControlBikeSetup> modes) {
        this.modeListHelmet = null;
        this.modeListBike = new ArrayList<>();
        for (LightControlBikeSetup src : modes) {
            LightControlBikeSetup dst = new LightControlBikeSetup();
            src.copy(dst);
            this.modeListBike.add(dst);
        }
        this.notifyDataSetChanged();
    }

    public void setSetupValue(LightControlBikeSetup setup) {
        boolean dataSetChanged = setupItemHelmet == null ^ setup == null;

        this.setupItemHelmet = null;
        this.setupItemBike = setup;

        if (dataSetChanged) {
            this.notifyDataSetChanged();            // setup has been added or removed
        } else {
            this.notifyItemChanged((modeListHelmet == null && modeListBike == null) ? 0 : 1);// setup has just been updated
        }
    }

    public int addDataItem(DataItemInfo item) {
        dataItemList.add(item);
        return dataItemList.size() - 1;
    }

    public void setDataValue(String value, int position) {
        DataItemInfo item = dataItemList.get(position);
        if (item == null) {
            return;
        }

        item.value = value;

        this.notifyItemChanged((modeListHelmet != null || modeListBike != null ? 1 : 0) +
                                        (setupItemHelmet != null || setupItemBike != null ? 1 : 0) +
                                        position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View modeCfg;

        switch (viewType) {
            case TYPE_MODELIST:
                modeCfg = layoutInflater.inflate(R.layout.device_modelist_item, parent, false);
                return new ModeListViewHolder(modeCfg);
            case TYPE_SETUP:
                modeCfg = layoutInflater.inflate(R.layout.device_modesetup_item, parent, false);
                return new ModeSetupViewHolder(modeCfg);
            case TYPE_DATA:
                modeCfg = layoutInflater.inflate(R.layout.device_status_data_item, parent, false);
                return new DataViewHolder(modeCfg);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        int pos = position;

        if (modeListHelmet == null && modeListBike == null)
            pos++;
        else if (pos == TYPE_MODELIST)
            return TYPE_MODELIST;

        if (setupItemHelmet == null && setupItemBike == null)
            pos++;
        else if (pos == TYPE_SETUP)
            return TYPE_SETUP;

        if (position - pos <= dataItemList.size())
            return TYPE_DATA;
        else
            return TYPE_INVALID;
    }

    private int getMipmapResId(String type, boolean state) {
        String pkgName = mContext.getPackageName();
        String name = type + (state ? "_active" : "_inactive");
        return mContext.getResources().getIdentifier(name, "mipmap", pkgName);
    }

    private int getMipmapResId(String type) {
        String pkgName = mContext.getPackageName();
        return mContext.getResources().getIdentifier(type, "mipmap", pkgName);
    }

    private static int convertDpToPixel(int dp, Context context){
        return (dp * context.getResources().getDisplayMetrics().densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_MODELIST:
                ModeListViewHolder modeListHolder = (ModeListViewHolder)holder;
                modeListHolder.heading.setText(mContext.getResources().getString(R.string.dev_status_modelist_heading));
                modeListHolder.background.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
                for (int i = 0; i < modeListHolder.modes.size(); i++) {
                    Button modeButton = modeListHolder.modes.get(i);
                    modeButton.setPadding(0, convertDpToPixel(11, mContext), 0, 0);
                    modeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 6);
                    modeButton.setTextColor(mContext.getResources().getColor(R.color.colorSelected));
                    final int mode = i;
                    modeButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onModeClick(mode);
                        }
                    });
                    if (features.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT) {
                        if (i >= modeListHelmet.size())
                            modeButton.setVisibility(View.GONE);
                        else {
                            LightControlHelmetSetup setup = modeListHelmet.get(i);
                            if (setup.floodActive && setup.spotActive) {
                                String value = setup.intensity + " " +
                                        (setup.pitchComensation ?
                                                mContext.getResources().getString(R.string.dev_unit_lux) :
                                                mContext.getResources().getString(R.string.dev_unit_percent));
                                modeButton.setBackgroundResource(getMipmapResId("ml_hlmt_flood_spot"));
                                modeButton.setText(value);
                            }
                            else if (setup.floodActive) {
                                String value = setup.intensity + " " +
                                        (setup.pitchComensation ?
                                                mContext.getResources().getString(R.string.dev_unit_lux) :
                                                mContext.getResources().getString(R.string.dev_unit_percent));
                                modeButton.setBackgroundResource(getMipmapResId("ml_hlmt_flood"));
                                modeButton.setText(value);
                            }
                            else if (setup.spotActive) {
                                String value = setup.intensity + " " +
                                        (setup.pitchComensation ?
                                                mContext.getResources().getString(R.string.dev_unit_lux) :
                                                mContext.getResources().getString(R.string.dev_unit_percent));
                                modeButton.setBackgroundResource(getMipmapResId("ml_hlmt_spot"));
                                modeButton.setText(value);
                            }
                            else {
                                String value = "--";
                                modeButton.setBackgroundResource(getMipmapResId("ml_hlmt_off"));
                                modeButton.setText(value);
                            }
                        }
                    }
                    else if (features.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT) {
                        if (i >= modeListBike.size())
                            modeButton.setVisibility(View.GONE);
                        else {
                            LightControlBikeSetup setup = modeListBike.get(i);
                            if ((setup.mainBeamActive || setup.extendedMainBeamActive) && setup.highBeamActive) {
                                String value = setup.mainBeamIntensity +  "/" + setup.highBeamIntensity +
                                        mContext.getResources().getString(R.string.dev_unit_percent);
                                modeButton.setBackgroundResource(getMipmapResId("ml_bk_mbhb"));
                                modeButton.setText(value);
                            }
                            else if ((setup.mainBeamActive || setup.extendedMainBeamActive)) {
                                String value = setup.mainBeamIntensity +  "/--" +
                                        mContext.getResources().getString(R.string.dev_unit_percent);
                                modeButton.setBackgroundResource(getMipmapResId("ml_bk_mb"));
                                modeButton.setText(value);
                            }
                            else if (setup.highBeamActive) {
                                String value = "--/" + setup.highBeamIntensity +
                                        mContext.getResources().getString(R.string.dev_unit_percent);
                                modeButton.setBackgroundResource(getMipmapResId("ml_bk_hb"));
                                modeButton.setText(value);
                            }
                            else {
                                String value = "--";
                                modeButton.setBackgroundResource(getMipmapResId("ml_bk_off"));
                                modeButton.setText(value);
                            }
                        }
                    }
                }
                break;
            case TYPE_SETUP:
                if (setupItemBike == null && setupItemHelmet == null) {
                    return;
                }
                ModeSetupViewHolder setupHolder = (ModeSetupViewHolder)holder;
                setupHolder.modeNo.setText(mContext.getResources().getString(R.string.dev_status_setup_heading));
                setupHolder.background.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
                if (setupItemHelmet != null) {
                    ImageView img = setupHolder.modeSetups.get(0);
                    if (features.helmetLightFeatures.floodSupported)
                        img.setImageResource(getMipmapResId("flood", setupItemHelmet.floodActive));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(1);
                    if (features.helmetLightFeatures.spotSupported)
                        img.setImageResource(getMipmapResId("spot", setupItemHelmet.spotActive));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(2);
                    if (features.helmetLightFeatures.pitchCompensationSupported)
                        img.setImageResource(getMipmapResId("pitch", setupItemHelmet.pitchComensation));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(3);
                    if (features.helmetLightFeatures.driverCloningSupported)
                        img.setImageResource(getMipmapResId("clone", setupItemHelmet.cloned));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(4);
                    if (features.helmetLightFeatures.externalTaillightSupported)
                        img.setImageResource(getMipmapResId("tail", setupItemHelmet.taillight));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(5);
                    if (features.helmetLightFeatures.externalBrakelightSupported)
                        img.setImageResource(getMipmapResId("brake", setupItemHelmet.brakeLight));
                    else
                        img.setVisibility(View.GONE);

                    if (setupItemHelmet.intensity == 0) {
                        setupHolder.intensityValue.setText(mContext.getResources().getString(R.string.measurement_default));
                        setupHolder.unit.setText("");
                    } else {
                        setupHolder.intensityValue.setText(String.valueOf(setupItemHelmet.intensity));
                        setupHolder.unit.setText(setupItemHelmet.pitchComensation ?
                                mContext.getResources().getString(R.string.dev_unit_lux) :
                                mContext.getResources().getString(R.string.dev_unit_percent));
                    }

                    setupHolder.intensityBar1.setVisibility(View.GONE);
                    setupHolder.intensityBar2.setVisibility(View.GONE);
                }
                else if (setupItemBike != null) {
                    ImageView img = setupHolder.modeSetups.get(0);
                    if (features.bikeLightFeatures.mainBeamSupported)
                        img.setImageResource(getMipmapResId("mb", setupItemBike.mainBeamActive));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(1);
                    if (features.bikeLightFeatures.extendedMainBeamSupported)
                        img.setImageResource(getMipmapResId("flood", setupItemBike.extendedMainBeamActive));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(2);
                    if (features.bikeLightFeatures.highBeamCompensationSupported)
                        img.setImageResource(getMipmapResId("spot", setupItemBike.highBeamActive));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(3);
                    if (features.bikeLightFeatures.daylightSupported)
                        img.setImageResource(getMipmapResId("dl", setupItemBike.daylightActive));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(4);
                    if (features.bikeLightFeatures.externalTaillightSupported)
                        img.setImageResource(getMipmapResId("tail", setupItemBike.taillight));
                    else
                        img.setVisibility(View.GONE);

                    img = setupHolder.modeSetups.get(5);
                    if (features.bikeLightFeatures.externalBrakelightSupported)
                        img.setImageResource(getMipmapResId("brake", setupItemBike.brakeLight));
                    else
                        img.setVisibility(View.GONE);

                    if (setupItemBike.mainBeamIntensity != 0 && setupItemBike.highBeamIntensity != 0) {
                        setupHolder.intensityValue.setText(setupItemBike.mainBeamIntensity + "/" + setupItemBike.highBeamIntensity);
                        setupHolder.unit.setText(mContext.getResources().getString(R.string.dev_unit_percent));
                    }
                    else if (setupItemBike.mainBeamIntensity != 0) {
                        setupHolder.intensityValue.setText(setupItemBike.mainBeamIntensity + "/--");
                        setupHolder.unit.setText(mContext.getResources().getString(R.string.dev_unit_percent));
                    }
                    else if (setupItemBike.highBeamIntensity != 0) {
                        setupHolder.intensityValue.setText("--/" + setupItemBike.highBeamIntensity);
                        setupHolder.unit.setText(mContext.getResources().getString(R.string.dev_unit_percent));
                    }
                    else {
                        setupHolder.intensityValue.setText(mContext.getResources().getString(R.string.measurement_default));
                        setupHolder.unit.setText("");
                    }

                    setupHolder.intensityBar1.setVisibility(View.GONE);
                    setupHolder.intensityBar2.setVisibility(View.GONE);
                }
                break;
            case TYPE_DATA:
                int pos = position - ((setupItemHelmet == null && setupItemBike == null) ? 0 : 1) - ((modeListHelmet == null && modeListBike == null) ? 0 : 1);
                if (pos < 0 || pos >= dataItemList.size()) {return;}
                DataItemInfo item = dataItemList.get(pos);
                if (item == null) {return;}
                DataViewHolder dataHolder = (DataViewHolder)holder;
                dataHolder.header.setText(item.header);
                dataHolder.unit.setText(item.unit);
                dataHolder.value.setText(item.value);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return ((modeListHelmet != null || modeListBike != null) ? 1 : 0) +
                ((setupItemHelmet != null || setupItemBike != null) ? 1 : 0) +
                dataItemList.size();
    }

    public class DataViewHolder extends RecyclerView.ViewHolder {
        public TextView header, unit, value;

        public DataViewHolder(View itemView) {
            super(itemView);
            this.header = itemView.findViewById(R.id.intensity);
            this.unit = itemView.findViewById(R.id.unit);
            this.value = itemView.findViewById(R.id.value);
            Typeface font = Typeface.createFromAsset(mContext.getAssets(), "fonts/DSEG7Classic-BoldItalic.ttf");
            this.value.setTypeface(font);
        }
    }

    public class ModeSetupViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ConstraintLayout background;
        public TextView modeNo, intensityValue, unit;
        public List<ImageView> modeSetups = new ArrayList<>();
        public SeekBar intensityBar1, intensityBar2;

        public ModeSetupViewHolder(View itemView) {
            super(itemView);
            background = itemView.findViewById(R.id.background);
            modeNo = itemView.findViewById(R.id.modesetup_heading);
            intensityValue = itemView.findViewById(R.id.intensity);
            unit = itemView.findViewById(R.id.unit);
            modeSetups.add(itemView.findViewById(R.id.modeSetup1));
            modeSetups.add(itemView.findViewById(R.id.modeSetup2));
            modeSetups.add(itemView.findViewById(R.id.modeSetup3));
            modeSetups.add(itemView.findViewById(R.id.modeSetup4));
            modeSetups.add(itemView.findViewById(R.id.modeSetup5));
            modeSetups.add(itemView.findViewById(R.id.modeSetup6));
            intensityBar1 = itemView.findViewById(R.id.intensityBar1);
            intensityBar2 = itemView.findViewById(R.id.intensityBar2);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onModeClick(-1);
            }
        }
    }

    /*public class SetupViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView intensity, unit;
        public ImageView flood, spot, pitch, clone, tail, brake;

        public SetupViewHolder(View itemView) {
            super(itemView);
            this.intensity = (TextView)itemView.findViewById(R.id.intensity);
            this.unit = (TextView)itemView.findViewById(R.id.unit);
            this.flood = (ImageView)itemView.findViewById(R.id.flood);
            this.spot = (ImageView)itemView.findViewById(R.id.spot);
            this.pitch = (ImageView)itemView.findViewById(R.id.pitch);
            this.clone = (ImageView)itemView.findViewById(R.id.clone);
            this.tail = (ImageView)itemView.findViewById(R.id.tail);
            this.brake = (ImageView)itemView.findViewById(R.id.brake);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onModeClick(-1);
            }
        }
    }*/

    public static class ModeListViewHolder extends  RecyclerView.ViewHolder {
        public ConstraintLayout background;
        public TextView heading;
        public List<Button> modes = new ArrayList<>();

        public ModeListViewHolder(View itemView) {
            super(itemView);
            background = itemView.findViewById(R.id.background);
            heading = itemView.findViewById(R.id.modelist_heading);
            modes.add(itemView.findViewById(R.id.button1));
            modes.add(itemView.findViewById(R.id.button2));
            modes.add(itemView.findViewById(R.id.button3));
            modes.add(itemView.findViewById(R.id.button4));
            modes.add(itemView.findViewById(R.id.button5));
            modes.add(itemView.findViewById(R.id.button6));
            modes.add(itemView.findViewById(R.id.button7));
            modes.add(itemView.findViewById(R.id.button8));
            modes.add(itemView.findViewById(R.id.button9));
            modes.add(itemView.findViewById(R.id.button10));
            modes.add(itemView.findViewById(R.id.button11));
            modes.add(itemView.findViewById(R.id.button12));
            modes.add(itemView.findViewById(R.id.button13));
            modes.add(itemView.findViewById(R.id.button14));
            modes.add(itemView.findViewById(R.id.button15));
            modes.add(itemView.findViewById(R.id.button16));
        }
    }

    /*public class ModeListViewHolder extends RecyclerView.ViewHolder {
        public List<Button> buttonList = new ArrayList<>();

        public ModeListViewHolder(View itemView, int numOfModes) {
            super(itemView);

            for (int i = 1; i <= numOfModes; i++) {
                String buttonId = "mode" + i;
                int resId = mContext.getResources().getIdentifier(buttonId, "id", mContext.getPackageName());
                Button modeButton = (Button)itemView.findViewById(resId);
                if (modeButton != null) {
                    buttonList.add(modeButton);
                }
            }
        }
    }*/
}

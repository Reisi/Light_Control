package com.reisi.lightcontrol.ui.device;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reisi.lightcontrol.R;
import com.reisi.lightcontrol.profile.LightControlBikeSetup;
import com.reisi.lightcontrol.profile.LightControlFeatureCallback;
import com.reisi.lightcontrol.profile.LightControlFeatureDataCallback;
import com.reisi.lightcontrol.profile.LightControlHelmetSetup;

import java.util.ArrayList;
import java.util.List;

public class DeviceConfigAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_GROUP = 0;
    private static final int TYPE_PREFERRED_MODE = 1;
    private static final int TYPE_TEMPORARY_MODE = 2;
    private static final int TYPE_MODE = 3;
    private static final int TYPE_INVALID = 4;

    private int groups, preferredMode, temporaryMode;
    LightControlFeatureDataCallback features;
    private List<LightControlHelmetSetup> modeListHelmet;
    private List<LightControlBikeSetup> modeListBike;
    private final Context mContext;

    public DeviceConfigAdapter(Context context, LightControlFeatureDataCallback features, int groups, int preferredMode, int temporaryMode, List<?> modeList) {
        this.mContext = context;
        this.groups = groups;
        this.preferredMode = preferredMode;
        this.temporaryMode = temporaryMode;
        this.features = features;
        if (features.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT) {
            this.modeListHelmet = new ArrayList<>();
            for (LightControlHelmetSetup src : (List<LightControlHelmetSetup>)modeList) {
                LightControlHelmetSetup dst = new LightControlHelmetSetup();
                src.copy(dst);
                this.modeListHelmet.add(dst);
            }
        }
        else if (features.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT) {
            this.modeListBike = new ArrayList<>();
            for (LightControlBikeSetup src : (List<LightControlBikeSetup>)modeList) {
                LightControlBikeSetup dst = new LightControlBikeSetup();
                src.copy(dst);
                this.modeListBike.add(dst);
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View modeCfg;

        switch (viewType) {
            case TYPE_GROUP:
                modeCfg = layoutInflater.inflate(R.layout.device_config_group_item, parent, false);
                return new GroupViewHolder(modeCfg);
            case TYPE_PREFERRED_MODE:
            case TYPE_TEMPORARY_MODE:
                modeCfg = layoutInflater.inflate(R.layout.device_modelist_item, parent, false);
                return new ModeListViewHolder(modeCfg);
            case TYPE_MODE:
                modeCfg = layoutInflater.inflate(R.layout.device_modesetup_item, parent, false);
                return new ModeSetupViewHolder(modeCfg);
        }
        return null;
    }

    private int getMipmapResIdForGroups(String type, boolean state) {
        String pkgName = mContext.getPackageName();
        String name = type + (state ? "_selected" : "_unselected");
        return mContext.getResources().getIdentifier(name, "mipmap", pkgName);
    }

    private int getMipmapResIdForSetup(String type, boolean state) {
        String pkgName = mContext.getPackageName();
        String name = type + (state ? "_active" : "_inactive");
        return mContext.getResources().getIdentifier(name, "mipmap", pkgName);
    }

    private void setGroups(int newGroups) {
        groups = newGroups;
        this.notifyItemChanged(0);
    }

    private void changePrefMode(int modeClicked) {
        modeClicked += 1;
        if (modeClicked == preferredMode) {
            preferredMode = 0;
        } else {
            preferredMode = modeClicked;
        }

        int position = 0;
        if (features.configurationFeatures.modeGroupingSupported)
            position++;
        this.notifyItemChanged(position);
    }

    private void changeTempMode(int modeClicked) {
        modeClicked += 1;
        if (modeClicked == temporaryMode) {
            temporaryMode = 0;
        } else {
            temporaryMode = modeClicked;
        }

        int position = 0;
        if (features.configurationFeatures.modeGroupingSupported)
            position++;
        if (features.configurationFeatures.preferredModeSupported)
            position++;
        this.notifyItemChanged(position);
    }

    private void toggleFlood(int modeNo) {
        if (modeListHelmet == null) {
            return;
        }
        LightControlHelmetSetup setup = modeListHelmet.get(modeNo);
        if (setup.spotActive && setup.cloned) {
            return; // not allowed
        }
        setup.floodActive = !setup.floodActive;
        if (!setup.floodActive && !setup.spotActive) {
            setup.cloned = false;
            setup.pitchComensation = false;
            setup.intensity = 0;
        }
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void toggleSpot(int modeNo) {
        if (modeListHelmet == null) {
            return;
        }
        LightControlHelmetSetup setup = modeListHelmet.get(modeNo);
        if (setup.floodActive && setup.cloned) {
            return; // not allowed
        }
        setup.spotActive = !setup.spotActive;
        if (!setup.floodActive && !setup.spotActive) {
            setup.cloned = false;
            setup.pitchComensation = false;
            setup.intensity = 0;
        }
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void togglePitch(int modeNo) {
        if (modeListHelmet == null) {
            return;
        }
        LightControlHelmetSetup setup = modeListHelmet.get(modeNo);
        if (!setup.floodActive && !setup.spotActive) {
            return; // not allowed
        }
        setup.pitchComensation = !setup.pitchComensation;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void toggleClone(int modeNo) {
        if (modeListHelmet == null) {
            return;
        }
        LightControlHelmetSetup setup = modeListHelmet.get(modeNo);
        if (setup.floodActive == setup.spotActive) {
            return; // not allowed
        }
        setup.cloned = !setup.cloned;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void toggleMainBeam(int modeNo) {
        if (modeListBike == null) {
            return;
        }
        LightControlBikeSetup setup = modeListBike.get(modeNo);
        setup.mainBeamActive = !setup.mainBeamActive;
        if (!setup.mainBeamActive && !setup.extendedMainBeamActive)
            setup.mainBeamIntensity = 0;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void toggleExtendedMainBeam(int modeNo) {
        if (modeListBike == null) {
            return;
        }
        LightControlBikeSetup setup = modeListBike.get(modeNo);
        setup.extendedMainBeamActive = !setup.extendedMainBeamActive;
        if (!setup.mainBeamActive && setup.extendedMainBeamActive)
            setup.mainBeamIntensity = 0;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void toggleHighBeam(int modeNo) {
        if (modeListBike == null) {
            return;
        }
        LightControlBikeSetup setup = modeListBike.get(modeNo);
        setup.highBeamActive = !setup.highBeamActive;
        if (!setup.highBeamActive)
            setup.highBeamIntensity = 0;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void toggleDaylight(int modeNo) {
        if (modeListBike == null) {
            return;
        }
        LightControlBikeSetup setup = modeListBike.get(modeNo);
        setup.daylightActive = !setup.daylightActive;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void toggleTail(int modeNo) {
        if (modeListHelmet != null) {
            LightControlHelmetSetup setup = modeListHelmet.get(modeNo);
            setup.taillight = !setup.taillight;
        } else if (modeListBike != null) {
            LightControlBikeSetup setup = modeListBike.get(modeNo);
            setup.taillight = !setup.taillight;
        }
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void toggleBrake(int modeNo) {
        if (modeListHelmet != null) {
            LightControlHelmetSetup setup = modeListHelmet.get(modeNo);
            setup.brakeLight = !setup.brakeLight;
        } else if (modeListBike != null) {
            LightControlBikeSetup setup = modeListBike.get(modeNo);
            setup.brakeLight = !setup.brakeLight;
        }
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void changeIntensity(int newIntensity, int modeNo) {
        LightControlHelmetSetup setup = modeListHelmet.get(modeNo);
        if (setup == null || (!setup.floodActive && !setup.spotActive)) {
            return;
        }
        setup.intensity = newIntensity;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void changeMainBeamIntensity(int newIntensity, int modeNo) {
        LightControlBikeSetup setup = modeListBike.get(modeNo);
        if (setup == null || (!setup.mainBeamActive && !setup.extendedMainBeamActive)) {
            return;
        }
        setup.mainBeamIntensity = newIntensity;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private void changeHighBeamIntensity(int newIntensity, int modeNo) {
        LightControlBikeSetup setup = modeListBike.get(modeNo);
        if (setup == null || !setup.highBeamActive) {
            return;
        }
        setup.highBeamIntensity = newIntensity;
        this.notifyItemChanged(getPosFromModeNo(modeNo));
    }

    private int getPosFromModeNo(int modeNo) {
        if (features.configurationFeatures.modeGroupingSupported)
            modeNo++;
        if (features.configurationFeatures.preferredModeSupported)
            modeNo++;
        if (features.configurationFeatures.temporaryModeSupported)
            modeNo++;
        return modeNo;
    }

    private int getModeNoFromPos(int position) {
        if (features.configurationFeatures.modeGroupingSupported)
            position--;
        if (features.configurationFeatures.preferredModeSupported)
            position--;
        if (features.configurationFeatures.temporaryModeSupported)
            position--;
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_GROUP:
                GroupViewHolder groupHolder = (GroupViewHolder)holder;
                groupHolder.one.setOnClickListener(v -> setGroups(1));
                groupHolder.one.setImageResource(getMipmapResIdForGroups("one", groups == 1));
                groupHolder.two.setOnClickListener(v -> setGroups(2));
                groupHolder.two.setImageResource(getMipmapResIdForGroups("two", groups == 2));
                groupHolder.four.setOnClickListener(v -> setGroups(4));
                groupHolder.four.setImageResource(getMipmapResIdForGroups("four", groups == 4));
                break;
            case TYPE_PREFERRED_MODE:
            case TYPE_TEMPORARY_MODE:
                ModeListViewHolder modeListHolder = (ModeListViewHolder)holder;
                if (holder.getItemViewType() == TYPE_PREFERRED_MODE)
                    modeListHolder.heading.setText(mContext.getResources().getString(R.string.dev_config_pref));
                else // if (holder.getItemViewType() == TYPE_TEMPORARY_MODE)
                    modeListHolder.heading.setText(mContext.getResources().getString(R.string.dev_config_temp));

                for (int i = 0; i < modeListHolder.modes.size(); i++) {
                    int color, modeCnt = 0;
                    final int modeClicked = i;
                    Button button = modeListHolder.modes.get(i);

                    if (features.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT)
                        modeCnt = modeListHelmet.size();
                    else if (features.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT)
                        modeCnt = modeListBike.size();
                    if (i >= modeCnt)
                        button.setVisibility(View.GONE);
                    else
                        button.setVisibility(View.VISIBLE);

                    if ((holder.getItemViewType() == TYPE_PREFERRED_MODE && preferredMode != 0 && preferredMode - 1 == i) ||
                        (holder.getItemViewType() == TYPE_TEMPORARY_MODE && temporaryMode != 0 && temporaryMode - 1 == i)) {
                        color = mContext.getResources().getColor(R.color.colorSelected);
                    } else {
                        color = mContext.getResources().getColor(R.color.colorUnselected);
                    }
                    button.setTextColor(color);
                    if (holder.getItemViewType() == TYPE_PREFERRED_MODE)
                        button.setOnClickListener(v -> changePrefMode(modeClicked));
                    else if (holder.getItemViewType() == TYPE_TEMPORARY_MODE)
                        button.setOnClickListener(v -> changeTempMode(modeClicked));
                }
                break;
            case TYPE_MODE:
                int pos = getModeNoFromPos(position);
                ModeSetupViewHolder modeSetupHolder = (ModeSetupViewHolder)holder;
                if (features.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT) {
                    LightControlHelmetSetup setup = modeListHelmet.get(pos);
                    if (setup == null) {
                        break;
                    }

                    String modeNo = mContext.getResources().getString(R.string.dev_config_mode) + " " + (pos + 1);
                    modeSetupHolder.modeNo.setText(modeNo);

                    ImageView img = modeSetupHolder.modeSetups.get(0);
                    if (features.helmetLightFeatures.floodSupported) {
                        img.setOnClickListener(v -> toggleFlood(pos));
                        img.setImageResource(getMipmapResIdForSetup("flood", setup.floodActive));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(1);
                    if (features.helmetLightFeatures.spotSupported) {
                        img.setOnClickListener(v -> toggleSpot(pos));
                        img.setImageResource(getMipmapResIdForSetup("spot", setup.spotActive));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(2);
                    if (features.helmetLightFeatures.pitchCompensationSupported) {
                        img.setOnClickListener(v -> togglePitch(pos));
                        img.setImageResource(getMipmapResIdForSetup("pitch", setup.pitchComensation));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(3);
                    if (features.helmetLightFeatures.driverCloningSupported) {
                        img.setOnClickListener(v -> toggleClone(pos));
                        img.setImageResource(getMipmapResIdForSetup("clone", setup.cloned));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(4);
                    if (features.helmetLightFeatures.externalTaillightSupported) {
                        img.setOnClickListener(v -> toggleTail(pos));
                        img.setImageResource(getMipmapResIdForSetup("tail", setup.taillight));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(5);
                    if (features.helmetLightFeatures.externalBrakelightSupported) {
                        img.setOnClickListener(v -> toggleBrake(pos));
                        img.setImageResource(getMipmapResIdForSetup("brake", setup.brakeLight));
                    } else
                        img.setVisibility(View.GONE);

                    SeekBar intensity = modeSetupHolder.intensityBar1;
                    intensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser) {
                                changeIntensity(progress * 5, pos);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                    if (!setup.floodActive && !setup.spotActive) {
                        intensity.setProgress(0);
                        intensity.setEnabled(false);
                        modeSetupHolder.intensityValue.setText(mContext.getResources().getString(R.string.measurement_default));
                        modeSetupHolder.unit.setText("");
                    } else {
                        intensity.setEnabled(true);
                        intensity.setProgress((int)((setup.intensity + 2.5) / 5));
                        modeSetupHolder.intensityValue.setText(String.valueOf(setup.intensity));
                        modeSetupHolder.unit.setText(setup.pitchComensation ?
                                mContext.getResources().getString(R.string.dev_unit_lux) :
                                mContext.getResources().getString(R.string.dev_unit_percent));
                    }

                    intensity = modeSetupHolder.intensityBar2;
                    intensity.setVisibility(View.GONE);
                }
                else if (features.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT) {
                    LightControlBikeSetup setup = modeListBike.get(pos);
                    if (setup == null) {
                        break;
                    }

                    String modeNo = mContext.getResources().getString(R.string.dev_config_mode) + " " + (pos + 1);
                    modeSetupHolder.modeNo.setText(modeNo);

                    ImageView img = modeSetupHolder.modeSetups.get(0);
                    if (features.bikeLightFeatures.mainBeamSupported) {
                        img.setOnClickListener(v -> toggleMainBeam(pos));
                        img.setImageResource(getMipmapResIdForSetup("mb", setup.mainBeamActive));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(1);
                    if (features.bikeLightFeatures.extendedMainBeamSupported) {
                        img.setOnClickListener(v -> toggleExtendedMainBeam(pos));
                        img.setImageResource(getMipmapResIdForSetup("flood", setup.extendedMainBeamActive));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(2);
                    if (features.bikeLightFeatures.highBeamCompensationSupported) {
                        img.setOnClickListener(v -> toggleHighBeam(pos));
                        img.setImageResource(getMipmapResIdForSetup("spot", setup.highBeamActive));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(3);
                    if (features.bikeLightFeatures.daylightSupported) {
                        img.setOnClickListener(v -> toggleDaylight(pos));
                        img.setImageResource(getMipmapResIdForSetup("dl", setup.daylightActive));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(4);
                    if (features.bikeLightFeatures.externalTaillightSupported) {
                        img.setOnClickListener(v -> toggleTail(pos));
                        img.setImageResource(getMipmapResIdForSetup("tail", setup.taillight));
                    } else
                        img.setVisibility(View.GONE);

                    img = modeSetupHolder.modeSetups.get(5);
                    if (features.bikeLightFeatures.externalBrakelightSupported) {
                        img.setOnClickListener(v -> toggleBrake(pos));
                        img.setImageResource(getMipmapResIdForSetup("brake", setup.brakeLight));
                    } else
                        img.setVisibility(View.GONE);

                    SeekBar intensity = modeSetupHolder.intensityBar1;
                    intensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser) {
                                changeMainBeamIntensity(progress * 5, pos);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                    if (!setup.mainBeamActive && !setup.extendedMainBeamActive) {
                        intensity.setProgress(0);
                        intensity.setEnabled(false);
                    } else {
                        intensity.setEnabled(true);
                        intensity.setProgress((int)((setup.mainBeamIntensity + 2.5) / 5));
                    }

                    intensity = modeSetupHolder.intensityBar2;
                    intensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser) {
                                changeHighBeamIntensity(progress * 5, pos);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                    if (!setup.highBeamActive) {
                        intensity.setProgress(0);
                        intensity.setEnabled(false);
                    } else {
                        intensity.setEnabled(true);
                        intensity.setProgress((int)((setup.highBeamIntensity + 2.5) / 5));
                    }

                    String intensityValue;
                    if (setup.mainBeamActive || setup.extendedMainBeamActive)
                        intensityValue = String.valueOf(setup.mainBeamIntensity);
                    else
                        intensityValue = mContext.getResources().getString(R.string.measurement_default);
                    intensityValue = intensityValue.concat("/");
                    if (setup.highBeamActive)
                        intensityValue = intensityValue.concat(String.valueOf(setup.highBeamIntensity));
                    else
                        intensityValue = intensityValue.concat(mContext.getResources().getString(R.string.measurement_default));
                    modeSetupHolder.intensityValue.setText(intensityValue);

                    if (!setup.mainBeamActive && !setup.extendedMainBeamActive && !setup.highBeamActive)
                        modeSetupHolder.unit.setText("");
                    else
                        modeSetupHolder.unit.setText(mContext.getResources().getString(R.string.dev_unit_percent));
                }
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (!features.configurationFeatures.modeGroupingSupported)
            position += 1;
        else if (position == TYPE_GROUP)
            return TYPE_GROUP;

        if (!features.configurationFeatures.preferredModeSupported)
            position += 1;
        else if (position == TYPE_PREFERRED_MODE)
            return TYPE_PREFERRED_MODE;

        if (!features.configurationFeatures.temporaryModeSupported)
            position += 1;
        else if (position == TYPE_TEMPORARY_MODE)
            return TYPE_TEMPORARY_MODE;

        if (features.configurationFeatures.modeConfigurationSupported)
            return TYPE_MODE;
         else
             return TYPE_INVALID;
    }

    @Override
    public int getItemCount() {
        int len = 0;
        if (features.configurationFeatures.modeGroupingSupported)
            len += 1;
        if (features.configurationFeatures.preferredModeSupported)
            len += 1;
        if (features.configurationFeatures.temporaryModeSupported)
            len += 1;
        if (features.configurationFeatures.modeConfigurationSupported) {
            if (features.lightType == LightControlFeatureCallback.LC_LF_LT_HELMET_LIGHT)
                len += modeListHelmet.size();
            else if (features.lightType == LightControlFeatureCallback.LC_LF_LT_BIKE_LIGHT)
                len += modeListBike.size();
        }
        return len;
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        ImageView one, two, four;

        GroupViewHolder(View itemView) {
            super(itemView);
            this.one = itemView.findViewById(R.id.oneGroup);
            this.two = itemView.findViewById(R.id.twoGroups);
            this.four = itemView.findViewById(R.id.fourGroups);
        }
    }

    public static class ModeSetupViewHolder extends RecyclerView.ViewHolder {
        public TextView modeNo, intensityValue, unit;
        public List<ImageView> modeSetups = new ArrayList<>();
        public SeekBar intensityBar1, intensityBar2;

        public ModeSetupViewHolder(View itemView) {
            super(itemView);
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
        }
    }

    public static class ModeListViewHolder extends  RecyclerView.ViewHolder {
        public TextView heading;
        public List<Button> modes = new ArrayList<>();
        public ModeListViewHolder(View itemView) {
            super(itemView);
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

    public int getGroups() {
        return groups;
    }

    public List<LightControlHelmetSetup> getModeListHelmet() {
        return modeListHelmet;
    }

    public List<LightControlBikeSetup> getModeListBike() {
        return modeListBike;
    }

    public int getPreferredMode() {
        return preferredMode;
    }

    public int getTemporaryMode() {
        return temporaryMode;
    }
}

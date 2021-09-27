package com.reisi.lightcontrol.profile;

import android.os.Parcel;
import android.os.Parcelable;

public class LightControlBikeSetup implements Parcelable {
    public boolean mainBeamActive;
    public boolean extendedMainBeamActive;
    public boolean highBeamActive;
    public boolean daylightActive;
    public boolean taillight;
    public boolean brakeLight;
    public int mainBeamIntensity;
    public int highBeamIntensity;

    public LightControlBikeSetup() {

    }

    public void copy(LightControlBikeSetup dst) {
        dst.mainBeamActive = this.mainBeamActive;
        dst.extendedMainBeamActive = this.extendedMainBeamActive;
        dst.highBeamActive = this.highBeamActive;
        dst.daylightActive = this.daylightActive;
        dst.taillight = this.taillight;
        dst.brakeLight = this.brakeLight;
        dst.mainBeamIntensity = this.mainBeamIntensity;
        dst.highBeamIntensity = this.highBeamIntensity;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mainBeamActive ? 1 : 0);
        dest.writeInt(extendedMainBeamActive ? 1 : 0);
        dest.writeInt(highBeamActive ? 1 : 0);
        dest.writeInt(daylightActive ? 1 : 0);
        dest.writeInt(taillight ? 1 : 0);
        dest.writeInt(brakeLight ? 1 : 0);
        dest.writeInt(mainBeamIntensity);
        dest.writeInt(highBeamIntensity);
    }

    public static final Creator<LightControlBikeSetup> CREATOR = new Creator<LightControlBikeSetup>() {
        public LightControlBikeSetup createFromParcel(Parcel pc) {
            return new LightControlBikeSetup(pc);
        }
        public LightControlBikeSetup[] newArray(int size) {
            return new LightControlBikeSetup[size];
        }
    };

    public LightControlBikeSetup(Parcel in) {
        mainBeamActive = in.readInt() != 0;
        extendedMainBeamActive = in.readInt() != 0;
        highBeamActive = in.readInt() != 0;
        daylightActive = in.readInt() != 0;
        taillight = in.readInt() != 0;
        brakeLight = in.readInt() != 0;
        mainBeamIntensity = in.readInt();
        highBeamIntensity = in.readInt();
    }
}

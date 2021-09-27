package com.reisi.lightcontrol.profile;

import android.os.Parcel;
import android.os.Parcelable;

public class LightControlHelmetSetup implements Parcelable {
    public boolean floodActive;
    public boolean spotActive;
    public boolean pitchComensation;
    public boolean cloned;
    public boolean taillight;
    public boolean brakeLight;
    public int intensity;

    public LightControlHelmetSetup() {

    }

    public void copy(LightControlHelmetSetup dst) {
        dst.floodActive = this.floodActive;
        dst.spotActive = this.spotActive;
        dst.pitchComensation = this.pitchComensation;
        dst.cloned = this.cloned;
        dst.taillight = this.taillight;
        dst.brakeLight = this.brakeLight;
        dst.intensity = this.intensity;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(floodActive ? 1 : 0);
        dest.writeInt(spotActive ? 1 : 0);
        dest.writeInt(pitchComensation ? 1 : 0);
        dest.writeInt(cloned ? 1 : 0);
        dest.writeInt(taillight ? 1 : 0);
        dest.writeInt(brakeLight ? 1 : 0);
        dest.writeInt(intensity);
    }

    public static final Parcelable.Creator<LightControlHelmetSetup> CREATOR = new Parcelable.Creator<LightControlHelmetSetup>() {
        public LightControlHelmetSetup createFromParcel(Parcel pc) {
            return new LightControlHelmetSetup(pc);
        }
        public LightControlHelmetSetup[] newArray(int size) {
            return new LightControlHelmetSetup[size];
        }
    };

    public LightControlHelmetSetup(Parcel in) {
        floodActive = in.readInt() != 0;
        spotActive = in.readInt() != 0;
        pitchComensation = in.readInt() != 0;
        cloned = in.readInt() != 0;
        taillight = in.readInt() != 0;
        brakeLight = in.readInt() != 0;
        intensity = in.readInt();
    }
}

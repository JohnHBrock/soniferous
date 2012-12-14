package brock.soniferous.device;

import java.util.Random;

import android.os.Parcel;
import android.os.Parcelable;
import brock.soniferous.R;
import brock.soniferous.Sound;

public class GpsSatellite extends DeviceBase {
	private float mAzimuth;
	private float mElevation;
	private float mSignalToNoiseRatio;
	private static final int[] mSoundResourceIDs = new int[] {
																															R.raw.amsel,
																															R.raw.amsel2,
																															R.raw.blackter,
																															R.raw.bsp3,
																															R.raw.chaffinc,
																															R.raw.corncrak2,
																															R.raw.fit,
																															R.raw.gambelim,
																															R.raw.gartenbl,
																															R.raw.grs,
																															R.raw.heckenb2,
																															R.raw.heckenbr,
																															R.raw.hnf,
																															R.raw.ksp4,
																															R.raw.nightin2,
																															R.raw.parus2,
																															R.raw.waldlaub3,
																															R.raw.wechselk,
																															R.raw.wendehal,
																															R.raw.zaunkoen,
																															R.raw.zkn2,
																															R.raw.zwergta
																														};
	private static Random random = new Random();
	
	public GpsSatellite(String pseudoRandomNumberString, float azimuth, float elevation, float signalToNoiseRatio) {
		super(pseudoRandomNumberString, "", 0, 0);
		
		int soundIndex = random.nextInt(mSoundResourceIDs.length);
		int repetitionNoise = random.nextInt(20000); // make the sound repeat every 10 seconds plus up to 20 additional seconds.
		mSound = new Sound(.75f, 0, 1, 10000 + repetitionNoise, mSoundResourceIDs[soundIndex]);
	}
	
	private GpsSatellite(Parcel p) {
		this(p.readString(), p.readFloat(), p.readFloat(), p.readFloat());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// The order of these lines matters! The values get read out in the same order they're written. Lame.
		dest.writeString(mUniqueID);
		dest.writeString(mDeviceName);
		dest.writeFloat(mAzimuth);
		dest.writeFloat(mElevation);
    dest.writeFloat(mSignalToNoiseRatio);
	}
	
	public static final Parcelable.Creator<GpsSatellite> CREATOR =
	new Parcelable.Creator<GpsSatellite>() {
		public GpsSatellite createFromParcel(Parcel p) {
			return new GpsSatellite(p);  
		}
	
		public GpsSatellite[] newArray(int size) {
			return new GpsSatellite[size];
		}
	};
	
	public float getAzmiuth() {
		return mAzimuth;
	}
	public void setAzimuth(float azimuth) {
		mAzimuth = azimuth;
	}
	
	public float getElevation() {
		return mElevation;
	}
	public void setElevation(float elevation) {
		mElevation = elevation;
	}
	
	public float getSignalToNoiseRatio() {
		return mSignalToNoiseRatio;
	}
	public void setSignalToNoiseRatio(float signalToNoiseRatio) {
		mSignalToNoiseRatio = signalToNoiseRatio;
	}
	
	@Override
	public String getPrettyPrintedInfo1() {
		return "";
	}

	@Override
	public String getPrettyPrintedInfo2() {
		return "SNR: " + mSignalToNoiseRatio;
	}
}

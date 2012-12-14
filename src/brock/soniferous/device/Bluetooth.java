package brock.soniferous.device;

import java.util.Random;

import android.os.Parcel;
import android.os.Parcelable;
import brock.soniferous.R;
import brock.soniferous.Sound;

public class Bluetooth extends DeviceBase {
	private final float MIN_AMPLITUDE_DBM = -100;
	private final float MAX_AMPLITUDE_DBM = -40;
	private static Random random = new Random();
	
	public Bluetooth(String deviceName, String deviceAddress, int signalStrengthDecibels) {
		super(deviceAddress, deviceName, 0, signalStrengthDecibels);
		int repetitionNoise = random.nextInt(5000); // make the sound repeat every 7 seconds plus up to 5 additional seconds.
		mSound = new Sound(mSignalStrengthDecibels, MIN_AMPLITUDE_DBM, MAX_AMPLITUDE_DBM, 7000 + repetitionNoise, R.raw.bee2);
	}
	
	private Bluetooth(Parcel p) {
		this(p.readString(), p.readString(), p.readInt());
	}

	@Override
	public String getPrettyPrintedInfo1() {
		return "";
	}

	@Override
	public String getPrettyPrintedInfo2() {
		return mSignalStrengthDecibels + " dBm";
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// The order of these lines matters! The values get read out in the same order they're written. Lame.
		dest.writeString(mDeviceName);
		dest.writeString(mUniqueID);
		dest.writeInt(mSignalStrengthDecibels);
	}
	
	public static final Parcelable.Creator<Bluetooth> CREATOR =
	new Parcelable.Creator<Bluetooth>() {
		public Bluetooth createFromParcel(Parcel p) {
			return new Bluetooth(p);  
		}
	
		public Bluetooth[] newArray(int size) {
			return new Bluetooth[size];
		}
	};
}

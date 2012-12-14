package brock.soniferous.device;

import android.os.Parcel;
import android.os.Parcelable;
import brock.soniferous.R;
import brock.soniferous.Sound;

public class WifiDevice extends DeviceBase {
	private final float MIN_FREQUENCY_MHZ = 2401; // See http://en.wikipedia.org/wiki/IEEE_802.11g-2003 for wifi frequency range.
	private final float MAX_FREQUENCY_MHZ = 2463;
	private final float MIN_AMPLITUDE_DBM = -100;
	private final float MAX_AMPLITUDE_DBM = -40;
	
	public WifiDevice(String SSID, String BSSID, int frequencyMHz, int signalStrengthDecibels) {
		super(BSSID, SSID, frequencyMHz, signalStrengthDecibels);
		
		mSound = new Sound(mFrequencyMhz, MIN_FREQUENCY_MHZ, MAX_FREQUENCY_MHZ, mSignalStrengthDecibels, MIN_AMPLITUDE_DBM, MAX_AMPLITUDE_DBM, 0, R.raw.acheta);
	}
	
	private WifiDevice(Parcel p) {
		this(p.readString(), p.readString(), p.readInt(), p.readInt());
		mSound = p.readParcelable(Sound.class.getClassLoader());
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// The order of these lines matters! The values get read out in the same order they're written. Lame.
		dest.writeString(mUniqueID);
		dest.writeString(mDeviceName);
		dest.writeInt(mFrequencyMhz);
    dest.writeInt(mSignalStrengthDecibels);
    dest.writeParcelable(mSound, 0);
	}
	
	public static final Parcelable.Creator<WifiDevice> CREATOR =
	new Parcelable.Creator<WifiDevice>() {
		public WifiDevice createFromParcel(Parcel p) {
			return new WifiDevice(p);  
		}
	
		public WifiDevice[] newArray(int size) {
			return new WifiDevice[size];
		}
	};

	@Override
	public String getPrettyPrintedInfo1() {
		return mFrequencyMhz + "MHz";
	}

	@Override
	public String getPrettyPrintedInfo2() {
		return mSignalStrengthDecibels + " dBm";
	}
}

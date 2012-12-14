package brock.soniferous.device;
import brock.soniferous.Sound;
import android.os.Parcel;
import android.os.Parcelable;

public abstract class DeviceBase implements Parcelable {
	protected String mDeviceName;
	protected String mUniqueID;
	protected int mFrequencyMhz;
	protected int mSignalStrengthDecibels;
	protected int mStaleCount = 0; // represents how many times in a row this device was not detected
	protected Sound mSound;
			
	protected DeviceBase(String theUniqueID, String theDeviceName, int theFrequencyMhz, int theSignalStrengthDecibels) {
		mUniqueID = theUniqueID;
		mDeviceName = theDeviceName;
		mFrequencyMhz = theFrequencyMhz;
		mSignalStrengthDecibels = theSignalStrengthDecibels;
	}
	
	public Sound getSound() {
		return mSound;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof DeviceBase) {
			return this.mUniqueID == ((DeviceBase)o).mUniqueID;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return mUniqueID.hashCode();
	}
	
	public String getUniqueID() {
		return mUniqueID;
	}
	
	public String getDeviceName() {
		return mDeviceName;
	}
	public void setDeviceName(String deviceName) {
		mDeviceName = deviceName;
	}
	
	public int getFrequencyMhz() {
		return mFrequencyMhz;
	}
	public void setFrequencyMhz(int frequencyMhz) {
		mFrequencyMhz = frequencyMhz;
		mSound.adjustPlaybackRateFromDeviceFrequency(frequencyMhz);
	}
	
	public int getSignalStrengthDecibels() {
		return mSignalStrengthDecibels;
	}
	public void setSignalStrengthDecibels(int signalStrengthDecibels) {
		mSignalStrengthDecibels = signalStrengthDecibels;
		mSound.adjustVolumeFromDeviceAmplitude(signalStrengthDecibels);
	}
	
	public int getStaleCount() {
		return mStaleCount;
	}
	public void incrementStaleCount() {
		++mStaleCount;
	}
	public void resetStaleCount() {
		mStaleCount = 0;
	}
	
	public abstract String getPrettyPrintedInfo1();
	public abstract String getPrettyPrintedInfo2();
	
	public int describeContents() {
		return this.hashCode();
	}
	
	public abstract void writeToParcel(Parcel dest, int flags);
}

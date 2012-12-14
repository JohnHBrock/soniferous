package brock.soniferous.device;

import android.os.Parcel;
import android.os.Parcelable;
import brock.soniferous.Sound;

public class CellSite extends DeviceBase {

	public CellSite(String theDeviceName) {
		super(theDeviceName, theDeviceName, 0, 0);
	}
	
	private CellSite(Parcel p) {
		this(p.readString());
	}
	
	@Override
	public String getPrettyPrintedInfo1() {
		return "";
	}

	@Override
	public String getPrettyPrintedInfo2() {
		return "";
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// The order of these lines matters! The values get read out in the same order they're written. Lame.
		dest.writeString(mDeviceName);
	}
	
	public static final Parcelable.Creator<CellSite> CREATOR =
	new Parcelable.Creator<CellSite>() {
		public CellSite createFromParcel(Parcel p) {
			return new CellSite(p);  
		}
	
		public CellSite[] newArray(int size) {
			return new CellSite[size];
		}
	};
}

package brock.soniferous.device;

import java.util.ArrayList;
import java.util.Comparator;

public class DeviceComparator implements Comparator<DeviceBase> {
	public enum ComparisonType {
		SIGNAL_STRENGTH,
		FREQUENCY,
		DEVICE_TYPE,
		DEVICE_NAME,
		DEVICE_ID
	}
	
	private ArrayList<Comparator<DeviceBase>> mComparatorList = new ArrayList<Comparator<DeviceBase>>();
	private int mIndexOfNextComparatorToRun = 0;
	
	// This could use refactoring, but the basic idea is that the order of the comparison (i.e., how to deal with ties) goes like this:
	// order by signal strength, then frequency, then device type, then device name, then device id.
	// However, the value of the ComparisonType enum changes this. Whatever is passed as the ComparisonType is what happens first. So, if DEVICE_NAME is passed,
	// the ordering will be: by device name, then signal strength, then frequency, then device type, then device ID.
	// Basically, the enum value moves the desired comparator to the front of the list so that it gets executed first.
	public DeviceComparator(ComparisonType comparisonType) {
		Comparator<DeviceBase> cSignalStrength = new Comparator<DeviceBase>() { public int compare(DeviceBase d1, DeviceBase d2) { return compareSignalStrength(d1, d2); } };
		Comparator<DeviceBase> cFrequency = new Comparator<DeviceBase>() { public int compare(DeviceBase d1, DeviceBase d2) { return compareFrequency(d1, d2); } };
		Comparator<DeviceBase> cDeviceType = new Comparator<DeviceBase>() { public int compare(DeviceBase d1, DeviceBase d2) { return compareDeviceType(d1, d2); } };
		Comparator<DeviceBase> cDeviceName = new Comparator<DeviceBase>() { public int compare(DeviceBase d1, DeviceBase d2) { return compareDeviceName(d1, d2); } };
		Comparator<DeviceBase> cDeviceID = new Comparator<DeviceBase>() { public int compare(DeviceBase d1, DeviceBase d2) { return compareDeviceID(d1, d2); } };
		
		mComparatorList.add(cSignalStrength);
		mComparatorList.add(cFrequency);
		mComparatorList.add(cDeviceType);
		mComparatorList.add(cDeviceName);
		mComparatorList.add(cDeviceID);
		
		// make the desired comparison type first in the list
		switch(comparisonType) {
			case SIGNAL_STRENGTH:
				mComparatorList.remove(cSignalStrength);
				mComparatorList.add(0, cSignalStrength);
				break;
			case FREQUENCY:
				mComparatorList.remove(cFrequency);
				mComparatorList.add(0, cFrequency);
				break;
			case DEVICE_TYPE:
				mComparatorList.remove(cDeviceType);
				mComparatorList.add(0, cDeviceType);
				break;
			case DEVICE_NAME:
				mComparatorList.remove(cDeviceName);
				mComparatorList.add(0, cDeviceName);
				break;
			case DEVICE_ID:
				mComparatorList.remove(cDeviceID);
				mComparatorList.add(0, cDeviceID);
				break;
			default:
				// TODO: log error, we shouldn't reach the default case.
		}
	}
	
	public int compare(DeviceBase d1, DeviceBase d2) {
		mIndexOfNextComparatorToRun = 0;
		return mComparatorList.get(mIndexOfNextComparatorToRun).compare(d1, d2);
	}

	private int compareSignalStrength(DeviceBase d1, DeviceBase d2) {
		++mIndexOfNextComparatorToRun;
		int d1SignalStrength = d1.getSignalStrengthDecibels();
		int d2SignalStrength = d2.getSignalStrengthDecibels();
		if (d1SignalStrength > d2SignalStrength) {
			return -1;
		}
		else if (d1SignalStrength < d2SignalStrength) {
			return 1;
		}
		else if (mIndexOfNextComparatorToRun < mComparatorList.size()) {
			return mComparatorList.get(mIndexOfNextComparatorToRun).compare(d1, d2);
		}
		return 0;
	}
	
	private int compareFrequency(DeviceBase d1, DeviceBase d2) {
		++mIndexOfNextComparatorToRun;
		int d1Frequency = d1.getFrequencyMhz();
		int d2Frequency = d2.getFrequencyMhz();
		if (d1Frequency > d2Frequency) {
			return -1;
		}
		else if (d1Frequency < d2Frequency) {
			return 1;
		}
		else if (mIndexOfNextComparatorToRun < mComparatorList.size()) {
			return mComparatorList.get(mIndexOfNextComparatorToRun).compare(d1, d2);
		}
		return 0;
	}
	
	private int compareDeviceType(DeviceBase d1, DeviceBase d2) {
		++mIndexOfNextComparatorToRun;
		String c1ClassName = d1.getClass().getName();
		String c2ClassName = d2.getClass().getName();
		int comparisonResult = c1ClassName.compareTo(c2ClassName);
		if (comparisonResult == 0 && mIndexOfNextComparatorToRun < mComparatorList.size()) {
			return mComparatorList.get(mIndexOfNextComparatorToRun).compare(d1, d2);
		}
		return comparisonResult;
	}
	
	private int compareDeviceName(DeviceBase d1, DeviceBase d2) {
		++mIndexOfNextComparatorToRun;
		String d1DeviceName = d1.getDeviceName();
		String d2DeviceName = d2.getDeviceName();
		int comparisonResult = d1DeviceName.compareTo(d2DeviceName); 
		if (comparisonResult == 0 && mIndexOfNextComparatorToRun < mComparatorList.size()) {
			return mComparatorList.get(mIndexOfNextComparatorToRun).compare(d1, d2);
		}
		return comparisonResult;
	}
	
	private int compareDeviceID(DeviceBase d1, DeviceBase d2) {
		++mIndexOfNextComparatorToRun;
		String d1DeviceID = d1.getUniqueID();
		String d2DeviceID = d2.getUniqueID();
		int comparisonResult = d1DeviceID.compareTo(d2DeviceID); 
		if (comparisonResult == 0 && mIndexOfNextComparatorToRun < mComparatorList.size()) {
			return mComparatorList.get(mIndexOfNextComparatorToRun).compare(d1, d2);
		}
		return comparisonResult;	
	}
}

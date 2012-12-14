package brock.soniferous.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import brock.soniferous.R;
import brock.soniferous.SoniferousContextCollectionService;
import brock.soniferous.Sound;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class DeviceSoundAdapter extends ArrayAdapter<DeviceBase> {
	private Activity mActivity;
	private LayoutInflater mInflater;
	private final SoniferousContextCollectionService mService;
	private DeviceComparator.ComparisonType mComparisonType = DeviceComparator.ComparisonType.SIGNAL_STRENGTH;
	
	private class ViewHolder {
		ImageButton ibToggleSound;
		ImageView ivDeviceType;
		TextView tvDeviceID;
		TextView tvDeviceName;
		TextView tvInfoTopRight;
		TextView tvInfoBottomRight;
		int position;
	}
	
	private ArrayList<DeviceBase> mDevices = new ArrayList<DeviceBase>();
	
	public DeviceSoundAdapter(Activity activity, ArrayList<DeviceBase> devices, SoniferousContextCollectionService service) {
		super(activity, devices.size(), devices);
		mActivity = activity;
		mInflater = LayoutInflater.from(mActivity);
		mDevices = devices;
		mService = service;
	}
	
	public int getCount() {
		return mDevices.size();
	}

	public DeviceBase getItem(int position) {
		return mDevices.get(position);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.device_sound_item, null);
			holder = new ViewHolder();
			holder.ibToggleSound = (ImageButton)convertView.findViewById(R.id.ibSoundToggle);
			holder.ivDeviceType = (ImageView)convertView.findViewById(R.id.ivDeviceType);
			holder.tvDeviceID = (TextView)convertView.findViewById(R.id.tvDeviceID);
			holder.tvDeviceName = (TextView)convertView.findViewById(R.id.tvDeviceName);
			holder.tvInfoTopRight = (TextView)convertView.findViewById(R.id.tvInfoTopRight);
			holder.tvInfoBottomRight = (TextView)convertView.findViewById(R.id.tvInfoBottomRight);
			holder.position = position;
			
			OnClickListener listener = new OnClickListener() {
				public void onClick(View view) {
					if (mService == null) {
						return;
					}
					
					ViewHolder vh = (ViewHolder)view.getTag();
					DeviceBase device = mDevices.get(vh.position);
					boolean isCurrentlyMuted = device.getSound().getMuted();
					device.getSound().setMuted(!isCurrentlyMuted);
					mService.muteSound(device.getUniqueID(), !isCurrentlyMuted);
					vh.ibToggleSound.setImageResource(getMuteImageResourceID(!isCurrentlyMuted));
				}
			};
      holder.ibToggleSound.setOnClickListener(listener);
			
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder)convertView.getTag();
		}
		
		DeviceBase device = mDevices.get(position);
		holder.tvDeviceID.setText(device.getUniqueID());
		holder.tvDeviceName.setText(device.getDeviceName());
		holder.tvInfoTopRight.setText(device.getPrettyPrintedInfo1());
		holder.tvInfoBottomRight.setText(device.getPrettyPrintedInfo2());
		
		int deviceTypeImageID = -1;
		if (device instanceof WifiDevice) {
			deviceTypeImageID = R.drawable.ic_soniferous_wifi;
		}
		else if (device instanceof GpsSatellite) {
			deviceTypeImageID = R.drawable.stat_sys_gps_on;
		}
		else if (device instanceof Bluetooth) {
			deviceTypeImageID = R.drawable.stat_sys_tether_bluetooth;
		}
		
		if (deviceTypeImageID != -1)
		{
			holder.ivDeviceType.setImageResource(deviceTypeImageID);
		}
		
		Sound sound = mDevices.get(position).getSound();
		if (sound != null) {
			holder.ibToggleSound.setImageResource(getMuteImageResourceID(sound.getMuted()));
		}
		holder.ibToggleSound.setTag(holder);
		
		return convertView;
	}
	
	private int getMuteImageResourceID(boolean isMuted) {
		return (isMuted ? android.R.drawable.ic_lock_silent_mode : android.R.drawable.ic_lock_silent_mode_off);
	}
	
	public void updateDeviceList(ArrayList<DeviceBase> devices) {
		Collections.sort(devices, new DeviceComparator(mComparisonType));
		mDevices.clear();
		mDevices.addAll(devices);
		notifyDataSetChanged();
	}

	public long getItemId(int position) {
		return 0;
	}
	
	public void sortBy(DeviceComparator.ComparisonType comparisonType) {
		mComparisonType = comparisonType;
		Collections.sort(mDevices, new DeviceComparator(comparisonType));
		notifyDataSetChanged();
	}
}

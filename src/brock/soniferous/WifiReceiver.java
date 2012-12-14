package brock.soniferous;

import java.util.List;

import brock.soniferous.device.WifiDevice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class WifiReceiver extends BroadcastReceiver {
	private WifiManager mWifi;
	private WifiDevice[] mWifiDevices = new WifiDevice[0];
	
	public WifiReceiver(WifiManager theWifi) {
		mWifi = theWifi;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		List<ScanResult> wifiResults = mWifi.getScanResults();
		mWifiDevices = new WifiDevice[wifiResults.size()];
		for (int i = 0; i < wifiResults.size(); ++i) {
			ScanResult result = wifiResults.get(i);
			mWifiDevices[i] = new WifiDevice(result.SSID, result.BSSID, result.frequency, result.level);
		}
	}
	
	public WifiDevice[] getDetectedWifiDevices() {
		return mWifiDevices;
	}
}

package brock.soniferous;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import brock.soniferous.device.DeviceBase;
import brock.soniferous.device.WifiDevice;
import brock.soniferous.device.Bluetooth;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class SoniferousContextCollectionService extends Service {
	public static final String DEVICE_LIST_UPDATE_EVENT_NAME = "device list update event";
	public static final String KEY_DEVICE_LIST_UPDATE_INFO = "dwevice list update info";
	private static final int MAX_STALE_COUNT_WIFI = 10; // A wifi device can be undetected this may times in a row. More than this, and it's removed.
	private static final int MAX_STALE_COUNT_GPS = 5; // A gps device can be undetected this may times in a row. More than this, and it's removed.
	private static final int MAX_STALE_COUNT_BLUETOOTH = 2; // A bluetooth device can be undetected this may times in a row. More than this, and it's removed.
	private static final int BROADCAST_DEVICE_UPDATE_INTERVAL_SECONDS = 5; // how often to update the UI
	private static final int SCAN_WIFI_INTERVAL_SECONDS = 2; // The number of seconds to wait in between wifi scans
	private static final int GPS_UPDATE_TIME_MILLISECONDS = 2000;
	private static final int GPS_UPDATE_DISTANCE_METERS = 10;
	private final IBinder binder = new SoniferousContextCollectionBinder();
	private WifiManager mWifiManager;
	private BroadcastReceiver mWifiResultsReceiver;
	private NotificationCompat.Builder mNotificatonBuilder;
	private LocationManager mLocationManager;
	private GpsStatus.Listener mGpsStatusListener;
	private BluetoothAdapter mBluetoothAdapter;
	private BroadcastReceiver mBluetoothDeviceFoundReceiver;
	private BroadcastReceiver mBluetoothScanFinishedReceiver;
	private SoniferousLocationListener mLocationListener;
	private ScheduledExecutorService mScheduleTaskExecutor = Executors.newScheduledThreadPool(2);

	private final ConcurrentHashMap<String, DeviceBase> mDeviceIdToDeviceMap = new ConcurrentHashMap<String, DeviceBase>();
	private final int NOTIFICATION_ID_SERVICE_IS_RUNNING = 99;

	/**
	 * Class used for the client Binder. Because we know this service always runs
	 * in the same process as its clients, we don't need to deal with IPC.
	 */
	public class SoniferousContextCollectionBinder extends Binder {
		SoniferousContextCollectionService getService() {
			// Return this instance of SoniferousContextCollectionService so clients
			// can call public methods
			return SoniferousContextCollectionService.this;
		}
	}

	@Override
	public void onCreate() {
		mLocationListener = new SoniferousLocationListener();
		Sound.initialize();
		
		mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		mWifiResultsReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent) 
			{
				incrementStaleCounts(WifiDevice.class);
				List<ScanResult> wifiResults = mWifiManager.getScanResults();
				for (int i = 0; i < wifiResults.size(); ++i) {
					ScanResult result = wifiResults.get(i);
					if (mDeviceIdToDeviceMap.containsKey(result.BSSID)) {
						if (mDeviceIdToDeviceMap.get(result.BSSID) instanceof WifiDevice) {
							WifiDevice wifiDevice = (WifiDevice)mDeviceIdToDeviceMap.get(result.BSSID);
							wifiDevice.setDeviceName(result.SSID);
							wifiDevice.setFrequencyMhz(result.frequency);
							wifiDevice.setSignalStrengthDecibels(result.level);
							wifiDevice.resetStaleCount();
						}
						else {
							// TODO: log warning, two different kinds of devices have the same unique ID
						}
					}
					else {
						WifiDevice wifiDevice = new WifiDevice(result.SSID, result.BSSID, result.frequency, result.level);
						wifiDevice.getSound().startPlaying();
						synchronized(mDeviceIdToDeviceMap) {
							mDeviceIdToDeviceMap.put(result.BSSID, wifiDevice);
						}
					}
				}
				removeStaleDevices(WifiDevice.class, MAX_STALE_COUNT_WIFI);
			}
		};
		registerReceiver(mWifiResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mGpsStatusListener = new GpsStatus.Listener() {
			public void onGpsStatusChanged(int event) {
				if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
					incrementStaleCounts(brock.soniferous.device.GpsSatellite.class);
					Iterable<GpsSatellite> satellites = mLocationManager.getGpsStatus(null).getSatellites();
					// convert to a collection of soniferous.device.GpsSatellite objects
					for (GpsSatellite detectedSat : satellites) {						
						String satID = String.valueOf(detectedSat.getPrn());
						if (mDeviceIdToDeviceMap.containsKey(satID)) {
							if (mDeviceIdToDeviceMap.get(detectedSat.getPrn()) instanceof brock.soniferous.device.GpsSatellite) {
								brock.soniferous.device.GpsSatellite sat = (brock.soniferous.device.GpsSatellite)mDeviceIdToDeviceMap.get(satID);
								sat.setAzimuth(detectedSat.getAzimuth());
								sat.setElevation(detectedSat.getElevation());
								sat.setSignalToNoiseRatio(detectedSat.getSnr());
								sat.resetStaleCount();
							}
							else {
								// TODO: log warning, two different kinds of devices have the same unique ID
							}
						}
						else {
							brock.soniferous.device.GpsSatellite gpsDevice = new brock.soniferous.device.GpsSatellite(satID, detectedSat.getAzimuth(), detectedSat.getElevation(), detectedSat.getSnr());
							gpsDevice.getSound().startPlaying();
							synchronized(mDeviceIdToDeviceMap) {
								mDeviceIdToDeviceMap.put(satID, gpsDevice);
							}
						}
					}
					removeStaleDevices(brock.soniferous.device.GpsSatellite.class, MAX_STALE_COUNT_GPS);
				}
			}
		};
		mLocationManager.addGpsStatusListener(mGpsStatusListener);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBluetoothDeviceFoundReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (mDeviceIdToDeviceMap.containsKey(device.getAddress())) {
					if (mDeviceIdToDeviceMap.get(device.getAddress()) instanceof Bluetooth) {
						Bluetooth bluetoothDevice = (Bluetooth)mDeviceIdToDeviceMap.get(device.getAddress());
						bluetoothDevice.setDeviceName(device.getName());
						bluetoothDevice.setSignalStrengthDecibels(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)0));
						bluetoothDevice.resetStaleCount();
					}
					else {
						// TODO: log warning, two different kinds of devices have the same unique ID
					}
				}
				else {
					int signalStrength = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)0);
					Bluetooth bluetoothDevice = new Bluetooth(device.getName(), device.getAddress(), signalStrength);
					bluetoothDevice.getSound().startPlaying();
					synchronized(mDeviceIdToDeviceMap) {
						mDeviceIdToDeviceMap.put(device.getAddress(), bluetoothDevice);
					}
				}
			}
		};
		registerReceiver(mBluetoothDeviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		
		mBluetoothScanFinishedReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent) {
				removeStaleDevices(Bluetooth.class, MAX_STALE_COUNT_BLUETOOTH);
				incrementStaleCounts(Bluetooth.class);
				scanForBluetoothDevices();
			}
		};
		registerReceiver(mBluetoothScanFinishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

		Intent intent = new Intent(this, SoniferousActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		mNotificatonBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_soniferous)
				.setContentTitle("Soniferous")
				.setContentIntent(pendingIntent);
	}
	
	private void incrementStaleCounts(Class typeOfDevice) {
		Iterator<String> iterator = mDeviceIdToDeviceMap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			DeviceBase possiblyStaleDevice = mDeviceIdToDeviceMap.get(key);
			if (possiblyStaleDevice.getClass() == typeOfDevice) {
				possiblyStaleDevice.incrementStaleCount();
			}
		}
	}
	
	private void removeStaleDevices(Class typeOfDevice, int maxStaleCount) {
		Iterator<String> iterator = mDeviceIdToDeviceMap.keySet().iterator(); 
		while (iterator.hasNext()) {
			String key = iterator.next();
			DeviceBase possiblyStaleDevice = mDeviceIdToDeviceMap.get(key);
			if (possiblyStaleDevice.getClass() == typeOfDevice && possiblyStaleDevice.getStaleCount() > maxStaleCount) {
				Sound sound = possiblyStaleDevice.getSound();
				if (sound != null) {
					sound.stopPlaying();
				}
				
				synchronized(mDeviceIdToDeviceMap) {
					mDeviceIdToDeviceMap.remove(key);
				}
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startID) {
		Notification notification = mNotificatonBuilder.build();
		startForeground(NOTIFICATION_ID_SERVICE_IS_RUNNING, notification);

		scanForGPS();
		scanForBluetoothDevices();
		
		mScheduleTaskExecutor.scheduleWithFixedDelay(new Runnable() {
		  public void run() {
		    scanForWifiDevices();
		  }
		}, 0, SCAN_WIFI_INTERVAL_SECONDS, TimeUnit.SECONDS);		
		
		mScheduleTaskExecutor.scheduleWithFixedDelay(new Runnable() {
		  public void run() {
		    broadcastUpdate();
		  }
		}, 0, BROADCAST_DEVICE_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);

		return START_STICKY;
	}
		
	private void broadcastUpdate() {
		Intent updateDevicesIntent = new Intent(DEVICE_LIST_UPDATE_EVENT_NAME);
		synchronized(mDeviceIdToDeviceMap) {
			ArrayList<DeviceBase> devices = new ArrayList<DeviceBase>(mDeviceIdToDeviceMap.values());
			updateDevicesIntent.putParcelableArrayListExtra(KEY_DEVICE_LIST_UPDATE_INFO, devices);
			LocalBroadcastManager.getInstance(SoniferousContextCollectionService.this).sendBroadcast(updateDevicesIntent);
		}
	}
	
	public void muteSound(String deviceID, boolean mute) {
		DeviceBase device = mDeviceIdToDeviceMap.get(deviceID);
		if (device != null) {
			Sound sound = device.getSound();
			if (sound != null) {
				sound.setMuted(mute);
			}
		}
	}
	
	@Override
	public void onDestroy() {
		mScheduleTaskExecutor.shutdownNow();
		mBluetoothAdapter.cancelDiscovery();
		
		if (mWifiResultsReceiver != null) {
			unregisterReceiver(mWifiResultsReceiver);
		}
		
		if (mBluetoothDeviceFoundReceiver != null) {
			unregisterReceiver(mBluetoothDeviceFoundReceiver);
		}
	
		if (mBluetoothScanFinishedReceiver != null) {
			unregisterReceiver(mBluetoothScanFinishedReceiver);
		}
		
		if (mLocationManager != null && mGpsStatusListener != null) {
			mLocationManager.removeGpsStatusListener(mGpsStatusListener);
		}
		
		if (mLocationManager != null && mLocationListener != null) {
			mLocationManager.removeUpdates(mLocationListener);
		}
		
		Sound.close();

		super.onDestroy();
	}

	private void scanForWifiDevices() {
		if (!mWifiManager.isWifiEnabled()) {
			Toast.makeText(getApplicationContext(), "Wifi needs to be enabled.", Toast.LENGTH_LONG).show();
			mWifiManager.setWifiEnabled(true); // TODO: shouldn't turn on wifi directly, should first get user permission.
		}
		mWifiManager.startScan();
	}
	
	private void scanForBluetoothDevices() {
		if (mBluetoothAdapter.isEnabled()){
			if (!mBluetoothAdapter.isDiscovering()) {
				incrementStaleCounts(Bluetooth.class);
				mBluetoothAdapter.startDiscovery();
			}
		}
		else {
			Toast.makeText(getApplicationContext(), "Bluetooth needs to be enabled.", Toast.LENGTH_LONG).show();
			mBluetoothAdapter.enable(); // TODO: shouldn't turn on Bluetooth directly, should first get user permission.
		}
	}
	
	private void scanForGPS() {
    Criteria criteria = new Criteria();
    criteria.setAccuracy(Criteria.ACCURACY_FINE);
    final String provider = mLocationManager.getBestProvider(criteria, true);
    mLocationManager.requestLocationUpdates(provider, GPS_UPDATE_TIME_MILLISECONDS, GPS_UPDATE_DISTANCE_METERS, mLocationListener);
	}
}

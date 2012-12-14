package brock.soniferous;

import java.util.ArrayList;

import brock.soniferous.SoniferousContextCollectionService.SoniferousContextCollectionBinder;
import brock.soniferous.device.DeviceBase;
import brock.soniferous.device.DeviceComparator;
import brock.soniferous.device.DeviceSoundAdapter;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ListView;

public class SoniferousActivity extends Activity implements OnCreateContextMenuListener {
		SoniferousContextCollectionService mService;
		DeviceSoundAdapter mDeviceSoundAdapter;
		ListView lv;

		/** Called when the activity is first created. */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);

			Intent intent = new Intent(this, SoniferousContextCollectionService.class);
			startService(intent);
		}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.soniferous_menu, menu);
      return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.pause_or_play:
    		if (item.getTitle().equals("Pause")) {
          unbindService(mConnection);
          mService = null;
          Intent intent = new Intent(this, SoniferousContextCollectionService.class);
          stopService(intent);
      		item.setTitle("Play");
      		item.setIcon(android.R.drawable.ic_media_play);
    		}
    		else {
      		Intent intent = new Intent(this, SoniferousContextCollectionService.class);
          startService(intent);
      		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
          item.setTitle("Pause");
      		item.setIcon(android.R.drawable.ic_media_pause);
    		}
    		return true;
    	case R.id.sort_by:
    		registerForContextMenu(lv);
    		openContextMenu(lv);
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
  	@Override
    public void onCreateContextMenu(ContextMenu menu, View target, ContextMenuInfo menuInfo) {  
  		menu.setHeaderTitle("Sort by...");
  		menu.add(0, DeviceComparator.ComparisonType.SIGNAL_STRENGTH.ordinal(), 0, "Signal Strength");
  		menu.add(0, DeviceComparator.ComparisonType.FREQUENCY.ordinal(), 0, "Frequency");
  		menu.add(0, DeviceComparator.ComparisonType.DEVICE_TYPE.ordinal(), 0, "Device Type");
  		menu.add(0, DeviceComparator.ComparisonType.DEVICE_NAME.ordinal(), 0, "Device Name");
  		menu.add(0, DeviceComparator.ComparisonType.DEVICE_ID.ordinal(), 0, "Device ID");
  	}
  	
  	@Override
    public boolean onContextItemSelected(MenuItem item) {  
  		DeviceComparator.ComparisonType sortBy = DeviceComparator.ComparisonType.values()[item.getItemId()];
  		mDeviceSoundAdapter.sortBy(sortBy);
  		return true;  
  	}
    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				ArrayList<DeviceBase> devices = intent.getParcelableArrayListExtra(SoniferousContextCollectionService.KEY_DEVICE_LIST_UPDATE_INFO);
				
				if (mDeviceSoundAdapter == null) {
					mDeviceSoundAdapter = new DeviceSoundAdapter(SoniferousActivity.this, devices, mService);
					lv = (ListView)findViewById(R.id.lvDevices);
					lv.setAdapter(mDeviceSoundAdapter);
				}
				else {
					mDeviceSoundAdapter.updateDeviceList(devices);
				}
			}
    };
    
    @Override
    protected void onStart() {
    	super.onStart();
    	if (mService == null) {
    		Intent intent = new Intent(this, SoniferousContextCollectionService.class);
    		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    	}
    	
    	LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(SoniferousContextCollectionService.DEVICE_LIST_UPDATE_EVENT_NAME));
    }
    
    
    
    @Override
    protected void onStop() {
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    	
      if (mService != null) {
        unbindService(mConnection);
        mService = null;
      }
    	
    	super.onStop();
    }
    
    @Override
    protected void onDestroy() {
      Intent intent = new Intent(this, SoniferousContextCollectionService.class);
      stopService(intent);
    	
    	super.onDestroy();
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	mService = ((SoniferousContextCollectionBinder)service).getService();
        }

        // apparently onServiceDisconnected only gets called in rare circumstances, not when unbinding.
        // See: http://stackoverflow.com/questions/5292954/android-unbind-service-and-onservicedisconnected-problem
        public void onServiceDisconnected(ComponentName componentName) {
        	mService = null;
        }
    };
}